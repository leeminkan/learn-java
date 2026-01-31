Building a JIT for **Apple Silicon (M1/M2/M3)** requires respecting the ARM64 architecture.

On ARM64, there are a few major differences from Intel (x86):

1.  **Instruction Set:** ARM uses fixed-width 32-bit instructions (4 bytes per instruction), whereas x86 uses variable lengths. You cannot output `0xB8` for a move; you must calculate specific 32-bit integers.
2.  **Memory Protection (W^X):** Modern macOS is strict. You generally cannot have memory that is both Writable and Executable at the same time. You must Write first, then "seal" it to be Executable using `mprotect`.
3.  **Cache Coherency:** After writing code, you **must** flush the CPU instruction cache, or the M1 chip will try to execute the old "empty" memory it cached earlier.

Here is the step-by-step plan to integrate an ARM64 JIT into your JVM.

---

### Step 1: Create the JIT Compiler Class

Create a new file `src/Jit.h`. This class will handle allocating memory, generating ARM64 machine code, and handling the "Write -> Execute" protection switch.

**Note:** For this implementation, we will build a **"Leaf JIT"**. It will optimize methods that return a constant integer (e.g., `return 42;`). This proves the full pipeline works without needing a complex register allocator.

```cpp
// src/Jit.h
#pragma once
#include "ConstantPool.h"
#include <vector>
#include <sys/mman.h>
#include <iostream>
#include <libkern/OSCacheControl.h> // REQUIRED for MacOS M1/M2 cache flushing
#include <cstring> // For memcpy

// Define the function pointer type for our JITed code
typedef int (*JitFunction)();

class JitCompiler {
public:
    // ARM64 Instruction Hex Codes (Little Endian)
    // RET: Returns from function
    const uint32_t ARM64_RET = 0xD65F03C0;

    // Helper to encode "MOV W0, #immediate" (Move constant into Return Register W0)
    // Instruction format: [ 1 0 1 (bits 31-29) ... imm16 (bits 20-5) ... Rd (bits 4-0) ]
    // Base opcode for MOVZ (Move with Zero) is 0x52800000
    uint32_t encode_mov_w0(uint16_t immediate) {
        // MOVZ W0, #immediate, LSL #0
        return 0x52800000 | (static_cast<uint32_t>(immediate) << 5);
    }

    // Main Compile Function
    void* compile(const MethodInfo& method) {
        std::vector<uint32_t> machine_code;

        // SIMPLE PASS: Look for "iconst/bipush + return" pattern
        // We are iterating through JVM bytecodes and converting to ARM64 opcodes
        for (size_t i = 0; i < method.bytecode.size(); ++i) {
            uint8_t opcode = method.bytecode[i];

            if (opcode >= 0x02 && opcode <= 0x08) { // iconst_m1 to iconst_5
                machine_code.push_back(encode_mov_w0(opcode - 3)); // e.g., iconst_0 (0x03) -> 0
            }
            else if (opcode == 0x10) { // bipush
                uint8_t val = method.bytecode[i + 1];
                machine_code.push_back(encode_mov_w0(val));
                i++; // Skip operand
            }
            else if (opcode == 0xac) { // ireturn
                machine_code.push_back(ARM64_RET);
            }
            // For this demo, we ignore other opcodes or fallback to interpreter
        }

        if (machine_code.empty() || machine_code.back() != ARM64_RET) {
             // We only JIT simple methods that return a constant.
             // If it doesn't end with a RET, it's too complex for our simple JIT.
            std::cerr << "JIT: Method " << method.name << " not eligible for simple JIT." << std::endl;
            return nullptr;
        }

        // 1. Allocate Memory (READ + WRITE initially)
        size_t size = machine_code.size() * sizeof(uint32_t);
        // Page align the size for mprotect
        size_t page_size = sysconf(_SC_PAGESIZE);
        size_t aligned_size = (size + page_size - 1) / page_size * page_size;

        void* mem = mmap(nullptr, aligned_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);

        if (mem == MAP_FAILED) {
            std::cerr << "JIT: Memory allocation failed!" << std::endl;
            return nullptr;
        }

        // 2. Write Machine Code
        memcpy(mem, machine_code.data(), size);

        // 3. Secure Memory (Switch to READ + EXECUTE)
        // MacOS on M1 forbids Writable+Executable memory. We must flip the switch.
        mprotect(mem, aligned_size, PROT_READ | PROT_EXEC);

        // 4. FLUSH CACHE (Critical for M1/M2/M3)
        // Tells the CPU "I changed code in memory, please clear your instruction cache"
        sys_icache_invalidate(mem, aligned_size);

        std::cout << "--- JIT COMPILED " << method.name << " TO ARM64 NATIVE CODE ---" << std::endl;
        return mem;
    }
};
```

