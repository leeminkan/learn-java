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
build/myjvm build/HelloWorld
```

Note that you should provide the class name without the `.class` extension.

## Current Capabilities

This JVM implementation is a work in progress and supports a limited set of features:

-   **Class File Parsing**: Basic parsing of `.class` files, including the constant pool and method information.
-   **Bytecode Interpretation**: Execution of a small subset of JVM opcodes, including:
    -   Integer arithmetic (`iadd`).
    -   Local variable manipulation (`istore`, `iload`).
    -   Static method calls (`invokestatic`).
    -   Hardcoded support for `System.out.println`.
    -   Basic object creation and field access (currently hardcoded for a `Point` class).

## Limitations

-   Many operations use hardcoded values instead of dynamic lookups in the constant pool.
-   Limited opcode support (no control flow, arrays, exceptions, floating-point numbers, etc.).
-   Incomplete parsing of `.class` file structures (e.g., fields are mostly ignored).

This project is ideal for learning about JVM internals and can be extended to support more Java features.
