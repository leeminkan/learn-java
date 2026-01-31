// main.cpp
#include "ClassReader.h"
#include "Interpreter.h"
#include <iostream>

int main(int argc, char *argv[])
{
    if (argc < 2)
    {
        std::cerr << "Usage: " << argv[0] << " <class_file>" << std::endl;
        return 1;
    }

    // 1. Parse the Class
    ClassReader reader(argv[1]);
    reader.parse();

    // 2. Find the 'main' method
    MethodInfo main_method;
    bool found = false;
    for (const auto &method : reader.get_methods())
    {
        if (method.name == "main" && method.descriptor == "([Ljava/lang/String;)V")
        {
            main_method = method;
            found = true;
            break;
        }
    }

    if (!found)
    {
        std::cerr << "Error: Main method not found." << std::endl;
        return 1;
    }
    // 3. Initialize Interpreter with the Constant Pool AND ALL METHODS
    // Pass the methods by reference to the interpreter
    std::vector<MethodInfo> &all_methods_ref = reader.get_methods();
    Interpreter interpreter(reader.get_constant_pool(), all_methods_ref);

    // Find the 'main' method within the *referenced* vector to ensure JIT changes persist
    MethodInfo *main_method_ptr = nullptr;
    for (auto &method : all_methods_ref)
    {
        if (method.name == "main" && method.descriptor == "([Ljava/lang/String;)V")
        {
            main_method_ptr = &method;
            break;
        }
    }

    if (!main_method_ptr)
    {
        std::cerr << "Error: Main method not found." << std::endl;
        return 1;
    }
    interpreter.run(*main_method_ptr);

    return 0;
}