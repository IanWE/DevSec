//
// Created by finder on 20-7-16.
//


#ifndef CCATTACK_DEXINFO_H
#define CCATTACK_DEXINFO_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <getopt.h>
#include <string>

typedef uint8_t             u1;
typedef uint16_t            u2;
typedef uint32_t            u4;
typedef uint64_t            u8;
typedef int8_t              s1;
typedef int16_t             s2;
typedef int32_t             s4;
typedef int64_t             s8;

using namespace std;

typedef struct {
    char dex[3];
    char newline[1];
    char ver[3];
    char zero[1];
} dex_magic;

typedef struct {
    dex_magic magic;
    u4 checksum[1];
    unsigned char signature[20];
    u4 file_size[1];
    u4 header_size[1];
    u4 endian_tag[1];
    u4 link_size[1];
    u4 link_off[1];
    u4 map_off[1];
    u4 string_ids_size[1];
    u4 string_ids_off[1];
    u4 type_ids_size[1];
    u4 type_ids_off[1];
    u4 proto_ids_size[1];
    u4 proto_ids_off[1];
    u4 field_ids_size[1];
    u4 field_ids_off[1];
    u4 method_ids_size[1];
    u4 method_ids_off[1];
    u4 class_defs_size[1];
    u4 class_defs_off[1];
    u4 data_size[1];
    u4 data_off[1];
} dex_header;

typedef struct {
    u4 class_idx[1];
    u4 access_flags[1];
    u4 superclass_idx[1];
    u4 interfaces_off[1];
    u4 source_file_idx[1];
    u4 annotations_off[1];
    u4 class_data_off[1];
    u4 static_values_off[1];
} class_def_struct;

typedef struct {
    u2 class_idx[1];
    u2 proto_idx[1];
    u4 name_idx[1];
} method_id_struct;

typedef struct {
    u4 string_data_off[1];
} string_id_struct;

typedef struct {
    u4 descriptor_idx[1];
} type_id_struct;

typedef struct {
    u4 descriptor_idx[1];
} proto_id_struct;

/*names for the access flags*/
const char * ACCESS_FLAG_NAMES[20] = {
        "public",
        "private",
        "protected",
        "static",
        "final",
        "synchronized",
        "super",
        "volatile",
        "bridge",
        "transient",
        "varargs",
        "native",
        "interface",
        "abstract",
        "strict",
        "synthetic",
        "annotation",
        "enum",
        "constructor",
        "declared_synchronized"};
/*values for the access flags, this and the preceeding list are used as a lookup dictionary*/
const u4 ACCESS_FLAG_VALUES[20] = {
        0x00000001,
        0x00000002,
        0x00000004,
        0x00000008,
        0x00000010,
        0x00000020,
        0x00000020,
        0x00000040,
        0x00000040,
        0x00000080,
        0x00000080,
        0x00000100,
        0x00000200,
        0x00000400,
        0x00000800,
        0x00001000,
        0x00002000,
        0x00004000,
        0x00010000,
        0x00020000};

const u4 NO_INDEX = 0xffffffff;

int readUnsignedLeb128(u1** pStream)
{
/* taken from dalvik's libdex/Leb128.h */
    u1* ptr = *pStream;
    int result = *(ptr++);

    if (result > 0x7f) {
        int cur = *(ptr++);
        result = (result & 0x7f) | ((cur & 0x7f) << 7);
        if (cur > 0x7f) {
            cur = *(ptr++);
            result |= (cur & 0x7f) << 14;
            if (cur > 0x7f) {
                cur = *(ptr++);
                result |= (cur & 0x7f) << 21;
                if (cur > 0x7f) {
                    /*
                     * Note: We don't check to see if cur is out of
                     * range here, meaning we tolerate garbage in the
                     * high four-order bits.
                     */
                    cur = *(ptr++);
                    result |= cur << 28;
                }
            }
        }
    }

    *pStream = ptr;
    return result;
}

int uleb128_value(u1* pStream)
{
    u1* ptr = pStream;
    int result = *(ptr++);

    if (result > 0x7f) {
        int cur = *(ptr++);
        result = (result & 0x7f) | ((cur & 0x7f) << 7);
        if (cur > 0x7f) {
            cur = *(ptr++);
            result |= (cur & 0x7f) << 14;
            if (cur > 0x7f) {
                cur = *(ptr++);
                result |= (cur & 0x7f) << 21;
                if (cur > 0x7f) {
                    /*
                     * Note: We don't check to see if cur is out of
                     * range here, meaning we tolerate garbage in the
                     * high four-order bits.
                     */
                    cur = *(ptr++);
                    result |= cur << 28;
                }
            }
        }
    }
    return result;
}


