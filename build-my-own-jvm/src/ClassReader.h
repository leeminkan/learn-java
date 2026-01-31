// ClassReader.h
#pragma once
#include "ConstantPool.h"
#include <fstream>
#include <vector>

class ClassReader
{
private:
    std::ifstream file;
    std::vector<std::shared_ptr<CpInfo>> constant_pool;
    std::vector<MethodInfo> methods;

    uint32_t read_u4();
    uint16_t read_u2();
    uint8_t read_u1();

public:
    ClassReader(const std::string &filename);
    ~ClassReader();
    void parse();
    std::vector<MethodInfo> get_methods() { return methods; }
    std::vector<std::shared_ptr<CpInfo>> get_constant_pool() { return constant_pool; }
};