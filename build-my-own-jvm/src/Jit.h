// src/Jit.h
#pragma once
#include "ConstantPool.h"
#include <vector>
#include <stack> // New: for reg_stack
#include <sys/mman.h>
#include <iostream>
#include <cstring>                  // New: For memcpy
#include <libkern/OSCacheControl.h> // Required for M1/M2/M3
#include <unistd.h>                 // New: For sysconf

typedef int (*JitFunction)(int, int); // Update: Function now accepts 2 integers

class JitCompiler
{
public:
    // ARM64 Opcodes
    const uint32_t ARM64_RET = 0xD65F03C0;

    // Encode: MOV Wd, #immediate
    uint32_t encode_mov_imm(uint8_t rd, uint16_t imm)
    {
        return 0x52800000 | (imm << 5) | rd;
    }

    // Encode: ADD Wd, Wn, Wm  (Wd = Wn + Wm)
    // Base: 0x0B000000
    uint32_t encode_add(uint8_t rd, uint8_t rn, uint8_t rm)
    {
        return 0x0B000000 | (rm << 16) | (rn << 5) | rd;
    }

    void *compile(const MethodInfo &method)
    {
        std::vector<uint32_t> machine_code;

        // This simulates the JVM Operand Stack during JIT compilation
        // But instead of values, we store which Register (0 for W0, 1 for W1) holds the value.
        std::stack<uint8_t> reg_stack;

        bool possible = true;

        for (uint8_t opcode : method.bytecode)
        {

            if (opcode == 0x1a)
            { // iload_0
                // In ARM64, Arg 1 is always in W0.
                // We "push" the knowledge that W0 has our data.
                reg_stack.push(0);
            }
            else if (opcode == 0x1b)
            { // iload_1
                // Arg 2 is always in W1.
                reg_stack.push(1);
            }
            else if (opcode == 0x60)
            { // iadd
                // Pop the two registers holding our values
                if (reg_stack.size() < 2)
                {
                    possible = false;
                    break;
                }

                uint8_t reg_b = reg_stack.top();
                reg_stack.pop(); // Top is 2nd operand
                uint8_t reg_a = reg_stack.top();
                reg_stack.pop(); // Next is 1st operand

                // Generate: ADD Wa, Wa, Wb  (Store result in Wa)
                machine_code.push_back(encode_add(reg_a, reg_a, reg_b));

                // Push the result register back
                reg_stack.push(reg_a);
            }
            else if (opcode == 0xac)
            { // ireturn
                if (reg_stack.empty())
                {
                    possible = false;
                    break;
                }

                uint8_t result_reg = reg_stack.top();
                reg_stack.pop();

                // If the result isn't in W0, we technically need to move it there.
                // For this simple demo, we assume the math naturally ends up in W0 (common for a + b).
                if (result_reg != 0)
                {
                    // In a full JIT, we would emit MOV W0, Wn here.
                    std::cerr << "JIT Warning: Result not in W0, skipping optimization." << std::endl;
                    possible = false;
                    break;
                }

                machine_code.push_back(ARM64_RET);
            }
            else
            {
                // If we encounter an opcode we don't know (like print), abort JIT.
                possible = false;
                break;
            }
        }

        if (!possible || machine_code.empty() || machine_code.back() != ARM64_RET)
        {
            // If it's not possible to compile, or if the method doesn't end with RET,
            // or if the reg_stack isn't empty after processing (meaning values left on stack),
            // or if the bytecode was empty.
            if (!possible)
                std::cerr << "JIT: Method " << method.name << " not eligible (unsupported opcode/stack issue)." << std::endl;
            if (machine_code.empty())
                std::cerr << "JIT: Method " << method.name << " not eligible (empty machine code)." << std::endl;
            if (!machine_code.empty() && machine_code.back() != ARM64_RET)
                std::cerr << "JIT: Method " << method.name << " not eligible (doesn't end with RET)." << std::endl;
            return nullptr;
        }

        // --- Standard M1 Memory Allocation ---
        size_t size = machine_code.size() * sizeof(uint32_t);
        // Page align the size for mprotect
        size_t page_size = sysconf(_SC_PAGESIZE);
        size_t aligned_size = (size + page_size - 1) / page_size * page_size;

        void *mem = mmap(nullptr, aligned_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);

        if (mem == MAP_FAILED)
        {
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
