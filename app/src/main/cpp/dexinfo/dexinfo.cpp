/*
 * dexinfo - a very rudimentary dex file parser
 *
 * Copyright (C) 2014 Keith Makan (@k3170Makan)
 * Copyright (C) 2012-2013 Pau Oliva Fora (@pof)
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <getopt.h>
#include <vector>
#include "dexinfo.h"
#include "../logoutput.h"
#define VERSION "0.1"

using namespace std;
vector<vector<string>> dexparse(char const *dexfile, vector<string> &classnames)
{
    //char *dexfile;
	FILE *input;
	size_t offset, offset2;
	ssize_t len;
	int i,c;
	int DEBUG=0;

	int static_fields_size;
	int instance_fields_size;
	int direct_methods_size;
	int virtual_methods_size; 

	int field_idx_diff;
	int field_access_flags;

	int method_idx_diff;
	int method_access_flags;
	int method_code_off;

	int key;

	dex_header header;
	class_def_struct class_def_item;
	u1* buffer;
	u1* buf;
	u1* buffer_pos;
	u1* str=NULL;

	method_id_struct method_id_item;
	method_id_struct* method_id_list;

	string_id_struct string_id_item;
	string_id_struct* string_id_list;

	type_id_struct type_id_item;
	type_id_struct* type_id_list;

	int size_uleb, size_uleb_value;
	input = fopen(dexfile, "rb");
    //input=(FILE *)dexfile;

	/* print dex header information */
	LOGD("[] Dex file: %s",(unsigned char*)dexfile);

	memset(&header, 0, sizeof(header));
	memset(&class_def_item, 0, sizeof(class_def_item));
	fread(&header, 1, sizeof(header), input);