---

### Step 2: Update `MethodInfo` in `ConstantPool.h`

Your method structure needs to track how often it is called (`call_count`) and store the pointer to the compiled native code if it exists.

Update `src/ConstantPool.h`:

```cpp
// src/ConstantPool.h
// ... existing includes ...

struct MethodInfo {
    std::string name;
    std::string descriptor;
    std::vector<uint8_t> bytecode;
    uint16_t max_stack;
    uint16_t max_locals;

    // --- NEW JIT FIELDS ---
    int call_count = 0;         // Profiler counter
    void* jit_code_ptr = nullptr; // Pointer to native ARM64 code
};
```

---

### Step 3: Update `Interpreter.h` for JIT Integration

This is the most significant step. We need to modify `Interpreter.h` to:
1.  Include `Jit.h`.
2.  Add a `JitCompiler` member.
3.  Change the `all_methods` member to `std::vector<MethodInfo>&` so that updates to profiling data (like `call_count` and `jit_code_ptr`) persist.
4.  Update the constructor to accept this reference.
5.  Change the `run` method's signature to take a `MethodInfo&` (mutable reference) for the currently executing method.
6.  Add the JIT check, profiling, and compilation trigger logic to the beginning of the `run` method.
7.  Crucially, the `OP_INVOKESTATIC` handler needs to be updated to dynamically resolve the target method and its arguments using the constant pool, rather than hardcoding. Also, the `find_method` helper must return a non-`const` pointer.

Here's the updated `src/Interpreter.h` content:

