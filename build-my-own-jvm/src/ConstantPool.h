// ConstantPool.h
#pragma once
#include <vector>
#include <string>
#include <memory>

enum ConstantTag
{
    CONSTANT_Utf8 = 1,
    CONSTANT_Class = 7,
    CONSTANT_String = 8,
    CONSTANT_Fieldref = 9,
    CONSTANT_Methodref = 10,
    CONSTANT_NameAndType = 12
};

struct CpInfo
{
    uint8_t tag;
    virtual ~CpInfo() = default;
};

struct CpUtf8 : CpInfo
{
    uint16_t length;
    std::string bytes;
};

struct CpMethodRef : CpInfo
{
    uint16_t class_index;
    uint16_t name_and_type_index;
};

struct CpString : CpInfo
{
    uint16_t string_index;
};

// Represents a parsed Java Method
struct MethodInfo
{
    std::string name;
    std::string descriptor;
    std::vector<uint8_t> bytecode; // The raw instructions
    uint16_t max_stack;
    uint16_t max_locals;

    // --- NEW JIT FIELDS ---
    int call_count = 0;           // Profiler counter
    void *jit_code_ptr = nullptr; // Pointer to native ARM64 code
};

// Add this struct
struct CpNameAndType : CpInfo
{
    uint16_t name_index;
    uint16_t descriptor_index;
};
