#pragma once

#include "art/compiler_filter.h"
#include <string>

// 初始化， oat_file为原始的oat文件名 out_dex_path为输出的dex路径
extern "C" bool InitOatParser(const char* oat_file, const char* out_dex_path);

// 将Ota文件dump成dex文件
extern "C" bool DoDumpToDex();

extern "C" void SetToGenerateSignature(bool is_signature_generated);

extern "C" void SetToGenerateSecurestore(bool is_securestore_generated);

extern "C" void SetToDoIV(bool is_do_IV);

extern "C" bool GenerateSignature(const char *oat_file, std::string *signature, Art::CompilerFilter::Filter *filter, int *dex_count);

extern "C" void ParseSecureStore(const char *securestore_file, int dex_count);

extern "C" void DoIV(const std::string signature, Art::CompilerFilter::Filter filter);

extern "C" bool ParseOatFile(const std::string read_file);

extern "C" bool TamperChecksum(const std::string tamper_file);


