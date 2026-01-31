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

### Stack Frames
Every time a method is called, a new **stack frame** is created. A frame contains its own operand stack and local variable array. In our implementation, we simulate this by making a recursive call to the `Interpreter::run` method. When a method returns, its frame is destroyed, and control returns to the calling method—a process perfectly modeled by a function returning from a recursive call.

### The Heap
A very simple heap is implemented as a `std::vector` of `JavaObject`s. When the `new` opcode is executed, a new object is allocated and added to this vector. The index of the object in the vector acts as its "reference" or "address," which is then pushed onto the operand stack.
