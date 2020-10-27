// OATParser.cpp : Defines the exported functions for the DLL application.
//

#include <stdio.h>
#include <iostream>
#include <sstream>
#include <string.h>
#include <stdint.h>
#include <stddef.h>
#include <dirent.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "../logoutput.h"
#include "OATParser.h"
#include "DexHeader.h"
#include "OATHeader.h"
#include "elfloader.h"
#include "../zlib/zlib.h"
#include "instruction_set.h"
#include "../base/bit_vector.h"

namespace Art {
    //OATParser OATParser::m_oat_parser;
    std::unique_ptr<char[]> buf;
    OATParser::OATParser(const unsigned char *a_oat, unsigned int a_size) : m_oat_begin(a_oat) {
        m_oat_end = a_oat + a_size;
    }

    size_t OATParser::Size(){
        return OATParser::End() - OATParser::Begin();
    }

    OATParser::~OATParser() {
    }

    void OATParser::init(const char *a_oat_file, const char *a_out_dex_path) {
        m_oat_file = a_oat_file;
        m_out_dex_path = a_out_dex_path;
    }

    const uint8_t *OATParser::Begin() const{
        return m_oat_begin;
    }
    const uint8_t  *OATParser::End() const{
        return m_oat_end;
    }

    bool OATParser::OpenOat(std::unique_ptr<char[]> &a_buf, unsigned int &a_len) {
        bool ret = false;
        FILE *f = fopen(m_oat_file.c_str(), "rb");
        if (NULL == f) {
            return ret;
        }
        fseek(f, 0, SEEK_END);
        a_len = ftell(f);
        fseek(f, 0, SEEK_SET);
        a_buf.reset(new char[a_len]);
        fread(a_buf.get(), sizeof(char), a_len, f);
        ret = true;
        fclose(f);
        return ret;
    }

    bool OATParser::OatReSet(std::unique_ptr<char[]> &a_buf) {
        unsigned int offset = 0;
        unsigned int len = 0;

        bool ret = GetOatInfo(offset, len);

        m_oat_begin = (const uint8_t *)a_buf.get() + offset;
        m_header_offset = offset;
        m_oat_end = (const uint8_t *)a_buf.get() + offset + len;
        m_oatheader.reset(new OATHeader());
        memcpy(m_oatheader.get(), m_oat_begin, sizeof(OATHeader));
        return ret;
    }

    // Advance start until it is either end or \0.
    static const char* ParseString(const char* start, const char* end) {
       while (start < end && *start != 0) {
              start++;
       }
            return start;
    }

    const char* OATParser::GetStoreValueByKey(const char *key_string, const char* key, const int size) {
            const char* ptr = key_string;
            const char* end = ptr + size;

            while (ptr < end) {
            // Scan for a closing zero.
            const char* str_end = ParseString(ptr, end);
            if (str_end < end) {
              if (strcmp(key, ptr) == 0) {
                // Same as key. Check if value is OK.
                if (ParseString(str_end + 1, end) < end) {
                  return str_end + 1;
                }
              } else {
                // Different from key. Advance over the value.
                ptr = ParseString(str_end + 1, end) + 1;
              }
            } else {
              break;
            }
          }
          // Not found.
          return nullptr;
       }

    const char* OATParser::GetCompilerFilter(const char *key_string, int key_string_size) {
        const char* kCompilerFilter = "compiler-filter";
        const char* key_value = GetStoreValueByKey(key_string, kCompilerFilter, key_string_size);
        return key_value;
    }

template <typename T>
inline static bool ReadOatDexFileData(const OATParser* oat_file,
                                          /*inout*/const uint8_t** oat,
                                          /*out*/T* value) {
      if (static_cast<size_t>(oat_file->End() - *oat) < sizeof(T)) {
        return false;
      }
      using unaligned_type __attribute__((__aligned__(1))) = T;
      *value = *reinterpret_cast<const unaligned_type*>(*oat);
      *oat += sizeof(T);
      return true;
    }


static bool ReadIndexBssMapping(OATParser* oat_file,
                                /*inout*/const uint8_t** oat,
                                size_t dex_file_index,
                                //const std::string& dex_file_location,
                                const char* tag,
                                /*out*/const IndexBssMapping** mapping) {
  uint32_t index_bss_mapping_offset;
  if (!ReadOatDexFileData(oat_file, oat, &index_bss_mapping_offset)) {
    LOGE("read index_bss_mapping error %s\n",tag);
    return false;
    }
  const bool readable_index_bss_mapping_size = index_bss_mapping_offset != 0u && index_bss_mapping_offset <= oat_file->Size();// && oat_file->Size() - index_bss_mapping_offset >= IndexBssMapping::ComputeSize(0);
  const IndexBssMapping* index_bss_mapping = readable_index_bss_mapping_size
      ? reinterpret_cast<const IndexBssMapping*>(oat_file->Begin() + index_bss_mapping_offset)
      : nullptr;
  if (index_bss_mapping_offset != 0u && index_bss_mapping == nullptr)// ||
//        index_bss_mapping->size() == 0u )
  {
    LOGE("bss truncated");
    return false;
  }
  *mapping = index_bss_mapping;
  return true;
  }

