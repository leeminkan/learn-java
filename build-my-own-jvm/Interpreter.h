// Interpreter.h
#pragma once
#include "ConstantPool.h"
#include <vector>
#include <stack>
#include <iostream>

// JVM Opcodes
#define OP_ICONST_5 0x08
#define OP_BIPUSH 0x10
#define OP_ISTORE_1 0x3c
#define OP_ISTORE_2 0x3d
#define OP_ISTORE_3 0x3e
#define OP_ILOAD_1 0x1b
#define OP_ILOAD_2 0x1c
#define OP_ILOAD_3 0x1d
#define OP_IADD 0x60
#define OP_GETSTATIC 0xb2
#define OP_LDC 0x12
#define OP_INVOKEVIRTUAL 0xb6
#define OP_RETURN 0xb1

class Interpreter
{
private:
    std::vector<std::shared_ptr<CpInfo>> cp;

public:
    Interpreter(std::vector<std::shared_ptr<CpInfo>> constant_pool) : cp(constant_pool) {}

    void run(const MethodInfo &method)
    {
        std::cout << "\n--- EXECUTING: " << method.name << " ---" << std::endl;

        // 1. The Operand Stack
        std::stack<int> operand_stack;

        // 2. The Local Variables Array (Sized by the compiler's max_locals)
        std::vector<int> local_variables(method.max_locals);

        const auto &code = method.bytecode;
        int pc = 0;

        while (pc < code.size())
        {
            uint8_t opcode = code[pc];

            switch (opcode)
            {
            // --- NEW MATH OPCODES ---
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

            // --- PREVIOUS OPCODES ---
            case OP_GETSTATIC:
                // Just pushing a dummy reference for System.out
                operand_stack.push(99);
                pc += 3;
                break;
            case OP_INVOKEVIRTUAL:
            {
                // For System.out.println(int)
                int value_to_print = operand_stack.top();
                operand_stack.pop();
                int system_out_ref = operand_stack.top();
                operand_stack.pop();
                std::cout << ">> JVM OUTPUT: " << value_to_print << std::endl;
                pc += 3;
                break;
            }
            case OP_RETURN:
                std::cout << "Instruction: return" << std::endl;
                return;
            default:
                // Skip unsupported opcodes for now
                std::cout << "Skipping opcode: 0x" << std::hex << (int)opcode << std::dec << std::endl;
                pc += 1;
                break;
            }
        }
    }
};