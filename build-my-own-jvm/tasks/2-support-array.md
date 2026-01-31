Implementing arrays is a critical step. In Java, arrays are Objects, but they behave differently: they are indexed by integers rather than field names.

To implement this, we need to modify our **Heap** representation to support array storage and add several new opcodes.

---

### Step 1: The Array Test Case

Create `tests/ArrayTest.java`. This program creates an array of size 3, fills it with numbers, and uses a loop to sum them up.

```java
public class ArrayTest {
    public static void main(String[] args) {
        // 1. Create array
        int[] nums = new int[3];

        // 2. Store values (IASTORE)
        nums[0] = 10;
        nums[1] = 20;
        nums[2] = 30;

        int sum = 0;

        // 3. Loop and read values (ARRAYLENGTH, IALOAD)
        for (int i = 0; i < nums.length; i++) {
            sum = sum + nums[i];
        }

        System.out.println(sum); // Expect: 60
    }
}
```

Compile it:
```bash
javac -d build tests/ArrayTest.java
```

---

### Step 2: Update `Interpreter.h` (Data Structures)

We need to update our `JavaObject` struct. Currently, it only holds a `map` for fields. We will add a vector to hold array data. For simplicity, we will make `JavaObject` capable of being _either_ a normal object _or_ an array.

Update the `JavaObject` struct and add the new Opcode definitions in `src/Interpreter.h`:

```cpp
// --- ADD NEW OPCODES ---
#define OP_NEWARRAY     0xbc  // Create primitive array
#define OP_ARRAYLENGTH  0xbe  // Get length of array
#define OP_IALOAD       0x2e  // Load int from array
#define OP_IASTORE      0x4f  // Store int into array

// --- UPDATE JAVAOBJECT STRUCT ---
struct JavaObject {
    // Standard Object stuff
    std::string class_name;
    std::unordered_map<std::string, int> fields;

    // Array stuff
    // We simply use a vector to store array elements if this object is an array
    std::vector<int> array_data;
};
```

---

### Step 3: Implement the Loop Condition Opcode

Running the test now would cause an `ArrayIndexOutOfBoundsException`. Why? The loop `for (int i = 0; i < nums.length; i++)` is translated by the Java compiler into bytecode that exits the loop when `i >= nums.length`. This requires the `if_icmpge` ("if integer comparison is greater than or equal") opcode, which we have not implemented yet.

Add the definition to `src/Interpreter.h`:
```cpp
#define OP_IF_ICMPGE 0xa2  // Branch if int comparison is >=
```

And add its implementation to the `switch` statement:
```cpp
            case OP_IF_ICMPGE: {
                // Format: if_icmpge branchbyte1 branchbyte2
                // Pops val2, val1. If val1 >= val2, jump.
                int16_t offset = (code[pc + 1] << 8) | code[pc + 2];
                int val2 = operand_stack.top(); operand_stack.pop();
                int val1 = operand_stack.top(); operand_stack.pop();

                if (val1 >= val2) {
                    std::cout << "Instruction: if_icmpge (Branch taken -> offset " << offset << ")" << std::endl;
                    pc += offset;
                } else {
                    std::cout << "Instruction: if_icmpge (Branch not taken)" << std::endl;
                    pc += 3;
                }
                break;
            }
```

---

### Step 4: Implement Array Opcodes

Now, add the core array-handling cases to your interpreter's switch statement in `src/Interpreter.h`.

```cpp
            case OP_NEWARRAY: {
                // Format: newarray atype
                uint8_t atype = code[pc + 1]; // 10 = int, 4 = boolean, etc.

                int count = operand_stack.top(); operand_stack.pop();

                // Create the array object
                auto obj = std::make_shared<JavaObject>();
                obj->class_name = "[I"; // Internal name for int[]
                obj->array_data.resize(count, 0); // Initialize with zeros

                heap.push_back(obj);
                int obj_ref = heap.size() - 1;
                operand_stack.push(obj_ref);

                std::cout << "Instruction: newarray (Created int[" << count << "] at Heap " << obj_ref << ")" << std::endl;
                pc += 2;
                break;
            }

            case OP_ARRAYLENGTH: {
                int obj_ref = operand_stack.top(); operand_stack.pop();
                int length = heap[obj_ref]->array_data.size();

                std::cout << "Instruction: arraylength (Len: " << length << ")" << std::endl;
                operand_stack.push(length);
                pc += 1;
                break;
            }

            case OP_IASTORE: {
                // Stack: arrayref, index, value -> (Empty)
                int value = operand_stack.top(); operand_stack.pop();
                int index = operand_stack.top(); operand_stack.pop();
                int obj_ref = operand_stack.top(); operand_stack.pop();

                // Safety check
                if (index < 0 || index >= heap[obj_ref]->array_data.size()) {
                    std::cerr << "Error: ArrayIndexOutOfBoundsException: " << index << std::endl;
                    return 0; // Simulate JVM exit on error
                }

                heap[obj_ref]->array_data[index] = value;
                std::cout << "Instruction: iastore (arr[" << index << "] = " << value << ")" << std::endl;
                pc += 1;
                break;
            }

            case OP_IALOAD: {
                // Stack: arrayref, index -> value
                int index = operand_stack.top(); operand_stack.pop();
                int obj_ref = operand_stack.top(); operand_stack.pop();

                // Safety check
                if (index < 0 || index >= heap[obj_ref]->array_data.size()) {
                    std::cerr << "Error: ArrayIndexOutOfBoundsException: " << index << std::endl;
                    return 0; // Simulate JVM exit on error
                }

                int value = heap[obj_ref]->array_data[index];

                std::cout << "Instruction: iaload (Read " << value << " from index " << index << ")" << std::endl;
                operand_stack.push(value);
                pc += 1;
                break;
            }
```

---

### Step 5: Compile and Run

Compile the updated JVM using the correct command that includes all source files:

```bash
clang++ -std=c++17 src/*.cpp -o build/myjvm
```

Run the array test (remembering to include the `.class` extension):
```bash
build/myjvm build/ArrayTest.class
```

### Expected Output

You should see the JVM managing memory, storing values, looping, and finally summing the result.

```text
--- ENTERING FRAME: main ---
Instruction: newarray (Created int[3] at Heap 0)
Instruction: iastore (arr[0] = 10)
Instruction: iastore (arr[1] = 20)
Instruction: iastore (arr[2] = 30)
... (looping logic) ...
Instruction: if_icmpge (Branch not taken)
Instruction: iaload (Read 10 from index 0)
... (looping continues) ...
Instruction: if_icmpge (Branch taken -> offset 15)
>> JVM OUTPUT: 60
```
