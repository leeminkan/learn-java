Handling arguments is the primary job of a JIT.

To do this, you need to understand the **ARM64 Calling Convention** (specifically how macOS passes data between functions).

### The Secret: Registers vs. Stack

- **The JVM** expects arguments to be in the "Local Variables Array" (Stack Frame).
- **The CPU (ARM64)** passes arguments in **Registers**:
- **Argument 1 (First int):** Inside Register `W0`
- **Argument 2 (Second int):** Inside Register `W1`
- **... up to W7**
- **Return Value:** Must end up in Register `W0`

### The Strategy

We will upgrade your JIT to be a "Register Tracker".

1. When it sees `iload_0`, it knows the value is waiting in **W0**.
2. When it sees `iload_1`, it knows the value is waiting in **W1**.
3. When it sees `iadd`, it will generate the machine code for `ADD W0, W0, W1`.

---

### Step 1: Upgrade `Jit.h`

We need to add the ability to encode the `ADD` instruction (register-to-register).

Replace your `src/Jit.h` with this upgraded version. I have added a simple **stack simulator** so the JIT remembers which registers hold your data.

````cpp
Handling arguments is the primary job of a JIT.

To do this, you need to understand the **ARM64 Calling Convention** (specifically how macOS passes data between functions).

### The Secret: Registers vs. Stack

-   **The JVM** expects arguments to be in the "Local Variables Array" (Stack Frame).
-   **The CPU (ARM64)** passes arguments in **Registers**:
    -   **Argument 1 (First int):** Inside Register `W0`
    -   **Argument 2 (Second int):** Inside Register `W1`
    -   **... up to W7**
-   **Return Value:** Must end up in Register `W0`

### The Strategy

We will upgrade your JIT to be a "Register Tracker".

1.  When it sees `iload_0`, it knows the value is waiting in **W0**.
2.  When it sees `iload_1`, it knows the value is waiting in **W1**.
3.  When it sees `iadd`, it will generate the machine code for `ADD W0, W0, W1`.

---

### Step 1: Upgrade `Jit.h`

We need to add the ability to encode the `ADD` instruction (register-to-register).

Replace your `src/Jit.h` with this upgraded version. It includes necessary headers and a simple **stack simulator** so the JIT remembers which registers hold your data.

