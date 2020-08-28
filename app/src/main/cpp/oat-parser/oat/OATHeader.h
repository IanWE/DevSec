#pragma once
#include "../art/compiler_filter.h"
#include "index_bss_mapping.h"
#include "../dex/type_lookup_table.h"
#include "../dex/dex_file.h"
#include <array>
#include <vector>

namespace Art {
//enum class InstructionSet;
enum class InstructionSet;
class InstructionSetFeatures;
class DexLayoutSections;
class DexFile;
class TypeLookupTable;
#pragma pack(4)
#include <stdio.h>
class OATHeader {
    public:
        OATHeader() { };
        ~OATHeader() { };
	    static constexpr std::array<uint8_t, 4> kOatMagic { { 'o', 'a', 't', '\n' } };
        // Last oat version changed reason: Remove unused trampoline entrypoints.
        static constexpr std::array<uint8_t, 4> kOatVersion { { '1', '7', '0', '\0' } };//need redundant compilation in coresponding codes.

        static constexpr const char* kDex2OatCmdLineKey = "dex2oat-cmdline";
        static constexpr const char* kDebuggableKey = "debuggable";
        static constexpr const char* kNativeDebuggableKey = "native-debuggable";
        static constexpr const char* kCompilerFilter = "compiler-filter";
        static constexpr const char* kClassPathKey = "classpath";
        static constexpr const char* kBootClassPathKey = "bootclasspath";
        static constexpr const char* kBootClassPathChecksumsKey = "bootclasspath-checksums";
        static constexpr const char* kConcurrentCopying = "concurrent-copying";
        static constexpr const char* kCompilationReasonKey = "compilation-reason";
        static constexpr const char kTrueValue[] = "true";
        static constexpr const char kFalseValue[] = "false";

        //static OATHeader* Create(InstructionSet instruction_set,
        //                         const InstructionSetFeatures* instruction_set_features,
        //                         uint32_t dex_file_count,
        //                         const SafeMap<std::string, std::string>* variable_data);

        bool IsValid() const;

        const char *GetMagic() const;

        uint32_t GetChecksum() const;

        uint32_t GetDexFileCount() const {
            return dex_file_count_;
        }

        uint32_t GetExecutableOffset() const {
            return executable_offset_;
        }

        uint32_t GetKeyValueStoreSize() const {
            return key_value_store_size_;
        }

        InstructionSet GetInstructionSet() const {
	    return instruction_set_;
        }

        const uint8_t* GetKeyValueStore() const {
              return key_value_store_;
	}

	uint32_t GetOatDexFilesOffset() const {
	    return oat_dex_files_offset_;
	}

    private:
        friend class OATParser;

	std::array<uint8_t, 4> magic_;
        std::array<uint8_t, 4> version_;
        uint32_t oat_checksum_;//20

        InstructionSet instruction_set_;//24
        uint32_t instruction_set_features_bitmap_;//28
        uint32_t dex_file_count_;

	uint32_t oat_dex_files_offset_;
        uint32_t executable_offset_;
        uint32_t jni_dlsym_lookup_offset_;
        uint32_t quick_generic_jni_trampoline_offset_;
        uint32_t quick_imt_conflict_trampoline_offset_;
        uint32_t quick_resolution_trampoline_offset_;
        uint32_t quick_to_interpreter_bridge_offset_;
      
        uint32_t key_value_store_size_;
        uint8_t key_value_store_[0];  // note variable width data at end
	//DISALLOW_COPY_AND_ASSIGN(OatHeader);
    };

enum OatClassType {
  kOatClassAllCompiled = 0,   // OatClass is followed by an OatMethodOffsets for each method.
  kOatClassSomeCompiled = 1,  // A bitmap of which OatMethodOffsets are present follows the OatClass.
  kOatClassNoneCompiled = 2,  // All methods are interpreted so no OatMethodOffsets are necessary.
  kOatClassMax = 3,
};

class OatMethodOffsets {
 public:
  explicit OatMethodOffsets(uint32_t code_offset = 0);
  ~OatMethodOffsets();
  OatMethodOffsets(const OatMethodOffsets&) = default;
  OatMethodOffsets& operator=(const OatMethodOffsets&) = default;
  uint32_t code_offset_;
};

#pragma pack()
}

