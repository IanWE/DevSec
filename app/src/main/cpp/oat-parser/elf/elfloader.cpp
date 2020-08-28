// elfloader.cpp : Defines the entry point for the console application.
//

#include<iomanip>
#include "elfcpp.h"
#include "elfcpp_file.h"
#include "map_file.h"

using namespace std;

//typedef elfcpp::Elf_file<32, false, File> ElfFile;
static File::Location s_oat(0, 0);

extern "C" /*__declspec(dllexport)*/ bool ElfInit(char *elf, unsigned int len) {
    bool ret = false;

    //Read ELF header: size, big_endian
    int want = elfcpp::Elf_recognizer::max_header_size;
    if (want > len) {
        want = len;
    }
    if (!elfcpp::Elf_recognizer::is_elf_file((const unsigned char *)elf, want)) {
        printf("This is not an ELF file\n");
        return false;
    }
    int size;
    bool big_endian;
    std::string error;
    if (!elfcpp::Elf_recognizer::is_valid_header((const unsigned char *)elf, want, &size, &big_endian, &error)) {
        printf("Cannot analyze ELF header: %s\n", error.c_str());
        return false;
    }
    printf("ELF size: %d, big_endian: %s\n", size, big_endian?"big_endian":"little_endian");

    File file(reinterpret_cast<unsigned char *>(elf), len);
 
    //reset s_oat
    s_oat.file_offset = 0;
    s_oat.data_size = 0;

    if (size == 32 && big_endian) {
    	typedef elfcpp::Elf_file<32, true, File> ElfFile;
        ElfFile::Ef_ehdr hdr(file.view(elfcpp::file_header_offset, ElfFile::ehdr_size).data());
        ElfFile efile(&file, hdr);

	//找到rodata段，将offset和size找到，offset将是ota文件的头
    	for (unsigned i = 0; i < efile.shnum(); ++i) {
        if (efile.section_name(i).compare(".rodata") == 0) {
            s_oat.file_offset = efile.section_contents(i).file_offset;
            s_oat.data_size = efile.section_contents(i).data_size;
            ret = true;
            break;
        }
        }

    }	
    else if (size == 64 && big_endian) {
    	typedef elfcpp::Elf_file<64, true, File> ElfFile;
 	    ElfFile::Ef_ehdr hdr(file.view(elfcpp::file_header_offset, ElfFile::ehdr_size).data());
        ElfFile efile(&file, hdr);
	
	//找到rodata段，将offset和size找到，offset将是ota文件的头
    	for (unsigned i = 0; i < efile.shnum(); ++i) {
        if (efile.section_name(i).compare(".rodata") == 0) {
            s_oat.file_offset = efile.section_contents(i).file_offset;
            s_oat.data_size = efile.section_contents(i).data_size;
            ret = true;
            break;
        }
        }

    }
    else if (size == 32 && !big_endian) {
    	typedef elfcpp::Elf_file<32, false, File> ElfFile;
	ElfFile::Ef_ehdr hdr(file.view(elfcpp::file_header_offset, ElfFile::ehdr_size).data());
        ElfFile efile(&file, hdr);
	
	//找到rodata段，将offset和size找到，offset将是ota文件的头
    	for (unsigned i = 0; i < efile.shnum(); ++i) {
        if (efile.section_name(i).compare(".rodata") == 0) {
            s_oat.file_offset = efile.section_contents(i).file_offset;
            s_oat.data_size = efile.section_contents(i).data_size;

            ret = true;
            break;
        }
        }

    }
    else if (size == 64 && !big_endian) {
    	typedef elfcpp::Elf_file<64, false, File> ElfFile;
	ElfFile::Ef_ehdr hdr(file.view(elfcpp::file_header_offset, ElfFile::ehdr_size).data());
        ElfFile efile(&file, hdr);
	
	//找到rodata段，将offset和size找到，offset将是ota文件的头
    	for (unsigned i = 0; i < efile.shnum(); ++i) {
        if (efile.section_name(i).compare(".rodata") == 0) {
            s_oat.file_offset = efile.section_contents(i).file_offset;
            s_oat.data_size = efile.section_contents(i).data_size;
	        printf("file offset %d and data size %d\n",efile.section_contents(i).file_offset, efile.section_contents(i).data_size);
            ret = true;
            break;
        }
        }

    }
    else
	return false;

/*
    //找到rodata段，将offset和size找到，offset将是ota文件的头
    for (unsigned i = 0; i < efile.shnum(); ++i) {
        if (efile.section_name(i).compare(".rodata") == 0) {
            s_oat.file_offset = efile.section_contents(i).file_offset;
            s_oat.data_size = efile.section_contents(i).data_size;

            ret = true;
            break;
        }
    }
*/

    return ret;
}

extern "C" bool GetOatInfo(unsigned int &offset, unsigned int &size) {
    if (s_oat.file_offset == 0 || s_oat.data_size == 0) {
        return false;
    }
    offset = s_oat.file_offset;
    size = s_oat.data_size;
    return true;
}

extern "C" bool ElfUnInit() {
    //reset s_oat
    s_oat.file_offset = 0;
    s_oat.data_size = 0;

    return true;
}