/*
	LOGD("[] DEX magic: ");
	for (i=0;i<3;i++) printf("%02X ", header.magic.dex[i]);
	LOGD("%02X ", *header.magic.newline);
	for (i=0;i<3;i++) printf("%02X ", header.magic.ver[i]);
	LOGD("%02X ", *header.magic.zero);

	if ( (strncmp(header.magic.dex,"dex",3) != 0) || 
	     (strncmp(header.magic.newline,"\n",1) != 0) || 
	     (strncmp(header.magic.zero,"\0",1) != 0 ) ) {
		LOGE("ERROR: not a dex file\n");
		return 0;
	}

	LOGD("[] DEX version: %s\n", header.magic.ver);
	if (strncmp(header.magic.ver,"035",3) != 0) {
		fprintf (stderr,"Warning: Dex file version != 035\n");
	}

	printf ("[] Adler32 checksum: 0x%x\n", *header.checksum);

	printf ("[] SHA1 signature: ");
	for (i=0;i<20;i++) printf("%02x", header.signature[i]);
	printf("\n");

	if (*header.header_size != 0x70) {
		fprintf (stderr,"Warning: Header size != 0x70\n");
	}

	if (*header.endian_tag != 0x12345678) {
		fprintf (stderr,"Warning: Endian tag != 0x12345678\n");
	}

	printf("\n[] Number of classes in the archive: %d\n", *header.class_defs_size);
*/
	/* parse the strings */
	string_id_list = (string_id_struct*)malloc(*header.string_ids_size*sizeof(string_id_item));
	fseek(input, *header.string_ids_off, SEEK_SET);
	fread(string_id_list, 1, *header.string_ids_size*sizeof(string_id_item), input);

	/* parse the types */
	type_id_list = (type_id_struct*)malloc(*header.type_ids_size*sizeof(type_id_item));
	fseek(input, *header.type_ids_off, SEEK_SET);
	fread(type_id_list, 1, *header.type_ids_size*sizeof(type_id_item), input);

	/* parse methods */
	method_id_list = (method_id_struct*)malloc(*header.method_ids_size*sizeof(method_id_item));
	fseek(input, *header.method_ids_off, SEEK_SET);
	fread(method_id_list, 1, *header.method_ids_size*sizeof(method_id_item), input);

	vector<vector<string> > func_list;
	/*Parse class definitions*/
	for (c=1; c <= (int)*header.class_defs_size; c++) { /*run through all the class */
		// change the position to the class_def_struct of each class
		offset = *header.class_defs_off + ((c-1)*sizeof(class_def_item)); /*get the offset for this class definition*/
		fseek(input, offset, SEEK_SET);
		//printf("[] Class %d ", c);
		// Get the class list
		fread(&class_def_item, 1, sizeof(class_def_item), input); /*read the class definition from the input*/
		/* print class filename */
		vector<string> cls;
		string class_name;
		if (*class_def_item.source_file_idx != 0xffffffff) {
			printClassFileName(string_id_list,class_def_item,input,str,class_name);
			//LOGD("class name: %s",class_name.c_str());
		} else {
			printf ("(No index): ");
		}
		//LOGD("Class %d: %s",c,cls[c-1]);
		if (DEBUG) {
			printf("\n");
			/* print type id */
			printf("\tclass_idx='0x%x':", *class_def_item.class_idx);
			printTypeDescForClass(string_id_list,type_id_list,class_def_item,input,str);
			printf("\taccess_flags='0x%x':", *class_def_item.access_flags); /*need to interpret this*/
			parseAccessFlags(*class_def_item.access_flags);
			printf("\tsuperclass_idx='0x%x':", *class_def_item.superclass_idx);
			printTypeDesc(string_id_list,type_id_list,*class_def_item.superclass_idx,input,str,"%s\n");
			printf("\tinterfaces_off='0x%x'\n", *class_def_item.interfaces_off); /*need to look this up in the DexTypeList*/
			printf("\tsource_file_idx='0x%x'\n", *class_def_item.source_file_idx);
            if (*class_def_item.source_file_idx != NO_INDEX) 
			printStringValue(string_id_list,*class_def_item.source_file_idx,input,str,"%s\n"); //causes a seg fault on some dex files
            // The seg fault was because there was no index value on the
            // class_def_item.scource_fie_idx
		/*should implement decoding the annotations directory items, we can use this to idenfiy Javascript interface accessible methods*/
			printf("\tannotations_off=0x%x\n", *class_def_item.annotations_off);
			printf("\tclass_data_off=0x%x (%d)\n", *class_def_item.class_data_off, *class_def_item.class_data_off);
			printf("\tstatic_values_off=0x%x (%d)\n", *class_def_item.static_values_off, *class_def_item.static_values_off);
		}

		// change position to class_data_off
		if (*class_def_item.class_data_off == 0) {// if it is a none class
			if (DEBUG) {
				printf ("\t0 static fields\n");
				printf ("\t0 instance fields\n");
				printf ("\t0 direct methods\n");
			} else {
				printf ("0 direct methods, 0 virtual methods\n");
			}
			classnames.push_back(class_name);
			func_list.push_back(cls);
			continue;
		} else {
			offset = *class_def_item.class_data_off;
			fseek(input, offset, SEEK_SET);
		}

		len = *header.map_off - offset;
		if (len < 1) {
			len = *header.file_size - offset;
			if (len < 1) {
				fprintf(stderr, "ERROR: invalid file length in dex header?\n");
				fclose(input);
				exit(1);
			}
		}

		buffer = (u1*)malloc(len * sizeof(u1));
		if (buffer == NULL) {
			fprintf(stderr, "ERROR: could not allocate memory!\n");
			fclose(input);
			exit(1);
		}
		buffer_pos=buffer;
		memset(buffer, 0, len);
		fread(buffer, 1, len, input);

		// from now on we continue on memory, as we have to parse uleb128
		static_fields_size = readUnsignedLeb128(&buffer);
		instance_fields_size = readUnsignedLeb128(&buffer);
		direct_methods_size = readUnsignedLeb128(&buffer);
		virtual_methods_size = readUnsignedLeb128(&buffer);

		if (DEBUG) printf ("\t%d static fields\n", static_fields_size);

		for (i=0;i<static_fields_size;i++) {
			field_idx_diff = readUnsignedLeb128(&buffer);
			field_access_flags = readUnsignedLeb128(&buffer);
			if (DEBUG) {
				printf ("\t\t[%d]|--field_idx_diff='0x%x'\n",i, field_idx_diff);
				//printTypeDesc(string_id_list,type_id_list,field_idx_diff,input,str," %s\n");
				printf ("\t\t    |--field_access_flags='0x%x'",field_access_flags);
				parseAccessFlags(field_access_flags);
			}
		}

		if (DEBUG) printf ("\t%d instance fields\n", instance_fields_size);

		for (i=0;i<instance_fields_size;i++) {
			field_idx_diff = readUnsignedLeb128(&buffer);
			field_access_flags = readUnsignedLeb128(&buffer);
			if (DEBUG) {
				printf ("\t\t[%d]|--field_idx_diff='0x%x'\n", i,field_idx_diff);
				//printTypeDesc(string_id_list,type_id_list,field_idx_diff,input,str,"%s\n");
				printf ("\t\t    |--field_access_flags='0x%x' :",field_access_flags);
				parseAccessFlags(field_access_flags);
			}
		}

		if (DEBUG) LOGD("%d direct methods, %d virtual methods\n", direct_methods_size, virtual_methods_size);


		if (DEBUG) printf ("\t%d direct methods\n", direct_methods_size);

		key=0;
		classnames.push_back(class_name);
		for (i=0;i<direct_methods_size;i++) {
			method_idx_diff = readUnsignedLeb128(&buffer);
			method_access_flags = readUnsignedLeb128(&buffer);
			method_code_off = readUnsignedLeb128(&buffer);

			/* methods */
			if (key == 0) key=method_idx_diff;
			else key += method_idx_diff;

			u2 class_idx=*method_id_list[key].class_idx;
			u2 proto_idx=*method_id_list[key].proto_idx;
			u4 name_idx=*method_id_list[key].name_idx;

			/* print method name ... should really do this stuff through a common function, its going to be annoying to debug this...:/ */
			offset2=*string_id_list[name_idx].string_data_off;
			fseek(input, offset2, SEEK_SET);

			buf = (u1*)malloc(10 * sizeof(u1));
			fread(buf, 1, sizeof(buf), input);
			size_uleb_value = uleb128_value(buf);
			size_uleb=len_uleb128(size_uleb_value);
			str = (u1*)malloc(size_uleb_value * sizeof(u1)+1);
			// offset2: on esta el tamany (size_uleb_value) en uleb32 de la string, seguit de la string
			fseek(input, offset2+size_uleb, SEEK_SET);
			fread(str, 1, size_uleb_value, input);
			str[size_uleb_value]='\0';

			//LOGD("\tdirect method %d = %s\n",i+1, str);
			cls.push_back((char*)str);
			free(str);
			str=NULL;
			if (DEBUG) {
				printf("\t\tmethod_code_off=0x%x\n", method_code_off);
				printf("\t\tmethod_access_flags='0x%x'\n", method_access_flags);
				//parseAccessFlags(method_access_flags);	
				printf("\t\tclass_idx='0x%x'\n", class_idx);
				//printTypeDesc(string_id_list,type_id_list,class_idx,input,str," %s\n");
				printf("\t\tproto_idx=0x%x\n", proto_idx);
			}
		}

		if (DEBUG) printf ("\t%d virtual methods\n", virtual_methods_size);

		key=0;
		for (i=0;i<virtual_methods_size;i++) {
			method_idx_diff = readUnsignedLeb128(&buffer);
			method_access_flags = readUnsignedLeb128(&buffer);
			method_code_off = readUnsignedLeb128(&buffer);

			/* methods */
			if (key == 0) key=method_idx_diff;
			else key += method_idx_diff;

			u2 class_idx=*method_id_list[key].class_idx;
			u2 proto_idx=*method_id_list[key].proto_idx;
			u4 name_idx=*method_id_list[key].name_idx;
			
			/* print method name */
			offset2=*string_id_list[name_idx].string_data_off;
			//printStringValue(string_id_list,name_idx,input,str,"%s\n");
			fseek(input, offset2, SEEK_SET);

			buf = (u1*)malloc(10 * sizeof(u1));
			fread(buf, 1, sizeof(buf), input);
			size_uleb_value = uleb128_value(buf);
			size_uleb=len_uleb128(size_uleb_value);
			str = (u1*)malloc(size_uleb_value * sizeof(u1)+1);
			// offset2: on esta el tamany (size_uleb_value) en uleb32 de la string, seguit de la string 
			fseek(input, offset2+size_uleb, SEEK_SET);
			fread(str, 1, size_uleb_value, input);
			str[size_uleb_value]='\0';

			//LOGD("\tvirtual method %d = %s\n",i+1, str);
			cls.push_back((char*)str);
			free(str);
			str=NULL;
			if (DEBUG) {
				printf("\t\tmethod_code_off=0x%x\n", method_code_off);
				printf("\t\tmethod_access_flags='0x%x'\n", method_access_flags);
				//parseAccessFlags(method_access_flags);	
				printf("\t\tclass_idx=0x%x\n", class_idx);
				printf("\t\tproto_idx=0x%x\n", proto_idx);
			}

		}
		func_list.push_back(cls);
		free(buffer_pos);
	}

	free(type_id_list);
	free(method_id_list);
	free(string_id_list);
	fclose(input);
	return func_list;
}
