// ClassReader.cpp
#include "ClassReader.h"
#include <iostream>
#include <arpa/inet.h>

ClassReader::ClassReader(const std::string &filename)
{
    file.open(filename, std::ios::binary);
    if (!file.is_open())
    {
        std::cerr << "Error: Could not open " << filename << std::endl;
        exit(1);
    }
    // JVM Constant Pool is 1-indexed
    constant_pool.push_back(nullptr);
}

ClassReader::~ClassReader()
{
    if (file.is_open())
        file.close();
}

uint32_t ClassReader::read_u4()
{
    uint32_t value;
    file.read(reinterpret_cast<char *>(&value), sizeof(value));
    return ntohl(value);
}

uint16_t ClassReader::read_u2()
{
    uint16_t value;
    file.read(reinterpret_cast<char *>(&value), sizeof(value));
    return ntohs(value);
}

uint8_t ClassReader::read_u1()
{
    uint8_t value;
    file.read(reinterpret_cast<char *>(&value), sizeof(value));
    return value;
}

void ClassReader::parse()
{
    // 1. Magic & Versions
    uint32_t magic = read_u4();
    if (magic != 0xCAFEBABE)
    {
        std::cerr << "Invalid Magic Number!" << std::endl;
        exit(1);
    }
    read_u2(); // minor
    read_u2(); // major

    // 2. Constant Pool
    uint16_t cp_count = read_u2();
    for (int i = 1; i < cp_count; ++i)
    {
        uint8_t tag = read_u1();
        switch (tag)
        {
        case CONSTANT_Fieldref:
            read_u2();
            read_u2();
            constant_pool.push_back(std::make_shared<CpInfo>());
            break;
        case CONSTANT_Methodref:
        {
            auto m = std::make_shared<CpMethodRef>();
            m->tag = tag;
            m->class_index = read_u2();
            m->name_and_type_index = read_u2();
            constant_pool.push_back(m);
            break;
        }
        case CONSTANT_NameAndType:
        {
            auto nt = std::make_shared<CpNameAndType>();
            nt->tag = tag;
            nt->name_index = read_u2();
            nt->descriptor_index = read_u2();
            constant_pool.push_back(nt);
            break;
        }
        case CONSTANT_Class:
            read_u2();
            constant_pool.push_back(std::make_shared<CpInfo>());
            break;
        case CONSTANT_String:
        {
            auto str_const = std::make_shared<CpString>();
            str_const->tag = tag;
            str_const->string_index = read_u2();
            constant_pool.push_back(str_const);
            break;
        }
        case CONSTANT_Utf8:
        {
            uint16_t length = read_u2();
            std::string text(length, ' ');
            file.read(&text[0], length);
            auto utf8_const = std::make_shared<CpUtf8>();
            utf8_const->tag = tag;
            utf8_const->length = length;
            utf8_const->bytes = text;
            constant_pool.push_back(utf8_const);
            break;
        }
        default:
            // Handle unmapped tags to prevent crashing
            constant_pool.push_back(std::make_shared<CpInfo>());
            break;
        }
    }

    // 3. Class Info
    read_u2(); // access_flags
    read_u2(); // this_class
    read_u2(); // super_class

    // 4. Interfaces
    uint16_t interfaces_count = read_u2();
    for (int i = 0; i < interfaces_count; ++i)
        read_u2();

    // 5. Fields
    uint16_t fields_count = read_u2();
    for (int i = 0; i < fields_count; ++i)
    {
        read_u2();
        read_u2();
        read_u2();
        uint16_t attr_count = read_u2();
        for (int a = 0; a < attr_count; ++a)
        {
            read_u2();
            uint32_t attr_len = read_u4();
            file.seekg(attr_len, std::ios::cur);
        }
    }

    // 6. Methods (This is where we extract the bytecode!)
    uint16_t methods_count = read_u2();
    for (int i = 0; i < methods_count; ++i)
    {
        MethodInfo method;
        read_u2(); // access_flags
        uint16_t name_idx = read_u2();
        uint16_t desc_idx = read_u2();

        auto name_const = std::dynamic_pointer_cast<CpUtf8>(constant_pool[name_idx]);
        auto desc_const = std::dynamic_pointer_cast<CpUtf8>(constant_pool[desc_idx]);

        method.name = name_const ? name_const->bytes : "unknown";
        method.descriptor = desc_const ? desc_const->bytes : "unknown";

        uint16_t attr_count = read_u2();
        for (int a = 0; a < attr_count; ++a)
        {
            uint16_t attr_name_idx = read_u2();
            uint32_t attr_len = read_u4();

            auto attr_name_const = std::dynamic_pointer_cast<CpUtf8>(constant_pool[attr_name_idx]);

            // Check if this attribute is the "Code" attribute
            if (attr_name_const && attr_name_const->bytes == "Code")
            {
                method.max_stack = read_u2();
                method.max_locals = read_u2();

                uint32_t code_length = read_u4();
                method.bytecode.resize(code_length);
                file.read(reinterpret_cast<char *>(method.bytecode.data()), code_length);

                // Skip exception table and code attributes
                uint16_t exception_table_len = read_u2();
                file.seekg(exception_table_len * 8, std::ios::cur);

                uint16_t code_attrs_count = read_u2();
                for (int ca = 0; ca < code_attrs_count; ++ca)
                {
                    read_u2();
                    uint32_t ca_len = read_u4();
                    file.seekg(ca_len, std::ios::cur);
                }
            }
            else
            {
                // Not the "Code" attribute, skip it
                file.seekg(attr_len, std::ios::cur);
            }
        }
        methods.push_back(method);
    }

    // 7. Class Attributes
    uint16_t class_attr_count = read_u2();
    for (int a = 0; a < class_attr_count; ++a)
    {
        read_u2();
        uint32_t attr_len = read_u4();
        file.seekg(attr_len, std::ios::cur);
    }
}