  OatDexFile::OatDexFile(const OATParser* oat_file,
                       const std::string& dex_file_location,
                       const std::string& canonical_dex_file_location,
                       uint32_t dex_file_location_checksum,
                       const uint8_t* dex_file_pointer,
                       const uint8_t* lookup_table_data,
                       const IndexBssMapping* method_bss_mapping_data,
                       const IndexBssMapping* type_bss_mapping_data,
                       const IndexBssMapping* string_bss_mapping_data,
                       const uint32_t* oat_class_offsets_pointer,
                       const DexLayoutSections* dex_layout_sections,
                       const std::string& oat_dex_file_location)
      : oat_file_(oat_file),
      dex_file_location_(dex_file_location),
      canonical_dex_file_location_(canonical_dex_file_location),
      dex_file_location_checksum_(dex_file_location_checksum),
      dex_file_pointer_(dex_file_pointer),
      lookup_table_data_(lookup_table_data),
      method_bss_mapping_(method_bss_mapping_data),
      type_bss_mapping_(type_bss_mapping_data),
      string_bss_mapping_(string_bss_mapping_data),
      oat_class_offsets_pointer_(oat_class_offsets_pointer),
      lookup_table_(),
      dex_layout_sections_(dex_layout_sections),
      oat_dex_file_location(oat_dex_file_location){};

  template <typename Enumeration>
  auto as_integer(Enumeration const value) -> typename std::underlying_type<Enumeration>::type
{
 return static_cast<typename std::underlying_type<Enumeration>::type>(value);
}

