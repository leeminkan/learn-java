# 4. Case Study: Just-In-Time (JIT) Compilation

## The Problem: Interpretation Overhead

While interpreting bytecode allows for portability, it incurs significant overhead. Each bytecode instruction must be fetched, decoded, and then executed by a C++ function. For "hot" methods (those executed frequently), this overhead can dominate execution time.

## The Solution: JIT Compilation

Just-In-Time (JIT) compilation is a performance optimization where selected portions of bytecode are translated into native machine code *during runtime*. This native code can then be executed directly by the CPU, bypassing the interpreter and significantly improving performance.

### Key Aspects of Our ARM64 JIT

Our JIT is designed for Apple Silicon (ARM64) and demonstrates the core principles of JIT compilation, specifically for a "leaf JIT" (a JIT that only optimizes methods that don't call other methods, or at least, don't call methods that need JIT compilation themselves).

1.  **Instruction Set Differences (ARM64 vs. x86)**:
    -   **Fixed-width Instructions**: Unlike x86's variable-length instructions, ARM64 uses fixed 32-bit instructions. Generating machine code involves constructing specific 32-bit integer values that represent the desired ARM64 assembly instructions.
    -   **Register Usage**: We use `W0` (the lower 32 bits of register `X0`) as the return register for integer values, as per ARM64 ABI conventions. `MOVZ W0, #immediate` is used to move a constant into `W0`. `RET` is the return instruction.

2.  **Memory Protection (W^X)**:
    -   Modern operating systems (especially macOS) enforce **W^X (Write XOR Execute)** security. Memory regions cannot be simultaneously Writable and Executable.
    -   Our JIT handles this by:
        1.  Allocating memory with `mmap` that is initially `PROT_READ | PROT_WRITE`.
        2.  Writing the generated machine code into this memory using `memcpy`.
        3.  Changing the memory protection to `PROT_READ | PROT_EXEC` using `mprotect`, making it executable but no longer writable.

3.  **Cache Coherency (`sys_icache_invalidate`)**:
    -   Apple Silicon processors have separate instruction and data caches. After writing new machine code into memory, the CPU's instruction cache might still hold stale data for that memory region.
    -   `sys_icache_invalidate` is a crucial macOS-specific function that flushes the instruction cache for a given memory range, ensuring the CPU fetches the newly written code. Without this, the CPU might try to execute cached "empty" memory, leading to crashes.

### JIT Integration Pipeline

The JIT is integrated into the `Interpreter` class with the following pipeline:

1.  **Profiling**:
    -   Each `MethodInfo` struct now includes a `call_count`.
    -   Every time `Interpreter::run` is invoked for a method, its `call_count` is incremented.

2.  **Hot Method Detection**:
    -   A threshold (e.g., 5 calls for demonstration) determines if a method is "hot" enough to warrant JIT compilation.

3.  **Compilation Trigger**:
    -   When a method's `call_count` reaches the threshold, the `JitCompiler::compile` method is invoked for that `MethodInfo`.
    -   The `compile` method inspects the method's bytecode. Our simple JIT looks for patterns like `iconst_X` or `bipush Y` followed by `ireturn`.

4.  **Code Generation**:
    -   If the bytecode matches an optimizable pattern (e.g., `bipush 42; ireturn`), the `JitCompiler` generates the corresponding ARM64 machine code (`MOV W0, #42; RET`).

5.  **Memory Management & Protection**:
    -   Memory is allocated (`mmap`), machine code is written (`memcpy`), and protection is switched (`mprotect`).
    -   The instruction cache is flushed (`sys_icache_invalidate`).

6.  **Native Execution**:
    -   The `jit_code_ptr` in `MethodInfo` is updated with the address of the newly compiled native code.
    -   Subsequent calls to this method immediately jump to and execute the native code, skipping the interpreter.
    -   This is achieved by casting the `jit_code_ptr` to a function pointer `JitFunction` and calling it directly.

This basic JIT demonstrates the complex interplay between runtime profiling, dynamic code generation, and low-level operating system and hardware considerations. It significantly enhances the JVM's capability beyond pure interpretation.