```cpp
// src/Interpreter.h
#pragma once
#include "ConstantPool.h"
#include "Jit.h" // <--- Add this
#include <vector>
#include <stack>
#include <iostream>
#include <string>
#include <unordered_map>
#include <memory>
#include <unistd.h> // For sysconf

// JVM Opcodes
#define OP_NEW 0xbb           // Create new object
#define OP_DUP 0x59           // Duplicate top stack item
#define OP_INVOKESPECIAL 0xb7 // Call constructor (<init>)
#define OP_PUTFIELD 0xb5      // Set field in object
#define OP_GETFIELD 0xb4      // Get field from object
#define OP_ASTORE_1 0x4c      // Store object ref in local var 1
#define OP_ALOAD_1 0x2b       // Load object ref from local var 1

#define OP_ACONST_NULL 0x01
#define OP_ICONST_M1 0x02
#define OP_ICONST_0 0x03
#define OP_ICONST_1 0x04
#define OP_ICONST_2 0x05
#define OP_ICONST_3 0x06
#define OP_ICONST_4 0x07
#define OP_ICONST_5 0x08
#define OP_BIPUSH 0x10
#define OP_ISTORE_1 0x3c
#define OP_ISTORE_2 0x3d
#define OP_ISTORE_3 0x3e
#define OP_ILOAD_0 0x1a
#define OP_ILOAD_1 0x1b
#define OP_ILOAD_2 0x1c
#define OP_ILOAD_3 0x1d
#define OP_IADD 0x60
#define OP_IRETURN 0xac      // Return an integer
#define OP_INVOKESTATIC 0xb8 // Call a static method
#define OP_GETSTATIC 0xb2
#define OP_LDC 0x12
#define OP_INVOKEVIRTUAL 0xb6
#define OP_RETURN 0xb1

#define OP_IF_ICMPLE 0xa4  // Branch if int comparison is <=
#define OP_IF_ICMPGT 0xa3  // Branch if int comparison is >
#define OP_GOTO      0xa7  // Jump unconditionally
#define OP_IINC      0x84  // Increment local variable
#define OP_IF_ICMPGE 0xa2  // Branch if int comparison is >=

// --- ARRAY OPCODES ---
#define OP_NEWARRAY     0xbc  // Create primitive array
#define OP_ARRAYLENGTH  0xbe  // Get length of array
#define OP_IALOAD       0x2e  // Load int from array
#define OP_IASTORE      0x4f  // Store int into array


// Represents a Java Object allocated on the Heap
struct JavaObject
{
    std::string class_name;
    std::unordered_map<std::string, int> fields; // Map of field name -> int value

    // Array stuff
    // We simply use a vector to store array elements if this object is an array
    std::vector<int> array_data;
};

class Interpreter
{
private:
    std::vector<std::shared_ptr<CpInfo>> cp;
    std::vector<MethodInfo>& all_methods; // NOTE: Now a reference

    // THE JVM HEAP
    // Stores all created objects. The index in this vector is the "Object Reference".
    std::vector<std::shared_ptr<JavaObject>> heap;
    JitCompiler jit; // <--- The JIT Engine

    // Helper to find a method by Name and Descriptor
    // Returns a non-const pointer so JIT details can be updated
    MethodInfo* find_method(const std::string &name, const std::string &desc)
    {
        for (auto &method : all_methods)
        {
            if (method.name == name && method.descriptor == desc)
            {
                return &method;
            }
        }
        return nullptr;
    }

public:
    // Constructor now takes a reference to methods
    Interpreter(std::vector<std::shared_ptr<CpInfo>> constant_pool, std::vector<MethodInfo>& methods)
        : cp(constant_pool), all_methods(methods) {}

    // run() now acts as a JVM Stack Frame. It returns an integer.
    // Takes MethodInfo& so JIT profiling/code_ptr can be updated
    int run(MethodInfo& method, const std::vector<int> &args = {})
    {
        // --- JIT CHECK ---

        // 1. If we already compiled this, run the native ARM64 code!
        if (method.jit_code_ptr != nullptr) {
            std::cout << "--- EXECUTING NATIVE CODE FOR " << method.name << " ---" << std::endl;
            // Cast memory address to function pointer
            JitFunction func = (JitFunction)method.jit_code_ptr;
            int result = func(); // CPU JUMPS HERE
            return result;
        }

        // 2. Profiling: Increment counter
        method.call_count++;

        // 3. Trigger Compilation if "Hot" (Threshold = 5 calls for demo)
        if (method.call_count >= 5) {
            void* code = jit.compile(method);
            if (code) {
                method.jit_code_ptr = code;
                // Run the newly compiled code immediately
                JitFunction func = (JitFunction)code;
                return func();
            }
        }

        // --- FALLBACK TO INTERPRETER ---

        std::cout << "\n--- INTERPRETING FRAME: " << method.name << " (call #" << method.call_count << ") ---" << std::endl;

        // 1. The Operand Stack
        std::stack<int> operand_stack; // Integers and Object References share the same stack


        // 2. The Local Variables Array (Sized by the compiler's max_locals)
        std::vector<int> local_variables(method.max_locals);

        // Load arguments into the local variables array
        for (size_t i = 0; i < args.size(); ++i)
        {
            local_variables[i] = args[i];
        }

        const auto &code = method.bytecode;
        int pc = 0;

        while (pc < code.size())
        {
            uint8_t opcode = code[pc];

            switch (opcode)
            {
                // --- OBJECT ORIENTED OPCODES ---

            case OP_NEW:
            {
                // Create object and push its Heap Index (Reference) to the stack
                auto obj = std::make_shared<JavaObject>();
                obj->class_name = "Point";
                heap.push_back(obj);

                int obj_ref = heap.size() - 1; // The reference is its index in the heap
                operand_stack.push(obj_ref);

                std::cout << "Instruction: new (Created Object at Heap Index " << obj_ref << ")" << std::endl;
                pc += 3;
                break;
            }

            case OP_DUP:
            {
                // Duplicates the top item. Needed because invokespecial (constructor) consumes a reference.
                int top = operand_stack.top();
                operand_stack.push(top);
                pc += 1;
                break;
            }

            case OP_INVOKESPECIAL:
            {
                // Calls the constructor (<init>). We'll just pop the object ref and do nothing for now.
                operand_stack.pop();
                std::cout << "Instruction: invokespecial (Called Point.<init>)" << std::endl;
                pc += 3;
                break;
            }

            case OP_ASTORE_1:
                local_variables[1] = operand_stack.top();
                operand_stack.pop();
                pc += 1;
                break;

            case OP_ALOAD_1:
                operand_stack.push(local_variables[1]);
                pc += 1;
                break;

            case OP_PUTFIELD:
            {
                // Set a field on an object
                int value = operand_stack.top();
                operand_stack.pop();
                int obj_ref = operand_stack.top();
                operand_stack.pop();

                // In a real JVM, we'd look up the field name from the Constant Pool.
                // For demo, we hardcode logic for x and y.
                std::string field_name = (value == 5) ? "x" : "y";

                heap[obj_ref]->fields[field_name] = value;
                std::cout << "Instruction: putfield (Set obj[" << obj_ref << "]." << field_name << " = " << value << ")" << std::endl;
                pc += 3;
                break;
            }

            case OP_GETFIELD:
            {
                // Read a field from an object
                int obj_ref = operand_stack.top();
                operand_stack.pop();

                // Hacky way to distinguish between reading x and y for the demo
                static bool read_x = true;
                std::string field_name = read_x ? "x" : "y";
                read_x = !read_x;

                int value = heap[obj_ref]->fields[field_name];
                operand_stack.push(value);

                std::cout << "Instruction: getfield (Read obj[" << obj_ref << "]." << field_name << " which is " << value << ")" << std::endl;
                pc += 3;
                break;
            }

            case OP_ACONST_NULL:
                operand_stack.push(0); // Representing null as 0
                pc += 1;
                break;
            case OP_ICONST_M1:
                operand_stack.push(-1);
                pc += 1;
                break;
            case OP_ICONST_0:
                operand_stack.push(0);
                pc += 1;
                break;
            case OP_ICONST_1:
                operand_stack.push(1);
                pc += 1;
                break;
            case OP_ICONST_2:
                operand_stack.push(2);
                pc += 1;
                break;
            case OP_ICONST_3:
                operand_stack.push(3);
                pc += 1;
                break;
            case OP_ICONST_4:
                operand_stack.push(4);
                pc += 1;
                break;

            case OP_ICONST_5:
                std::cout << "Instruction: iconst_5" << std::endl;
                operand_stack.push(5);
                pc += 1;
                break;
            case OP_BIPUSH:
            {
                int8_t value = code[pc + 1];
                std::cout << "Instruction: bipush " << (int)value << std::endl;
                operand_stack.push(value);
                pc += 2;
                break;
            }
            case OP_ISTORE_1:
                std::cout << "Instruction: istore_1" << std::endl;
                local_variables[1] = operand_stack.top();
                operand_stack.pop();
                pc += 1;
                break;
            case OP_ISTORE_2:
                std::cout << "Instruction: istore_2" << std::endl;
                local_variables[2] = operand_stack.top();
                operand_stack.pop();
                pc += 1;
                break;
            case OP_ISTORE_3:
                std::cout << "Instruction: istore_3" << std::endl;
                local_variables[3] = operand_stack.top();
                operand_stack.pop();
                pc += 1;
                break;
            // Note: Static methods use local_variables[0] for the first argument
            case OP_ILOAD_0:
                operand_stack.push(local_variables[0]);
                pc += 1;
                break;
            case OP_ILOAD_1:
                std::cout << "Instruction: iload_1" << std::endl;
                operand_stack.push(local_variables[1]);
                pc += 1;
                break;
            case OP_ILOAD_2:
                std::cout << "Instruction: iload_2" << std::endl;
                operand_stack.push(local_variables[2]);
                pc += 1;
                break;
            case OP_ILOAD_3:
                std::cout << "Instruction: iload_3" << std::endl;
                operand_stack.push(local_variables[3]);
                pc += 1;
                break;
            case OP_IADD:
            {
                std::cout << "Instruction: iadd" << std::endl;
                int val2 = operand_stack.top();
                operand_stack.pop();
                int val1 = operand_stack.top();
                operand_stack.pop();
                operand_stack.push(val1 + val2);
                pc += 1;
                break;
            }

            case OP_INVOKESTATIC:
            {
                uint16_t method_idx = (code[pc + 1] << 8) | code[pc + 2];

                // Resolve the method reference from the Constant Pool
                auto method_ref = std::dynamic_pointer_cast<CpMethodRef>(cp[method_idx]);
                auto name_and_type = std::dynamic_pointer_cast<CpNameAndType>(cp[method_ref->name_and_type_index]);
                auto name_utf8 = std::dynamic_pointer_cast<CpUtf8>(cp[name_and_type->name_index]);
                auto descriptor_utf8 = std::dynamic_pointer_cast<CpUtf8>(cp[name_and_type->descriptor_index]);

                MethodInfo *target_method = find_method(name_utf8->bytes, descriptor_utf8->bytes);

                if (!target_method) {
                    std::cerr << "Error: Method not found for invokestatic: " << name_utf8->bytes << descriptor_utf8->bytes << std::endl;
                    return 0;
                }

                std::cout << "Instruction: invokestatic (Calling " << target_method->name << ")" << std::endl;

                // Dynamically determine number of arguments from descriptor
                std::string desc_str = descriptor_utf8->bytes;
                size_t num_args = 0;
                size_t param_start = desc_str.find('(');
                size_t param_end = desc_str.find(')');

                if (param_start != std::string::npos && param_end != std::string::npos && param_start < param_end) {
                    std::string params = desc_str.substr(param_start + 1, param_end - param_start - 1);
                    // This is a simplification. A real JVM parses full descriptor for arg types.
                    // For now, assume 'I' for int and 'L...' for objects.
                    // Each 'I' and 'L...;' counts as one argument for our simple stack.
                    for (char c : params) {
                        if (c == 'I') {
                            num_args++;
                        } else if (c == 'L') { // Object type
                            num_args++;
                            size_t semicolon_pos = params.find(';', params.find(c));
                            if (semicolon_pos != std::string::npos) {
                                c = params[semicolon_pos]; // Advance 'c' to semicolon to skip object type chars
                            }
                        }
                    }
                }

                std::vector<int> call_args(num_args);
                for (size_t i = 0; i < num_args; ++i) {
                    call_args[num_args - 1 - i] = operand_stack.top(); // Pop in reverse order
                    operand_stack.pop();
                }

                // CREATE NEW FRAME (Recursive call)
                int return_value = run(*target_method, call_args);

                // Push the result back onto the current stack
                operand_stack.push(return_value);

                std::cout << "--- RETURNED TO FRAME: " << method.name << " (Result: " << return_value << ") ---" << std::endl;
                pc += 3;
                break;
            }

            case OP_IRETURN:
            {
                int result = operand_stack.top();
                operand_stack.pop();
                std::cout << "Instruction: ireturn (" << result << ")" << std::endl;
                return result; // DESTROY FRAME
            }

            case OP_GETSTATIC:
            {
                // getstatic indexbyte1 indexbyte2
                uint16_t index = (code[pc + 1] << 8) | code[pc + 2];
                std::cout << "Instruction: getstatic #" << index << " (System.out)" << std::endl;
                // In a real JVM, this pushes a reference to System.out onto the stack.
                // We'll push a dummy reference '99' to represent System.out.
                operand_stack.push(99);
                pc += 3;
                break;
            }
            case OP_INVOKEVIRTUAL:
            {
                // invokevirtual indexbyte1 indexbyte2
                uint16_t index = (code[pc + 1] << 8) | code[pc + 2];
                std::cout << "Instruction: invokevirtual #" << index;

                // 1. Resolve the method reference from the constant pool
                auto method_ref = std::dynamic_pointer_cast<CpMethodRef>(cp[index]);
                auto name_and_type = std::dynamic_pointer_cast<CpNameAndType>(cp[method_ref->name_and_type_index]);
                auto descriptor = std::dynamic_pointer_cast<CpUtf8>(cp[name_and_type->descriptor_index]);
                
                std::cout << " (Method: " << std::dynamic_pointer_cast<CpUtf8>(cp[name_and_type->name_index])->bytes << ", Descriptor: " << descriptor->bytes << ")" << std::endl;

                // 2. Based on descriptor, decide how to handle the call
                if (descriptor->bytes == "(Ljava/lang/String;)V") {
                    // Handle println(String)
                    int string_cp_index = operand_stack.top();
                    operand_stack.pop();
                    operand_stack.pop(); // Pop dummy object ref for System.out

                    // Resolve string from Constant Pool
                    auto str_const = std::dynamic_pointer_cast<CpString>(cp[string_cp_index]);
                    auto utf8_const = std::dynamic_pointer_cast<CpUtf8>(cp[str_const->string_index]);
                    
                    std::cout << ">> JVM OUTPUT: " << utf8_const->bytes << std::endl;
                } else if (descriptor->bytes == "(I)V") {
                    // Handle println(int)
                    int value = operand_stack.top();
                    operand_stack.pop();
                    operand_stack.pop(); // pop dummy system.out
                    std::cout << ">> JVM OUTPUT: " << value << std::endl;
                } else {
                    // For other invokevirtual calls we don't support, just pop the arguments.
                    // This is a simplification! A real JVM would count args from the descriptor.
                    int arg = operand_stack.top();
                    operand_stack.pop();
                    operand_stack.pop(); // object ref
                     std::cout << "Warning: Simplified handling for invokevirtual descriptor " << descriptor->bytes << std::endl;
                }

                pc += 3;
                break;
            }

            case OP_IF_ICMPGT: {
                // Format: if_icmpgt branchbyte1 branchbyte2
                // Pops val2, val1. If val1 > val2, jump.
                int16_t offset = (code[pc + 1] << 8) | code[pc + 2];
                int val2 = operand_stack.top(); operand_stack.pop();
                int val1 = operand_stack.top(); operand_stack.pop();

                if (val1 > val2) {
                    std::cout << "Instruction: if_icmpgt (Branch taken -> offset " << offset << ")" << std::endl;
                    pc += offset; // JUMP!
                } else {
                    std::cout << "Instruction: if_icmpgt (Branch not taken)" << std::endl;
                    pc += 3; // Continue to next instruction
                }
                break;
            }

            case OP_GOTO: {
                // Format: goto branchbyte1 branchbyte2
                int16_t offset = (code[pc + 1] << 8) | code[pc + 2];
                std::cout << "Instruction: goto (Loop back -> offset " << offset << ")" << std::endl;
                pc += offset; // JUMP!
                break;
            }

            case OP_IINC: {
                // Format: iinc index const
                // Increments local variable at 'index' by 'const'
                uint8_t index = code[pc + 1];
                int8_t constant = code[pc + 2]; // signed byte

                local_variables[index] += constant;
                std::cout << "Instruction: iinc (Var " << (int)index << " += " << (int)constant << ")" << std::endl;
                pc += 3;
                break;
            }

            // NOTE: Your java compiler version might use 'if_icmple'
            case OP_IF_ICMPLE: {
                 int16_t offset = (code[pc + 1] << 8) | code[pc + 2];
                 int val2 = operand_stack.top(); operand_stack.pop();
                 int val1 = operand_stack.top(); operand_stack.pop();

                 if (val1 <= val2) {
                     std::cout << "Instruction: if_icmple (Branch taken -> offset " << offset << ")" << std::endl;
                     pc += offset;
                 } else {
                     std::cout << "Instruction: if_icmple (Branch not taken)" << std::endl;
                     pc += 3;
                 }
                 break;
            }

            case OP_IF_ICMPGE: {
                // Format: if_icmpge branchbyte1 branchbyte2
                // Pops val2, val1. If val1 >= val2, jump.
                int16_t offset = (code[pc + 1] << 8) | code[pc + 2];
                int val2 = operand_stack.top(); operand_stack.pop();
                int val1 = operand_stack.top(); operand_stack.pop();

                if (val1 >= val2) {
                    std::cout << "Instruction: if_icmpge (Branch taken -> offset " << offset << ")" << std::endl;
                    pc += offset;
                } else {
                    std::cout << "Instruction: if_icmpge (Branch not taken)" << std::endl;
                    pc += 3;
                }
                break;
            }

            case OP_RETURN:
                std::cout << "Instruction: return" << std::endl;
                return 0;
            case OP_LDC:
            {
                // ldc indexbyte
                uint8_t index = code[pc + 1];
                std::cout << "Instruction: ldc #" << (int)index << " (Load Constant)" << std::endl;
                // In our CP, this index points to the String "Hello, World!"
                operand_stack.push(index);
                pc += 2;
                break;
            }

            case OP_NEWARRAY: {
                // Format: newarray atype
                uint8_t atype = code[pc + 1]; // 10 = int, 4 = boolean, etc.

                int count = operand_stack.top(); operand_stack.pop();

                // Create the array object
                auto obj = std::make_shared<JavaObject>();
                obj->class_name = "[I"; // Internal name for int[]
                obj->array_data.resize(count, 0); // Initialize with zeros

                heap.push_back(obj);
                int obj_ref = heap.size() - 1;
                operand_stack.push(obj_ref);

                std::cout << "Instruction: newarray (Created int[" << count << "] at Heap " << obj_ref << ")" << std::endl;
                pc += 2;
                break;
            }

            case OP_ARRAYLENGTH: {
                int obj_ref = operand_stack.top(); operand_stack.pop();
                int length = heap[obj_ref]->array_data.size();

                std::cout << "Instruction: arraylength (Len: " << length << ")" << std::endl;
                operand_stack.push(length);
                pc += 1;
                break;
            }

            case OP_IASTORE: {
                // Stack: arrayref, index, value -> (Empty)
                int value = operand_stack.top(); operand_stack.pop();
                int index = operand_stack.top(); operand_stack.pop();
                int obj_ref = operand_stack.top(); operand_stack.pop();

                // Safety check
                if (index < 0 || index >= heap[obj_ref]->array_data.size()) {
                    std::cerr << "Error: ArrayIndexOutOfBoundsException: " << index << std::endl;
                    return 0; // Simulate JVM exit on error
                }

                heap[obj_ref]->array_data[index] = value;
                std::cout << "Instruction: iastore (arr[" << index << "] = " << value << ")" << std::endl;
                pc += 1;
                break;
            }

            case OP_IALOAD: {
                // Stack: arrayref, index -> value
                int index = operand_stack.top(); operand_stack.pop();
                int obj_ref = operand_stack.top(); operand_stack.pop();

                // Safety check
                if (index < 0 || index >= heap[obj_ref]->array_data.size()) {
                    std::cerr << "Error: ArrayIndexOutOfBoundsException: " << index << std::endl;
                    return 0; // Simulate JVM exit on error
                }

                int value = heap[obj_ref]->array_data[index];

                std::cout << "Instruction: iaload (Read " << value << " from index " << index << ")" << std::endl;
                operand_stack.push(value);
                pc += 1;
                break;
            }

            default:
                // Skip unsupported opcodes for now
                std::cout << "Skipping opcode: 0x" << std::hex << (int)opcode << std::dec << std::endl;
                pc += 1;
                break;
            }
        }
        return 0;
    }
};
```

