Now, let's make your JVM Turing-complete by implementing **Control Flow (Loops and Conditionals)**.

### The Theory: How JVM Loops Work

In Java source code, you see curly braces `{}` and keywords like `for` or `while`. In Bytecode, there are no loops. There are only **Jumps** (Branching).

To run a loop, the JVM uses:
1.  **Comparison Instructions (e.g., `if_icmpgt`):** "If integer A is greater than B, jump to offset X."
2.  **GoTo Instructions (`goto`):** "Jump unconditionally back to offset Y."

---

### Step 1: The Loop Test Case

Create `tests/LoopTest.java`. This program calculates the sum of numbers from 1 to 5 (which is 15).

```java
public class LoopTest {
    public static void main(String[] args) {
        int sum = 0;
        for (int i = 1; i <= 5; i++) {
            sum = sum + i;
        }
        System.out.println(sum);
    }
}
```

Compile it into your `build` directory:
```bash
javac -d build tests/LoopTest.java
```

---

### Step 2: Implement Prerequisite `iconst` Opcodes

A critical and easy-to-miss detail is that simple code like `int sum = 0;` and `int i = 1;` doesn't use the generic `bipush` instruction. For efficiency, the JVM has special single-byte opcodes for pushing small, common integers. The `LoopTest` will fail with a segmentation fault if these are not implemented, because the interpreter will expect a value on the stack that was never pushed.

Add the following definitions to `src/Interpreter.h`:
```cpp
#define OP_ACONST_NULL 0x01
#define OP_ICONST_M1 0x02
#define OP_ICONST_0 0x03
#define OP_ICONST_1 0x04
#define OP_ICONST_2 0x05
#define OP_ICONST_3 0x06
#define OP_ICONST_4 0x07
```

Now, add the corresponding `case` blocks inside your `run` loop's `switch` statement. A good place is right before `OP_ICONST_5`.
```cpp
// Inside src/Interpreter.h switch statement
            case OP_ACONST_NULL: operand_stack.push(0); pc += 1; break;
            case OP_ICONST_M1: operand_stack.push(-1); pc += 1; break;
            case OP_ICONST_0: operand_stack.push(0); pc += 1; break;
            case OP_ICONST_1: operand_stack.push(1); pc += 1; break;
            case OP_ICONST_2: operand_stack.push(2); pc += 1; break;
            case OP_ICONST_3: operand_stack.push(3); pc += 1; break;
            case OP_ICONST_4: operand_stack.push(4); pc += 1; break;
```

---

### Step 3: Update `Interpreter.h` with Branching Logic

Now for the main logic. Branching instructions use a **signed 16-bit offset** to calculate their jump target. If the current Program Counter (`pc`) is `10` and the instruction is `goto -5`, the next instruction to execute is at index `5`.

Add these definitions to `src/Interpreter.h`:
```cpp
#define OP_IF_ICMPLE 0xa4  // Branch if int comparison is <=
#define OP_IF_ICMPGT 0xa3  // Branch if int comparison is >
#define OP_GOTO      0xa7  // Jump unconditionally
#define OP_IINC      0x84  // Increment local variable
```

Add the logic inside your `run` loop. Branching instructions are unique because they modify the `pc` directly, rather than just incrementing it.

```cpp
// Inside src/Interpreter.h switch statement

            case OP_IF_ICMPGT: {
                // Format: if_icmpgt branchbyte1 branchbyte2
                // Pops val2, val1. If val1 > val2, jump.
                int16_t offset = (code[pc + 1] << 8) | code[pc + 2];
                int val2 = operand_stack.top(); operand_stack.pop();
                int val1 = operand_stack.top(); operand_stack.pop();

                if (val1 > val2) {
                    std::cout << "Instruction: if_icmpgt (Branch taken -> offset " << offset << ")" << std::endl;
                    pc += offset; // JUMP!
                } else {
                    std::cout << "Instruction: if_icmpgt (Branch not taken)" << std::endl;
                    pc += 3; // Continue to next instruction
                }
                break;
            }

            case OP_GOTO: {
                // Format: goto branchbyte1 branchbyte2
                int16_t offset = (code[pc + 1] << 8) | code[pc + 2];
                std::cout << "Instruction: goto (Loop back -> offset " << offset << ")" << std::endl;
                pc += offset; // JUMP!
                break;
            }

            case OP_IINC: {
                // Format: iinc index const
                // Increments local variable at 'index' by 'const'
                uint8_t index = code[pc + 1];
                int8_t constant = code[pc + 2]; // signed byte

                local_variables[index] += constant;
                std::cout << "Instruction: iinc (Var " << (int)index << " += " << (int)constant << ")" << std::endl;
                pc += 3;
                break;
            }

            // NOTE: Your java compiler version might use 'if_icmple'
            case OP_IF_ICMPLE: {
                 int16_t offset = (code[pc + 1] << 8) | code[pc + 2];
                 int val2 = operand_stack.top(); operand_stack.pop();
                 int val1 = operand_stack.top(); operand_stack.pop();

                 if (val1 <= val2) {
                     std::cout << "Instruction: if_icmple (Branch taken -> offset " << offset << ")" << std::endl;
                     pc += offset;
                 } else {
                     std::cout << "Instruction: if_icmple (Branch not taken)" << std::endl;
                     pc += 3;
                 }
                 break;
            }
```

---

### Step 4: Compile and Run

The previous compile command was incomplete. Use this one to ensure all `.cpp` files in the `src` directory are included.

Compile from your project root:
```bash
clang++ -std=c++17 src/*.cpp -o build/myjvm
```

Run the test. Remember that our `main.cpp` requires the full filename.
```bash
build/myjvm build/LoopTest.class
```

### Expected Output

You should see the JVM looping! The exact output may vary, but the key is to see the `goto` and `if_icmpgt` instructions working together, followed by the correct final result.

```text
--- ENTERING FRAME: main ---
...
Instruction: if_icmpgt (Branch not taken)
Instruction: iadd
Instruction: iinc (Var 2 += 1)
Instruction: goto (Loop back -> offset -12)
... (repeats 5 times) ...
Instruction: if_icmpgt (Branch taken -> offset 13)
Instruction: getstatic #7 (System.out)
Instruction: invokevirtual #13 (Method: println, Descriptor: (I)V)
>> JVM OUTPUT: 15
Instruction: return
```

If you see this, congratulations! Your JVM is now Turing-complete. It can theoretically compute anything that is computable.
