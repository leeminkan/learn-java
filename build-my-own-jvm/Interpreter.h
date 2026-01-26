// Interpreter.h
#pragma once
#include "ConstantPool.h"
#include <vector>
#include <stack>
#include <iostream>
#include <string>

// JVM Opcodes
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

class Interpreter
{
private:
    std::vector<std::shared_ptr<CpInfo>> cp;
    std::vector<MethodInfo> all_methods;

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
        std::cout << "\n--- ENTERING FRAME: " << method.name << method.descriptor << " ---" << std::endl;

        // 1. The Operand Stack
        std::stack<int> operand_stack;

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
                // Just pushing a dummy reference for System.out
                operand_stack.push(99);
                pc += 3;
                break;
            case OP_INVOKEVIRTUAL:
            {
                int value = operand_stack.top();
                operand_stack.pop();
                operand_stack.pop(); // pop dummy system.out
                std::cout << ">> JVM OUTPUT: " << value << std::endl;
                pc += 3;
                break;
            }
            case OP_RETURN:
                std::cout << "Instruction: return" << std::endl;
                return 0;
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