---

### Critical Fixes for Data Persistence

The JIT profiling counters and `jit_code_ptr` must persist across method calls. This means the `Interpreter` must operate on *references* to the actual `MethodInfo` objects.

Here's how to ensure data persistence:

1.  **Update `ClassReader.h`**: Change `get_methods()` to return a reference.
    ```cpp
    // src/ClassReader.h
    // ...
    class ClassReader
    {
    // ...
    public:
        // ...
        std::vector<MethodInfo>& get_methods() { return methods; } // <--- Return reference
        // ...
    };
    ```

2.  **Update `main.cpp`**: Pass a reference to the `Interpreter` constructor and find the `main` method within that referenced vector.

    ```cpp
    // src/main.cpp
    #include "ClassReader.h"
    #include "Interpreter.h"
    #include <iostream>

    int main(int argc, char *argv[])
    {
        if (argc < 2)
        {
            std::cerr << "Usage: " << argv[0] << " <class_file>" << std::endl;
            return 1;
        }

        // 1. Parse the Class
        ClassReader reader(argv[1]);
        reader.parse();

        // 2. Initialize Interpreter with the Constant Pool AND ALL METHODS
        // Pass the methods by reference to the interpreter
        std::vector<MethodInfo>& all_methods_ref = reader.get_methods();
        Interpreter interpreter(reader.get_constant_pool(), all_methods_ref);

        // Find the 'main' method within the *referenced* vector to ensure JIT changes persist
        MethodInfo* main_method_ptr = nullptr;
        for (auto &method : all_methods_ref)
        {
            if (method.name == "main" && method.descriptor == "([Ljava/lang/String;)V")
            {
                main_method_ptr = &method;
                break;
            }
        }

        if (!main_method_ptr)
        {
            std::cerr << "Error: Main method not found." << std::endl;
            return 1;
        }
        // Run the main method, passing it by reference
        interpreter.run(*main_method_ptr);

        return 0;
    }
    ```

