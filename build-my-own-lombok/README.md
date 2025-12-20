# Build My Own Lombok: A Deep Dive into Java Annotation Processing

This project is a educational "Tiny Lombok" clone. It demonstrates how Java tools can automate boilerplate code by hooking into the **Java Compiler (javac)** using the **JSR 269 Pluggable Annotation Processing API**.

## 🧠 The "Node.js" Mental Model

If you are coming from a Node.js background, think of this project as a **Babel Plugin** or a **TypeScript Transformer**:

* It doesn't run your code; it **transpiles** your code during the build step.
* It reads your source code as an **Abstract Syntax Tree (AST)**.
* It generates new code based on the metadata (Annotations) it finds.

## 🛠 Project Structure

To avoid "Chicken and Egg" compilation errors, the project is split into a multi-module Maven build:

1. **`lombok-engine`**: The "DevDependency". Contains the `@MyBuilder` annotation and the `MyBuilderProcessor` logic.
2. **`example-app`**: The "Consumer". Uses the annotation to trigger code generation.

---

## 🚀 How It Works (Under the Hood)

### 1. The Hook

We register our processor in `META-INF/services/javax.annotation.processing.Processor`. When `javac` runs, it scans this file and "wakes up" our engine.

### 2. The Subscription

We use `@SupportedAnnotationTypes` to tell the compiler: *"Only call me if you see `@MyBuilder` in a file."*

### 3. The AST Introspection

Instead of using Regex, we use the `javax.lang.model` API to traverse the code structure:

* **`TypeElement`**: Represents the Class.
* **`VariableElement`**: Represents the Fields.
* **`TypeMirror`**: Represents the data types (enabling type-safe builders).

### 4. Code Generation

We use the `Filer` API to write new `.java` files. These are written to `target/generated-sources/annotations/`, where the compiler automatically picks them up for the final build.

---

## ✨ Features Implemented

* **Custom Annotation**: Created `@MyBuilder` to mark target classes.
* **Fluent API**: Generates a Builder pattern with chainable methods (e.g., `.name("Kan").id(1)`).
* **Type Awareness**: Automatically detects field types (`int`, `String`, `boolean`, etc.) and applies them to the builder methods.
* **Compile-time Validation**:
* Fails the build if `@MyBuilder` is used on an `interface`.
* Fails the build if used on an `abstract class`.
* Underlines the error directly in the IDE.



---

## 🛠 Setup & Usage

### Maven Build

Run the following command from the root directory:

```bash
mvn clean install

```

### IntelliJ Integration

1. **Enable Annotation Processing**: `Settings > Build > Compiler > Annotation Processors > Enable annotation processing`.
2. **Mark Generated Sources**: Right-click `target/generated-sources/annotations` and select `Mark Directory as > Generated Sources Root`.

---

## 🔬 Lombok vs. This Project

| Feature | This Project | Real Project Lombok |
| --- | --- | --- |
| **Method** | **Source Generation** | **AST Modification** |
| **Result** | Creates a *new* `.java` file. | Modifies the *existing* `.class` file. |
| **Complexity** | Uses standard, stable APIs. | Uses internal, "hacky" compiler APIs. |
| **Visibility** | You can see the generated source. | Code is only visible in bytecode (or via "Delombok"). |

---

## 🎓 Lessons Learned

* **Metadata over Boilerplate**: Annotations allow us to write "code that writes code."
* **Fail Fast**: Using the `Messager` API to throw compiler errors is better than runtime exceptions.
* **Tooling Synergy**: The relationship between the Compiler, Maven, and the IDE is what makes the Java developer experience powerful.