  OATParser::OatClass OatDexFile::GetOatClass(uint16_t class_def_index) const
  {
    uint32_t oat_class_offset = GetOatClassOffset(class_def_index);
    const uint8_t* oat_class_pointer = oat_file_->Begin() + oat_class_offset;

    const uint8_t* status_pointer = oat_class_pointer;//
    //ClassStatus status = enum_cast<ClassStatus>(*reinterpret_cast<const int16_t*>(status_pointer));
    int16_t status = *reinterpret_cast<const int16_t*>(status_pointer);
    //std::cout<<oat_class_pointer<<":"<<as_integer(status)<<std::endl; 
    //LOGD("Status: %d",status);//
    const uint8_t* type_pointer = status_pointer + sizeof(uint16_t);
    OatClassType type = static_cast<OatClassType>(*reinterpret_cast<const uint16_t*>(type_pointer));//check compilation
    //LOGD("OatClassType: %d",type); //kOatClassNoneCompiled
    const uint8_t* after_type_pointer = type_pointer + sizeof(int16_t);
    uint32_t bitmap_size = 0;
    const uint8_t* bitmap_pointer = nullptr;
    const uint8_t* methods_pointer = nullptr;
    if (type != kOatClassNoneCompiled){
        if (type == kOatClassSomeCompiled) {
            //LOGD("It's Somecompiled. \n");
            bitmap_size = static_cast<uint32_t>(*reinterpret_cast<const uint32_t*>(after_type_pointer));
            bitmap_pointer = after_type_pointer + sizeof(bitmap_size);
	        methods_pointer = bitmap_pointer + bitmap_size;
        } else {
	        LOGD("It's Allcompiled. \n");
            methods_pointer = after_type_pointer;
        }
    }
    else {
        //LOGD("It's Nonecompiled. \n");
    }
    return OATParser::OatClass(oat_file_,
                           status,
                           type,
                           bitmap_size,
                           reinterpret_cast<const uint32_t*>(bitmap_pointer),
                           reinterpret_cast<const OatMethodOffsets*>(methods_pointer));
    //ClassStatus status = enum_cast<ClassStatus>(*reinterpret_cast<const int16_t*>(status_pointer));
  }

const OatMethodOffsets* OATParser::OatClass::GetOatMethodOffsets(uint32_t method_index) const {
  // NOTE: We don't keep the number of methods and cannot do a bounds check for method_index.
  if (methods_pointer_ == nullptr) {
    //LOGE("(No this function)Null pointer.\n");
    return nullptr;
  }
  size_t methods_pointer_index;
  if (bitmap_ == nullptr) {
    methods_pointer_index = method_index;
  } else {
    if (!BitVector::IsBitSet(bitmap_, method_index)) {
      return nullptr;
    }
    size_t num_set_bits = BitVector::NumSetBits(bitmap_, method_index);
    methods_pointer_index = num_set_bits;
  }
  //const OatMethodOffsets& oat_method_offsets = methods_pointer_[methods_pointer_index];
  const OatMethodOffsets& oat_method_offsets = methods_pointer_[methods_pointer_index];
  return &oat_method_offsets;
}

const OATParser::OatMethod OATParser::OatClass::GetOatMethod(uint32_t method_index) const {
  const OatMethodOffsets* oat_method_offsets = GetOatMethodOffsets(method_index);
  if (oat_method_offsets == nullptr) {
    //LOGD("Method %d is null\n",method_index);
    return OatMethod(nullptr, 0);
  }
  //if (oat_file_->IsExecutable() ||
  //    Runtime::Current() == nullptr ||        // This case applies for oatdump.
  //    Runtime::Current()->IsAotCompiler()) {
  return OatMethod(oat_file_->Begin(), oat_method_offsets->code_offset_);
}
  // We aren't allowed to use the compiled code. We just force it down the interpreted / jit
  // version.
bool OATParser::ParseOatFile(const std::string read_file) {
        m_oat_file = read_file;
        unsigned int oat_len = 0;
        
        if (!OpenOat(buf, oat_len)) {//Read oat into buf, length into oat_len
            LOGE("Open Oat %s Error!",m_oat_file.c_str());
            return false;
        }
 
        if (!ElfInit(buf.get(), oat_len)) {//Check if it is elf, and read     
	    // in elfloader.cpp static File::Location s_oat(0, 0);
	    //s_oat.file_offset;
            //s_oat.data_size;
            return false;
        }

        if (!OatReSet(buf)) {
            return false;
        }

        const uint8_t *oat = Begin(); //get m_oat_begin

        oat += sizeof(OATHeader);
        if (oat > End()) {
            return false;
        }

        InstructionSet iset = GetOatHeader()->GetInstructionSet();
        // 跳过一些key-value的存储值
        uint32_t hs = GetOatHeader()->GetKeyValueStoreSize();

        const char *compilerFilter = GetCompilerFilter((const char *)oat, hs);
        //LOGD("Oat compilter filter: %s",compilerFilter);

        oat += hs;

        if (oat > End()) {
            return false;
        }

	    size_t oat_dex_files_offset = GetOatHeader()->GetOatDexFilesOffset();
    	oat = Begin() + oat_dex_files_offset; // Jump to the OatDexFile records.

    	uint32_t dex_file_count = GetOatHeader()->GetDexFileCount();
    	oat_dex_files_storage_.reserve(dex_file_count);
	    //LOGD("dex_file_count: %d",dex_file_count);
	    for (size_t i = 0; i < dex_file_count; i++) {
	      uint32_t dex_file_location_size;
	      if (!ReadOatDexFileData(this, &oat, &dex_file_location_size)) {
            LOGE("Read dex_file_location_size error!,%d,%d", i, dex_file_location_size);
            return false;
          }
	      //LOGD("dex_file_location_size %x\n",dex_file_location_size);
	      if(dex_file_location_size==0U){
	        LOGE("found OatDexFile empty");
	        return false;
	      }
	    const char* dex_file_location_data = reinterpret_cast<const char*>(oat);
	    oat += dex_file_location_size;
	    std::string oat_dex_file_location(dex_file_location_data, dex_file_location_size);// Location encoded in the oat file.
	    LOGD("oat_dex_file_location:%s",oat_dex_file_location.c_str());
	    std::string dex_file_name;
	    std::string dex_file_location;
	    //ResolveRelativeEncodedDexLocation(abs_dex_location,
         //                         oat_dex_file_location,
         //                         &dex_file_location,
         //                         &dex_file_name);

	    uint32_t dex_file_checksum;
	    if(!ReadOatDexFileData(this,&oat,&dex_file_checksum))
        {
	        LOGE("Read checksum error!,%d,%d",i,dex_file_checksum);
	        return false;
	    }

	    uint32_t dex_file_offset;
	    if(!ReadOatDexFileData(this,&oat,&dex_file_offset))
	    {
          LOGE("Read file offset error!,%d,%x",i,dex_file_offset);
	      return false;
	    }

	    const uint8_t* dex_file_pointer = nullptr;
	    //LOGD("dex_file_offset %d\n",dex_file_offset);
	    if (dex_file_offset == 0U) {
	        if (uncompressed_dex_files_ == nullptr) {
		        if(i>0) {
			       LOGE("unsupported uncompressed-file!");
			       return false;
		        }
		        uncompressed_dex_files_.reset(new std::vector<std::unique_ptr<const DexFile>>());
		        //const ArtDexFileLoader dex_file_loader;
		        bool loaded = false;
             }//uncompressed_dex_files_ == nullptr
        }

	    uint32_t class_offsets_offset;
	    if(!ReadOatDexFileData(this,&oat,&class_offsets_offset))
	    {
           LOGE("Read class offset error!,%d,%x",i,class_offsets_offset);
           return false;
	    }
	    if(class_offsets_offset > Size())//||(Size() - class_offsets_offset) / sizeof(uint32_t) < header->class_defs_size_)
	    {
           LOGE("Error: truncated class offsets\n");
	       return false;
	    }
	    const uint32_t* class_offsets_pointer =  reinterpret_cast<const uint32_t*>(Begin() + class_offsets_offset);

	    uint32_t lookup_table_offset;
	    if (!ReadOatDexFileData(this, &oat, &lookup_table_offset))
	    {
           LOGE("\nError: Load lookup_table_offset error\n");
           return false;
	    }

	    const uint8_t* lookup_table_data = lookup_table_offset != 0u ? Begin()+ lookup_table_offset:nullptr;
	    uint32_t dex_layout_sections_offset;
	    if (!ReadOatDexFileData(this, &oat, &dex_layout_sections_offset)) {
            LOGE("Error: Load dex layout sections error");
            return false;
	    }
	    const DexLayoutSections* const dex_layout_sections = dex_layout_sections_offset != 0 ? reinterpret_cast<const DexLayoutSections*>(Begin() + dex_layout_sections_offset):nullptr;
	    const IndexBssMapping* method_bss_mapping;
	    const IndexBssMapping* type_bss_mapping;
	    const IndexBssMapping* string_bss_mapping;
	    if (!ReadIndexBssMapping(this, &oat, i,  "method", &method_bss_mapping) ||  !ReadIndexBssMapping(this, &oat, i,  "type", &type_bss_mapping) || !ReadIndexBssMapping(this, &oat, i, "string", &string_bss_mapping)) {
           return false;
	    }
	    std::string canonical;
	    OatDexFile* oat_dex_file = new OatDexFile(
          this,
          dex_file_location,
          //DexFileLoader::GetDexCanonicalLocation(dex_file_name.c_str()),
	      canonical,
          dex_file_checksum,
          dex_file_pointer,
          lookup_table_data,
          method_bss_mapping,
          type_bss_mapping,
          string_bss_mapping,
          class_offsets_pointer,
          dex_layout_sections,
          oat_dex_file_location);
        oat_dex_files_storage_.push_back(oat_dex_file);
	    LOGD("Create OatDexFile:%d\n",oat_dex_files_storage_.size());
	    //LOGD("test: %s %x", oat_dex_files_storage_[0]->GetLocation().c_str(),\
	    //        oat_dex_files_storage_[0]->GetOatClass(0).GetOatMethod(1).GetOffset());
	    //In this time, the file is still in the memory, so we can read the oat class and method.
	    //But if we read the oat from out side, the file is not in the file, the offset returns a 0;
	    /*
	    LOGD("==============Method Parser Start==================\n");
	    for(int index=0;index<1;index++){
	        OatClass oatcls = oat_dex_file->GetOatClass(index);
	        LOGD("OatClass %d:%x\n",index);
	        uint32_t method_index = 1;
	        OatMethod m = oatcls.GetOatMethod(method_index);
	        LOGD("OatMethod %d: %x\n", method_index,m.GetOffset());
	    }
	    LOGD("===============Method  Parser Finished==============\n");
        */
    }//for
    return true;
  }//func
}//Art

// 初始化， oat_file为原始的oat文件名 out_dex_path为输出的dex路径
//extern "C" bool InitOatParser(const char *oat_file, const char *out_dex_path) {
//    Art::OATParser::GetInstance().init(oat_file, out_dex_path);
//    return true;
//}
//extern "C" bool ParseOatFile(const std::string read_file) {
//    return Art::OATParser::GetInstance().ParseOatFile(read_file);
//}

