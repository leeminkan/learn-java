// src/Jit.h
#pragma once
#include "ConstantPool.h"
#include <vector>
#include <sys/mman.h>
#include <iostream>
#include <libkern/OSCacheControl.h> // REQUIRED for MacOS M1/M2 cache flushing

// Define the function pointer type for our JITed code
typedef int (*JitFunction)();

class JitCompiler
{
public:
    // ARM64 Instruction Hex Codes (Little Endian)
    // RET: Returns from function
    const uint32_t ARM64_RET = 0xD65F03C0;

    // Helper to encode "MOV W0, #immediate" (Move constant into Return Register W0)
    // Instruction format: [ 1 0 1 (bits 31-29) ... imm16 (bits 20-5) ... Rd (bits 4-0) ]
    // Base opcode for MOVZ (Move with Zero) is 0x52800000
    uint32_t encode_mov_w0(uint16_t immediate)
    {
        return 0x52800000 | (immediate << 5);
    }

    // Main Compile Function
    void *compile(const MethodInfo &method)
    {
        std::vector<uint32_t> machine_code;

        // SIMPLE PASS: Look for "iconst/bipush + return" pattern
        // We are iterating through JVM bytecodes and converting to ARM64 opcodes
        for (size_t i = 0; i < method.bytecode.size(); ++i)
        {
            uint8_t opcode = method.bytecode[i];

            if (opcode >= 0x02 && opcode <= 0x08)
            {                                                      // iconst_m1 to iconst_5
                machine_code.push_back(encode_mov_w0(opcode - 3)); // e.g., iconst_0 (0x03) -> 0
            }
            else if (opcode == 0x10)
            { // bipush
                uint8_t val = method.bytecode[i + 1];
                machine_code.push_back(encode_mov_w0(val));
                i++; // Skip operand
            }
            else if (opcode == 0xac)
            { // ireturn
                machine_code.push_back(ARM64_RET);
            }
            // For this demo, we ignore other opcodes or fallback to interpreter
        }

        if (machine_code.empty() || machine_code.back() != ARM64_RET)
        {
            // We only JIT simple methods that return a constant.
            // If it doesn't end with a RET, it's too complex for our simple JIT.
            return nullptr;
        }

        // 1. Allocate Memory (READ + WRITE initially)
        size_t size = machine_code.size() * sizeof(uint32_t);
        void *mem = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);

        if (mem == MAP_FAILED)
        {
            std::cerr << "JIT: Memory allocation failed!" << std::endl;
            return nullptr;
        }

        // 2. Write Machine Code
        memcpy(mem, machine_code.data(), size);

        // 3. Secure Memory (Switch to READ + EXECUTE)
        // MacOS on M1 forbids Writable+Executable memory. We must flip the switch.
        mprotect(mem, size, PROT_READ | PROT_EXEC);

        // 4. FLUSH CACHE (Critical for M1/M2/M3)
        // Tells the CPU "I changed code in memory, please clear your instruction cache"
        sys_icache_invalidate(mem, size);

        std::cout << "--- JIT COMPILED " << method.name << " TO ARM64 NATIVE CODE ---" << std::endl;
        return mem;
    }
};