size_t len_uleb128(unsigned long n)
{
    static unsigned char b[32];
    size_t i;

    i = 0;
    do
    {
        b[i] = n & 0x7F;
        if (n >>= 7)
            b[i] |= 0x80;
    }
    while (b[i++] & 0x80);
    return i;
}
/*wrote this to avoid dumping the leb128 parsing and grabbing code all over the place ;)*/
std::string
printUnsignedLebValue(char *format,
                      u1 *stringData,
                      size_t offset,
                      FILE* DexFile){

    u1 *uLebBuff;
    int uLebValue, uLebValueLength;

    fseek(DexFile,offset,SEEK_SET); /*move position to the string in data section*/
    uLebBuff = (u1*)malloc(10*sizeof(u1))	;
    fread(uLebBuff,1,sizeof(uLebBuff),DexFile);

    uLebValue = uleb128_value(uLebBuff);
    uLebValueLength = len_uleb128(uLebValue);
    stringData = (u1*)malloc(uLebValue * sizeof(u1)+1);

    fseek(DexFile, offset+uLebValueLength ,SEEK_SET);
    fread(stringData,1,uLebValue,DexFile);

    stringData[uLebValue]='\0';
    std::string tmp = (char*) stringData;
    printf(format,stringData);
    free(uLebBuff);
    return tmp;
}
/*this allows us to print ACC_FLAGS symbolically*/
void parseAccessFlags(u4 flags){
    int i = 0;
    if (flags){
        for (;i<20;i++){
            if (flags & ACCESS_FLAG_VALUES[i]){
                printf(" %s ",ACCESS_FLAG_NAMES[i]);
            }
        }
    }
    printf("\n");
}
/*not entirely sure how I should use these methods, as is they are only usefull for printing values, and don't return them :/
though as a tradeoff I've made the methods manipulate the string data in place, so the conversion to returning them would be easy */
/*Generic methods for printing types*/
void
printStringValue(string_id_struct *strIdList,
                 u4 offset_pointer,
                 FILE* DexFile,
                 u1* stringData,
                 char* format){

    size_t strIdOff;
    if (offset_pointer){
        strIdOff = *strIdList[offset_pointer].string_data_off; /*get the offset to the string in the data section*/
        /*would be cool if we have a RAW mode, with only hex unparsed data, and a SYMBOLIC mode where all the data is parsed and interpreted */
        printUnsignedLebValue(format,stringData,strIdOff,DexFile);
    }
    else{
        printf("none\n");
    }
    free(stringData);
    stringData=NULL;

}

void
printTypeDesc(string_id_struct *strIdList,
              type_id_struct* typeIdList,
              u4 offset_pointer,
              FILE* DexFile,
              u1* stringData,
              char* format){

    size_t strIdOff;
    if (offset_pointer){
        strIdOff = *strIdList[*typeIdList[offset_pointer].descriptor_idx].string_data_off; /*get the offset to the string in the data section*/
        /*would be cool if we have a RAW mode, with only hex unparsed data, and a SYMBOLIC mode where all the data is parsed and interpreted */
        printUnsignedLebValue(format,stringData,strIdOff,DexFile);
    }
    else{
        printf("none\n");
    }
    free(stringData);
    stringData=NULL;

}
void
printClassFileName(string_id_struct *strIdList,
                   class_def_struct classDefItem,
                   FILE *DexFile,
                   u1* stringData,
                   std::string &class_name){

    size_t strIdOff;
    if (classDefItem.source_file_idx){
        strIdOff = *strIdList[*classDefItem.source_file_idx].string_data_off; /*get the offset to the string in the data section*/
        class_name = printUnsignedLebValue("(%s)\n",stringData,strIdOff,DexFile);
    }
    else{
        printf("none\n");
    }
    free(stringData);
    stringData=NULL;
}
void
printTypeDescForClass(string_id_struct *strIdList,
                      type_id_struct* typeIdList,
                      class_def_struct classDefItem,
                      FILE *DexFile,
                      u1* stringData){
    size_t strIdOff;
    if (classDefItem.class_idx){
        strIdOff = *strIdList[*typeIdList[*classDefItem.class_idx].descriptor_idx].string_data_off; /*get the offset to the string in the data section*/
        printUnsignedLebValue("%s\n",stringData,strIdOff,DexFile);
        free(stringData);
        stringData=NULL;
    }
    else{
        printf("none\n");
    }
}
void parseClass(){

}
void parseHeader(){

}
void help_show_message()
{
    fprintf(stderr, "Usage: dexinfo <file.dex> [options]\n");
    fprintf(stderr, " options:\n");
    fprintf(stderr, "    -V             print verbose information\n");
}
vector<vector<string>> dexparse(const char *dexfile, vector<string> &classnames);
#endif //CCATTACK_DEXINFO_H