---

### Step 4: Create a JIT Test Case

We need a Java program that calls a method enough times to trigger the JIT threshold (5 times).

Create `tests/JitTest.java`:

```java
public class JitTest {
    // This method is simple enough for our JIT to handle
    // It maps to: bipush 42, ireturn
    public static int getNumber() {
        return 42;
    }

    public static void main(String[] args) {
        int sum = 0;
        // Loop 10 times to trigger JIT (Threshold is 5)
        for (int i = 0; i < 10; i++) {
            sum = sum + getNumber();
        }
        System.out.println(sum); // Should be 420
    }
}
```

Compile it:

```bash
javac -d build tests/JitTest.java
```

---

### Step 5: Compile and Run (On your Mac M1/M2/M3)

Compile your JVM. Ensure all C++ source files are included:

```bash
clang++ -std=c++17 src/*.cpp -o build/myjvm
```

Run the test. Remember that our `main.cpp` requires the full filename including the `.class` extension:

```bash
build/myjvm build/JitTest.class
```

### What you should see

If successful, you will see the interpreter run the first few times, and then suddenly the JIT kicks in for `getNumber()`. The output will clearly show the transition from `INTERPRETING FRAME` to `EXECUTING NATIVE CODE`.

```text
--- INTERPRETING FRAME: main (call #1) ---
Instruction: istore_1
Instruction: istore_2
Instruction: iload_2
Instruction: bipush 10
Instruction: if_icmpge (Branch not taken)
Instruction: iload_1
Instruction: invokestatic (Calling getNumber)

--- INTERPRETING FRAME: getNumber (call #1) ---
Instruction: bipush 42
Instruction: ireturn (42)
--- RETURNED TO FRAME: main (Result: 42) ---
... (This pattern for getNumber repeats for calls #2, #3, #4) ...
--- INTERPRETING FRAME: getNumber (call #4) ---
Instruction: bipush 42
Instruction: ireturn (42)
--- RETURNED TO FRAME: main (Result: 42) ---
Instruction: iadd
Instruction: istore_1
Instruction: iinc (Var 2 += 1)
Instruction: goto (Loop back -> offset -15)
Instruction: iload_2
Instruction: bipush 10
Instruction: if_icmpge (Branch not taken)
Instruction: iload_1
Instruction: invokestatic (Calling getNumber)

--- INTERPRETING FRAME: getNumber (call #5) ---
Instruction: bipush 42
Instruction: ireturn (42)
--- JIT COMPILED getNumber TO ARM64 NATIVE CODE ---
--- RETURNED TO FRAME: main (Result: 42) ---
Instruction: iadd
Instruction: istore_1
Instruction: iinc (Var 2 += 1)
Instruction: goto (Loop back -> offset -15)
Instruction: iload_2
Instruction: bipush 10
Instruction: if_icmpge (Branch not taken)
Instruction: iload_1
Instruction: invokestatic (Calling getNumber)

--- EXECUTING NATIVE CODE FOR getNumber ---
--- RETURNED TO FRAME: main (Result: 42) ---
... (This pattern for EXECUTING NATIVE CODE repeats for remaining calls) ...
Instruction: invokevirtual #19 (Method: println, Descriptor: (I)V)
>> JVM OUTPUT: 420
Instruction: return
```

### Summary of what just happened

1.  **Profiling:** Your JVM counted how many times `getNumber()` was called.
2.  **Threshold:** On the 5th call, it paused execution.
3.  **Compilation:**
    -   It saw `bipush 42` -> Encoded to `0x52800540` (`MOV W0, #42`).
    -   It saw `ireturn` -> Encoded to `0xD65F03C0` (`RET`).
4.  **Memory:** It asked MacOS for memory, wrote the bytes, and used `mprotect` + `sys_icache_invalidate` to make it safe for the M1 chip.
5.  **Execution:** It jumped the CPU Instruction Pointer to that memory address.

You have now successfully optimized Java bytecode into native Apple Silicon machine code!
