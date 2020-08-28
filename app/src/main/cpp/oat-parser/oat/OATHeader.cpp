#include <string.h>
#include <stdint.h>
#include <array>
#include <vector>

#include "instruction_set.h"
#include "OATHeader.h"

namespace Art {
    //const uint8_t OATHeader::kOatMagic[] = {'o', 'a', 't', '\n'};
    //const uint8_t OATHeader::kOatVersion[] = {'0', '4', '6', '\0'};
    static const int kPageSize = 4096;
    
    constexpr std::array<uint8_t, 4> OATHeader::kOatMagic;
    constexpr std::array<uint8_t, 4> OATHeader::kOatVersion;

    template<int n, typename T>
    static inline bool IsAligned(T x) {
        static_assert((n & (n - 1)) == 0, "n is not a power of two");
        return (x & (n - 1)) == 0;
    }

    template<int n, typename T>
    static inline bool IsAligned(T *x) {
        return IsAligned<n>(reinterpret_cast<const uintptr_t>(x));
    }

    bool OATHeader::IsValid() const {
        //if (memcmp(magic_, kOatMagic, sizeof(kOatMagic)) != 0) {
        //    return false;
        //}
	if (magic_ != kOatMagic) {
            return false;
        }
	if (version_ != kOatVersion) {
            return false;
        }
        if (!IsAligned<kPageSize>(executable_offset_)) {
            return false;
        }
        //if (!IsValidInstructionSet(instruction_set_)) {
        //    return false;
        //}
        return true;
    }

    const char *OATHeader::GetMagic() const {
        return reinterpret_cast<const char *>(magic_.data());
    }

    uint32_t OATHeader::GetChecksum() const {
        return oat_checksum_;
    }

}
