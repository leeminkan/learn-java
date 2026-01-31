# Build Your Own JVM

This project is a simple, educational implementation of a Java Virtual Machine (JVM) written in C++. Its primary goal is to demonstrate the fundamental concepts of bytecode interpretation, class file parsing, and basic JVM execution.

## Project Structure

The project is organized into the following directories:

-   `src/`: Contains all C++ source code (`.cpp`) and header files (`.h`) for the JVM implementation.
-   `tests/`: Contains Java source files (`.java`) used for testing the JVM.
-   `build/`: Contains compiled Java `.class` files and the `myjvm` executable after compilation.

## How to Build

To build the `myjvm` executable, navigate to the project root and use a C++ compiler (like g++):

```bash
clang++ -std=c++17 src/*.cpp -o build/myjvm
```

This command compiles all `.cpp` files in the `src/` directory and links them into an executable named `myjvm` inside the `build/` directory.

## How to Run

First, ensure you have compiled the `myjvm` executable as described above.

Then, compile your Java test files from the `tests/` directory into `.class` files and place them in the `build/` directory. For example, to compile `HelloWorld.java`:

```bash
javac tests/HelloWorld.java -d build/
```

Once the `.class` files are in the `build/` directory, you can run them using `myjvm`:

```bash
build/myjvm build/HelloWorld.class
```

Note that you must provide the full class filename, including the `.class` extension.

## Current Capabilities

This JVM implementation is a work in progress and supports a growing set of features:

-   **Class File Parsing**: Basic parsing of `.class` files, including the constant pool and method information.
-   **Bytecode Interpretation**: Execution of an expanding subset of JVM opcodes.
    -   **Integer Arithmetic & Constants**: `iadd`, `iconst_*`, `bipush`.
    -   **Control Flow**: Branching opcodes like `goto` and `if_icmpge` that enable loops and conditionals, making the JVM Turing-complete.
    -   **Local Variables**: `iload`, `istore`, `iinc`.
    -   **Object & Array Support**:
        -   Basic object creation for a hardcoded `Point` class.
        -   Integer array creation (`newarray`), length checking (`arraylength`), and element access (`iaload`, `iastore`).
    -   **Method Invocation**: Static method calls (`invokestatic`) and dynamic dispatch for `System.out.println` (`invokevirtual`).
    -   **Just-In-Time (JIT) Compilation**: A basic ARM64 JIT compiler that optimizes "hot" methods returning constant integers into native machine code for faster execution.

## Limitations

-   Many operations still use hardcoded values instead of fully dynamic lookups in the constant pool (e.g., for object fields).
-   Opcode support is still incomplete (no floating-point numbers, exceptions, etc.).
-   Array support is limited to integer arrays.
-   The class parser ignores many attributes and structures.
-   The JIT compiler is a "leaf JIT", only optimizing very simple methods (constant return values) and demonstrating the pipeline, not a full-featured JIT.

This project is ideal for learning about JVM internals and can be extended to support more Java features.