```cpp
// src/Jit.h
#pragma once
#include "ConstantPool.h"
#include <vector>
#include <stack> // New: for reg_stack
#include <sys/mman.h>
#include <iostream>
#include <cstring> // New: For memcpy
#include <libkern/OSCacheControl.h> // Required for M1/M2/M3
#include <unistd.h> // New: For sysconf

typedef int (*JitFunction)(int, int); // Update: Function now accepts 2 integers

class JitCompiler {
public:
    // ARM64 Opcodes
    const uint32_t ARM64_RET = 0xD65F03C0;

    // Encode: MOV Wd, #immediate
    uint32_t encode_mov_imm(uint8_t rd, uint16_t imm) {
        return 0x52800000 | (imm << 5) | rd;
    }

    // Encode: ADD Wd, Wn, Wm  (Wd = Wn + Wm)
    // Base: 0x0B000000
    uint32_t encode_add(uint8_t rd, uint8_t rn, uint8_t rm) {
        return 0x0B000000 | (rm << 16) | (rn << 5) | rd;
    }

    void* compile(const MethodInfo& method) {
        std::vector<uint32_t> machine_code;

        // This simulates the JVM Operand Stack during JIT compilation
        // But instead of values, we store which Register (0 for W0, 1 for W1) holds the value.
        std::stack<uint8_t> reg_stack;

        bool possible = true;

        for (uint8_t opcode : method.bytecode) {

            if (opcode == 0x1a) { // iload_0
                // In ARM64, Arg 1 is always in W0.
                // We "push" the knowledge that W0 has our data.
                reg_stack.push(0);
            }
            else if (opcode == 0x1b) { // iload_1
                // Arg 2 is always in W1.
                reg_stack.push(1);
            }
            else if (opcode == 0x60) { // iadd
                // Pop the two registers holding our values
                if (reg_stack.size() < 2) { possible = false; break; }

                uint8_t reg_b = reg_stack.top(); reg_stack.pop(); // Top is 2nd operand
                uint8_t reg_a = reg_stack.top(); reg_stack.pop(); // Next is 1st operand

                // Generate: ADD Wa, Wa, Wb  (Store result in Wa)
                machine_code.push_back(encode_add(reg_a, reg_a, reg_b));

                // Push the result register back
                reg_stack.push(reg_a);
            }
            else if (opcode == 0xac) { // ireturn
                if (reg_stack.empty()) { possible = false; break; }

                uint8_t result_reg = reg_stack.top(); reg_stack.pop();

                // If the result isn't in W0, we technically need to move it there.
                // For this simple demo, we assume the math naturally ends up in W0 (common for a + b).
                if (result_reg != 0) {
                   // In a full JIT, we would emit MOV W0, Wn here.
                   std::cerr << "JIT Warning: Result not in W0, skipping optimization." << std::endl;
                   possible = false; break;
                }

                machine_code.push_back(ARM64_RET);
            }
            else {
                // If we encounter an opcode we don't know (like print), abort JIT.
                possible = false;
                break;
            }
        }

        if (!possible || machine_code.empty() || machine_code.back() != ARM64_RET) {
            // If it's not possible to compile, or if the method doesn't end with RET,
            // or if the reg_stack isn't empty after processing (meaning values left on stack),
            // or if the bytecode was empty.
            if (!possible) std::cerr << "JIT: Method " << method.name << " not eligible (unsupported opcode/stack issue)." << std::endl;
            if (machine_code.empty()) std::cerr << "JIT: Method " << method.name << " not eligible (empty machine code)." << std::endl;
            if (!machine_code.empty() && machine_code.back() != ARM64_RET) std::cerr << "JIT: Method " << method.name << " not eligible (doesn't end with RET)." << std::endl;
            return nullptr;
        }

        // --- Standard M1 Memory Allocation ---
        size_t size = machine_code.size() * sizeof(uint32_t);
        // Page align the size for mprotect
        size_t page_size = sysconf(_SC_PAGESIZE);
        size_t aligned_size = (size + page_size - 1) / page_size * page_size;

        void* mem = mmap(nullptr, aligned_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);

        if (mem == MAP_FAILED) {
            std::cerr << "JIT: Memory allocation failed!" << std::endl;
            return nullptr;
        }

        memcpy(mem, machine_code.data(), size);
        mprotect(mem, aligned_size, PROT_READ | PROT_EXEC);
        sys_icache_invalidate(mem, aligned_size);

        std::cout << "--- JIT COMPILED " << method.name << " (Supports Arguments) ---" << std::endl;
        return mem;
    }
};


````

---

### Step 2: Update `Interpreter.h`

We need to update the `run()` method to handle passing arguments to the native function pointer.

Modify the JIT block in `src/Interpreter.h`:

```cpp
        // ... inside run() ...

        // 1. If we already compiled this, run the native ARM64 code!
        if (method.jit_code_ptr != nullptr) {
            // Function pointer type: int func(int a, int b)
            typedef int (*JitFunctionArgs)(int, int);
            JitFunctionArgs func = (JitFunctionArgs)method.jit_code_ptr;

            // Get arguments from the vector if they exist
            int arg1 = (args.size() > 0) ? args[0] : 0;
            int arg2 = (args.size() > 1) ? args[1] : 0;

            // Execute Native Code!
            int result = func(arg1, arg2);
            return result;
        }

        // ... continue with profiling/interpreter logic ...

```

---

### Step 3: Create the Test Case

Create `tests/JitArgsTest.java`. We will create an `add` method and call it repeatedly.

```java
public class JitArgsTest {
    // This maps to: iload_0, iload_1, iadd, ireturn
    public static int add(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        int sum = 0;
        // Loop to trigger JIT (Threshold 5)
        for (int i = 0; i < 10; i++) {
            sum = sum + add(10, 20);
        }
        System.out.println(sum); // Expect: 300
    }
}

```

Compile it:

```bash
javac -d build tests/JitArgsTest.java

```

---

### Step 4: Run It

Compile your JVM and run:

```bash
clang++ -std=c++17 src/main.cpp src/ClassReader.cpp -o build/myjvm -I src
./build/myjvm build/JitArgsTest.class

```

### Expected Output

```text
--- ENTERING FRAME: main ---
...
--- ENTERING FRAME: add ---
Instruction: iload_0 (0)
Instruction: iload_1 (1)
Instruction: iadd
Instruction: ireturn (30)
... (repeats 5 times) ...

--- JIT COMPILED add (Supports Arguments) ---

... (Remaining calls run instantly on the M1 CPU) ...

>> JVM OUTPUT: 300

```

### What just happened?

1. **Bytecode Analysis:** Your JIT saw `iload_0` and pushed "Register 0" to its simulation stack. It saw `iload_1` and pushed "Register 1".
2. **Code Gen:** When it saw `iadd`, it popped "Reg 0" and "Reg 1" and generated the ARM64 instruction `0x0B010000` (`ADD W0, W0, W1`).
3. **Execution:** When you called `add(10, 20)`, your C++ code passed `10` and `20` to the generated function.

- `10` went into CPU Register `W0`.
- `20` went into CPU Register `W1`.
- The CPU added them and left `30` in `W0`.
- The function returned `30`.
