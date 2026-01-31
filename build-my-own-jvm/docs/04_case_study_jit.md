# 4. Case Study: Just-In-Time (JIT) Compilation

## The Problem: Interpretation Overhead

While interpreting bytecode allows for portability, it incurs significant overhead. Each bytecode instruction must be fetched, decoded, and then executed by a C++ function. For "hot" methods (those executed frequently), this overhead can dominate execution time.

## The Solution: JIT Compilation

Just-In-Time (JIT) compilation is a performance optimization where selected portions of bytecode are translated into native machine code _during runtime_. This native code can then be executed directly by the CPU, bypassing the interpreter and significantly improving performance.

### Key Aspects of Our ARM64 JIT

Our JIT is designed for Apple Silicon (ARM64) and demonstrates the core principles of JIT compilation. It functions as a "leaf JIT", currently optimizing methods that are simple enough not to require complex stack frames or calls to other JIT-compiled methods.

1.  **Instruction Set Differences (ARM64 vs. x86)**:
    - **Fixed-width Instructions**: Unlike x86's variable-length instructions, ARM64 uses fixed 32-bit instructions. Generating machine code involves constructing specific 32-bit integer values that represent the desired ARM64 assembly instructions.
    - **Register Usage**: We use `W0` (the lower 32 bits of register `X0`) as the return register for integer values, as per ARM64 ABI conventions. `MOVZ W0, #immediate` is used to move a constant into `W0`. `RET` is the return instruction.

