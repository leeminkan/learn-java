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

    // 3. Execute!
    Interpreter interpreter(reader.get_constant_pool());
    interpreter.run(main_method);

    return 0;
}