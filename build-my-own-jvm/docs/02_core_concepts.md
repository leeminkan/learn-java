# 2. Core JVM Concepts Implemented

This JVM implements several fundamental concepts of the official Java Virtual Machine Specification, albeit in a simplified form.

### Stack-Based Architecture

The JVM is a **stack-based machine**. Instead of using CPU registers for calculations, it uses an **Operand Stack**.

- **Operand Stack**: A LIFO stack where bytecode instructions push and pop values. For example, the `iadd` instruction pops two integers, adds them, and pushes the result back onto the stack.
- **Local Variable Array**: Each method frame has an array to hold local variables and method arguments. Instructions like `iload_1` (push the integer from local variable 1) and `istore_1` (pop an integer and save it to local variable 1) interact with this array.

### Class File Parsing

The `ClassReader` is responsible for parsing the `HelloWorld.class` file, which is a binary file containing compiled Java bytecode and metadata. The reader extracts crucial information, including:

- **The Constant Pool**: A table of constants, strings, and references used by the program.
- **Method Information**: Details about each method, including its name, descriptor, and its actual bytecode.

### The Constant Pool

The Constant Pool is the heart of the `.class` file. It acts as a symbol table, holding nearly all literal values and symbolic references. This includes:

- String literals (e.g., "Hello, World!").
- Names of classes, fields, and methods.
- Type descriptors that define method signatures.
  Our `ConstantPool.h` defines C++ structs to represent the various types of constant pool entries.

### Bytecode Execution

The `Interpreter` contains the main execution loop. It iterates through the bytecode of a method, using a large `switch` statement to execute one instruction at a time. The program counter (`pc`) keeps track of the current instruction.

### Control Flow and Branching

A significant milestone for the JVM is becoming Turing-complete, which is achieved by implementing control flow. In Java, this looks like `for` loops, `while` loops, or `if` statements. In bytecode, it's all handled by **branching** (or **jump**) instructions.

- **Conditional Jumps**: Opcodes like `if_icmpge` ("if integer comparison is greater than or equal") pop two integers from the operand stack and compare them. If the condition is true, the interpreter adds a signed 16-bit _offset_ (read from the next two bytes of code) to the program counter (`pc`), causing execution to "jump" to a new location. If false, it proceeds to the next instruction.
- **Unconditional Jumps**: The `goto` opcode always adds the offset to the `pc`, forcing a jump. This is essential for creating loops, as it allows the JVM to jump from the end of a loop body back to its beginning.

### Stack Frames

Every time a method is called, a new **stack frame** is created. A frame contains its own operand stack and local variable array. In our implementation, we simulate this by making a recursive call to the `Interpreter::run` method. When a method returns, its frame is destroyed, and control returns to the calling method—a process perfectly modeled by a function returning from a recursive call.

### The Heap

A very simple heap is implemented as a `std::vector` of `JavaObject`s. When the `new` or `newarray` opcodes are executed, a new object/array is allocated and added to this vector. The index of the item in the vector acts as its "reference" or "address," which is then pushed onto the operand stack.

The `JavaObject` struct was updated to be a versatile container that can represent either a standard object or an array:

```cpp
struct JavaObject {
    // For standard objects
    std::string class_name;
    std::unordered_map<std::string, int> fields;

    // For arrays
    std::vector<int> array_data;
};
```

When an array is created, its data is stored in `array_data`. When a standard object is created, its instance variables are stored in the `fields` map.

### Just-In-Time (JIT) Compilation

To improve performance beyond pure interpretation, modern JVMs employ Just-In-Time (JIT) compilers. Our JIT takes frequently executed ("hot") methods and translates their bytecode into native machine code (ARM64 in our case) during runtime. This process currently optimizes simple methods that return constant integers or perform basic arithmetic using arguments.

- **Profiling**: The JVM tracks the `call_count` for each method.

- **Compilation Trigger**: When a method's `call_count` reaches a certain threshold, the `JitCompiler` attempts to compile it.

- **Native Execution**: If successful, subsequent calls to that method directly execute the much faster native code, bypassing the interpreter.