2.  **ARM64 Calling Convention for Arguments**:

    A critical aspect for methods that take arguments (like `add(int a, int b)`) is understanding how the CPU receives them. On ARM64 (macOS):
    - The first integer argument is passed in CPU register `W0`.

    - The second integer argument is passed in CPU register `W1`.

    - And so on, up to `W7`.

    - The return value must also be placed in `W0`.

    #### Mechanism of Calling JIT'd Code from C++

    When you call the JIT-compiled function from C++ like `func(arg1, arg2)`, there's a specific mechanism at play dictated by the **Application Binary Interface (ABI)** or **Calling Convention**. This allows C++ code to seamlessly interact with your JIT-generated machine code.

    Here's a step-by-step breakdown of what happens on an ARM64 (Apple Silicon) Mac:
    1.  **The `typedef` (Defining the Contract)**:

        ```cpp

        typedef int (*JitFunctionArgs)(int, int);

        ```

        This C++ `typedef` acts as a contract. It tells the C++ compiler: "The code at `method.jit_code_ptr` expects two integer arguments and will return one integer."

    2.  **C++ Compiler Prepares Registers**:

        When `func(arg1, arg2)` is invoked, your C++ compiler (Clang) generates assembly instructions _before_ jumping to your JIT code. These instructions move your C++ variables (`arg1`, `arg2`) into the designated CPU registers according to the ARM64 ABI:
        - `arg1`'s value is moved into CPU Register **W0**.

        - `arg2`'s value is moved into CPU Register **W1**.

        (The compiler also loads the JIT code's memory address into a temporary register).

    3.  **The Jump (`BLR` - Branch with Link to Register)**:

        The C++ code then executes a `BLR` instruction, which:
        - Changes the CPU's Program Counter (`PC`) to the starting address of your JIT-compiled code.

        - Saves the "return address" (the next instruction in the C++ code) into a special register called the **Link Register (LR/x30)**.

    4.  **JIT Code Executes**:

        The CPU is now executing your JIT-generated machine code. Because the C++ compiler followed the ABI, your JIT code can _assume_ the arguments are already in `W0` and `W1`. For example, an `ADD W0, W0, W1` instruction directly uses these registers.

    5.  **The Return (`RET`)**:

        Your JIT code ends with `RET` (`0xD65F03C0`). This instruction tells the CPU to:
        - Read the return address from the Link Register (LR).

        - Jump back to that C++ address, effectively returning control to the C++ code that called `func`.

    6.  **C++ Retrieves Result**:

        Back in the C++ code, the compiler knows that the integer return value from the called function will be in `W0` (another ABI rule). It reads the value from `W0` and assigns it to your `result` variable.

    This entire process allows different layers of code (C++ and native machine code) to communicate efficiently and correctly by adhering to a predefined contract (the ABI).

3.  **Memory Protection (W^X)**:
    - Modern operating systems (especially macOS) enforce **W^X (Write XOR Execute)** security. Memory regions cannot be simultaneously Writable and Executable.
    - Our JIT handles this by:
      1.  Allocating memory with `mmap` that is initially `PROT_READ | PROT_WRITE`.
      2.  Writing the generated machine code into this memory using `memcpy`.
      3.  Changing the memory protection to `PROT_READ | PROT_EXEC` using `mprotect`, making it executable but no longer writable.

4.  **Cache Coherency (`sys_icache_invalidate`)**:
    - Apple Silicon processors have separate instruction and data caches. After writing new machine code into memory, the CPU's instruction cache might still hold stale data for that memory region.
    - `sys_icache_invalidate` is a crucial macOS-specific function that flushes the instruction cache for a given memory range, ensuring the CPU fetches the newly written code. Without this, the CPU might try to execute cached "empty" memory, leading to crashes.

### JIT Integration Pipeline

The JIT is integrated into the `Interpreter` class with the following pipeline:

1.  **Profiling**:
    - Each `MethodInfo` struct now includes a `call_count`.
    - Every time `Interpreter::run` is invoked for a method, its `call_count` is incremented.

2.  **Hot Method Detection**:
    - A threshold (e.g., 5 calls for demonstration) determines if a method is "hot" enough to warrant JIT compilation.

3.  **Compilation Trigger**:
    - When a method's `call_count` reaches the threshold, the `JitCompiler::compile` method is invoked for that `MethodInfo`.
    - The `compile` method inspects the method's bytecode.

4.  **Code Generation: The "Register Tracker" Strategy**:
    - Our JIT implements a simple **Register Tracker**. It simulates the JVM's operand stack during compilation, but instead of values, it tracks _which ARM64 register_ currently holds the result of an operation.
    - **`iload_0`**: When the bytecode pushes the first local variable (`iload_0`), the JIT knows this value is already in `W0` (due to the calling convention), so it pushes '0' (representing `W0`) onto its internal `reg_stack`.
    - **`iload_1`**: Similarly, for `iload_1`, it pushes '1' (representing `W1`) onto its `reg_stack`.
    - **`iadd`**: When `iadd` is encountered, the JIT pops the two top registers from `reg_stack` (e.g., `W1` then `W0`). It then generates the ARM64 `ADD` instruction (e.g., `ADD W0, W0, W1`) to perform the sum directly in registers, storing the result in one of the source registers (here, `W0`). The result register (`W0`) is then pushed back onto `reg_stack`.
    - **`ireturn`**: For `ireturn`, it expects `W0` to hold the final result, and then generates the `RET` instruction.

5.  **Memory Management & Protection**:
    - Memory is allocated (`mmap`), machine code is written (`memcpy`), and protection is switched (`mprotect`).
    - The instruction cache is flushed (`sys_icache_invalidate`).

6.  **Native Execution**:
    - The `jit_code_ptr` in `MethodInfo` is updated with the address of the newly compiled native code.
    - Subsequent calls to this method immediately jump to and execute the native code, skipping the interpreter.
    - This is achieved by casting the `jit_code_ptr` to a function pointer (e.g., `JitFunction(int, int)`) and calling it directly with the method's arguments.

This expanded JIT demonstrates the complex interplay between runtime profiling, dynamic code generation, and low-level operating system and hardware considerations. It significantly enhances the JVM's capability beyond pure interpretation by optimizing methods that involve argument passing and simple arithmetic.
