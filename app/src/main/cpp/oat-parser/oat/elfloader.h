#pragma once

extern "C" bool ElfInit(char* elf, unsigned int len);

extern "C" bool GetOatInfo(unsigned int& offset, unsigned int& size);

extern "C" bool ElfUnInit();