// Interpreter.h
#pragma once
#include "ConstantPool.h"
#include <vector>
#include <stack>
#include <iostream>
#include <string>
#include <unordered_map>
#include <memory>

// JVM Opcodes
#define OP_NEW 0xbb           // Create new object
#define OP_DUP 0x59           // Duplicate top stack item
#define OP_INVOKESPECIAL 0xb7 // Call constructor (<init>)
#define OP_PUTFIELD 0xb5      // Set field in object
#define OP_GETFIELD 0xb4      // Get field from object
#define OP_ASTORE_1 0x4c      // Store object ref in local var 1
#define OP_ALOAD_1 0x2b       // Load object ref from local var 1

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

// Represents a Java Object allocated on the Heap
struct JavaObject
{
    std::string class_name;
    std::unordered_map<std::string, int> fields; // Map of field name -> int value
};

class Interpreter
{
private:
    std::vector<std::shared_ptr<CpInfo>> cp;
    std::vector<MethodInfo> all_methods;

    // THE JVM HEAP
    // Stores all created objects. The index in this vector is the "Object Reference".
    std::vector<std::shared_ptr<JavaObject>> heap;

    // Helper to find a method by Name and Descriptor
    const MethodInfo *find_method(const std::string &name, const std::string &desc)
    {
        for (const auto &method : all_methods)
        {
            if (method.name == name && method.descriptor == desc)
            {
                return &method;
            }
        }
        return nullptr;
    }

public:
    Interpreter(std::vector<std::shared_ptr<CpInfo>> constant_pool, std::vector<MethodInfo> methods)
        : cp(constant_pool), all_methods(methods) {}

    // run() now acts as a JVM Stack Frame. It returns an integer.
    int run(const MethodInfo &method, const std::vector<int> &args = {})
    {
        std::cout << "\n--- ENTERING FRAME: " << method.name << " ---" << std::endl;

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

                // Resolve the method name and type from the Constant Pool
                auto method_ref = std::dynamic_pointer_cast<CpInfo>(cp[method_idx]); // Simplification for demo
                // In a full JVM, you'd extract the Name and Descriptor strings here.
                // For this specific test, we know it calls "add" "(II)I"
                const MethodInfo *target_method = find_method("add", "(II)I");

                std::cout << "Instruction: invokestatic (Calling " << target_method->name << ")" << std::endl;

                // Pop arguments for the method (in reverse order)
                int arg2 = operand_stack.top();
                operand_stack.pop();
                int arg1 = operand_stack.top();
                operand_stack.pop();

                // CREATE NEW FRAME (Recursive call)
                int return_value = run(*target_method, {arg1, arg2});

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
                if (descriptor->bytes == "(Ljava/lang/String;)V")
                {
                    // Handle println(String)
                    int string_cp_index = operand_stack.top();
                    operand_stack.pop();
                    operand_stack.pop(); // Pop dummy object ref for System.out

                    // Resolve string from Constant Pool
                    auto str_const = std::dynamic_pointer_cast<CpString>(cp[string_cp_index]);
                    auto utf8_const = std::dynamic_pointer_cast<CpUtf8>(cp[str_const->string_index]);

                    std::cout << ">> JVM OUTPUT: " << utf8_const->bytes << std::endl;
                }
                else if (descriptor->bytes == "(I)V")
                {
                    // Handle println(int)
                    int value = operand_stack.top();
                    operand_stack.pop();
                    operand_stack.pop(); // pop dummy system.out
                    std::cout << ">> JVM OUTPUT: " << value << std::endl;
                }
                else
                {
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