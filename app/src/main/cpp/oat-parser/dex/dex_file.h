#include <memory>
//#include "dex_file_types.h"
//#include "dex_file_structs.h"

namespace dex{
  class StringId;
  class TypeId;
  class FieldId;
  class MethodId;
  class ProtoId;
  class ClassDef;
  class MethodHandleItem;
  class CallSiteIdItem;
  class HiddenapiClassData;
}
namespace Art{
class DexFileContainer;
class OatDexFile;

namespace hiddenapi {
enum class Domain {
  kCorePlatform = 0,
  kPlatform,
  kApplication,
};
}

class DexFile {
 public:
  // Number of bytes in the dex file magic.
  static constexpr size_t kDexMagicSize = 4;
  static constexpr size_t kDexVersionLen = 4;

  // First Dex format version enforcing class definition ordering rules.
  static const uint32_t kClassDefinitionOrderEnforcedVersion = 37;

  static constexpr size_t kSha1DigestSize = 20;
  static constexpr uint32_t kDexEndianConstant = 0x12345678;

  // The value of an invalid index.
  static const uint16_t kDexNoIndex16 = 0xFFFF;
  static const uint32_t kDexNoIndex32 = 0xFFFFFFFF;

  // Raw header_item.
  struct Header {
    uint8_t magic_[8] = {};
    uint32_t checksum_ = 0;  // See also location_checksum_
    uint8_t signature_[kSha1DigestSize] = {};
    uint32_t file_size_ = 0;  // size of entire file
    uint32_t header_size_ = 0;  // offset to start of next section
    uint32_t endian_tag_ = 0;
    uint32_t link_size_ = 0;  // unused
    uint32_t link_off_ = 0;  // unused
    uint32_t map_off_ = 0;  // map list offset from data_off_
    uint32_t string_ids_size_ = 0;  // number of StringIds
    uint32_t string_ids_off_ = 0;  // file offset of StringIds array
    uint32_t type_ids_size_ = 0;  // number of TypeIds, we don't support more than 65535
    uint32_t type_ids_off_ = 0;  // file offset of TypeIds array
    uint32_t proto_ids_size_ = 0;  // number of ProtoIds, we don't support more than 65535
    uint32_t proto_ids_off_ = 0;  // file offset of ProtoIds array
    uint32_t field_ids_size_ = 0;  // number of FieldIds
    uint32_t field_ids_off_ = 0;  // file offset of FieldIds array
    uint32_t method_ids_size_ = 0;  // number of MethodIds
    uint32_t method_ids_off_ = 0;  // file offset of MethodIds array
    uint32_t class_defs_size_ = 0;  // number of ClassDefs
    uint32_t class_defs_off_ = 0;  // file offset of ClassDef array
    uint32_t data_size_ = 0;  // size of data section
    uint32_t data_off_ = 0;  // file offset of data section

    // Decode the dex magic version
    uint32_t GetVersion() const;
  };

  // Map item type codes.
  enum MapItemType : uint16_t {  // private
    kDexTypeHeaderItem               = 0x0000,
    kDexTypeStringIdItem             = 0x0001,
    kDexTypeTypeIdItem               = 0x0002,
    kDexTypeProtoIdItem              = 0x0003,
    kDexTypeFieldIdItem              = 0x0004,
    kDexTypeMethodIdItem             = 0x0005,
    kDexTypeClassDefItem             = 0x0006,
    kDexTypeCallSiteIdItem           = 0x0007,
    kDexTypeMethodHandleItem         = 0x0008,
    kDexTypeMapList                  = 0x1000,
    kDexTypeTypeList                 = 0x1001,
    kDexTypeAnnotationSetRefList     = 0x1002,
    kDexTypeAnnotationSetItem        = 0x1003,
    kDexTypeClassDataItem            = 0x2000,
    kDexTypeCodeItem                 = 0x2001,
    kDexTypeStringDataItem           = 0x2002,
    kDexTypeDebugInfoItem            = 0x2003,
    kDexTypeAnnotationItem           = 0x2004,
    kDexTypeEncodedArrayItem         = 0x2005,
    kDexTypeAnnotationsDirectoryItem = 0x2006,
    kDexTypeHiddenapiClassData       = 0xF000,
  };
  enum class MethodHandleType : uint16_t {  // private
    kStaticPut         = 0x0000,  // a setter for a given static field.
    kStaticGet         = 0x0001,  // a getter for a given static field.
    kInstancePut       = 0x0002,  // a setter for a given instance field.
    kInstanceGet       = 0x0003,  // a getter for a given instance field.
    kInvokeStatic      = 0x0004,  // an invoker for a given static method.
    kInvokeInstance    = 0x0005,  // invoke_instance : an invoker for a given instance method. This
                                  // can be any non-static method on any class (or interface) except
                                  // for “<init>”.
    kInvokeConstructor = 0x0006,  // an invoker for a given constructor.
    kInvokeDirect      = 0x0007,  // an invoker for a direct (special) method.
    kInvokeInterface   = 0x0008,  // an invoker for an interface method.
    kLast = static_cast<int>(kInvokeInterface)
  };

  // Annotation constants.
  enum {
    kDexVisibilityBuild         = 0x00,     /* annotation visibility */
    kDexVisibilityRuntime       = 0x01,
    kDexVisibilitySystem        = 0x02,

    kDexAnnotationByte          = 0x00,
    kDexAnnotationShort         = 0x02,
    kDexAnnotationChar          = 0x03,
    kDexAnnotationInt           = 0x04,
    kDexAnnotationLong          = 0x06,
    kDexAnnotationFloat         = 0x10,
    kDexAnnotationDouble        = 0x11,
    kDexAnnotationMethodType    = 0x15,
    kDexAnnotationMethodHandle  = 0x16,
    kDexAnnotationString        = 0x17,
    kDexAnnotationType          = 0x18,
    kDexAnnotationField         = 0x19,
    kDexAnnotationMethod        = 0x1a,
    kDexAnnotationEnum          = 0x1b,
    kDexAnnotationArray         = 0x1c,
    kDexAnnotationAnnotation    = 0x1d,
    kDexAnnotationNull          = 0x1e,
    kDexAnnotationBoolean       = 0x1f,
    kDexAnnotationValueTypeMask = 0x1f,     /* low 5 bits */
    kDexAnnotationValueArgShift = 5,
  };

  enum AnnotationResultStyle {  // private
    kAllObjects,
    kPrimitivesOrObjects,
    kAllRaw
  };

  struct AnnotationValue;

  // Closes a .dex file.
  virtual ~DexFile();

  const std::string& GetLocation() const {
    return location_;
  }

  // For DexFiles directly from .dex files, this is the checksum from the DexFile::Header.
  // For DexFiles opened from a zip files, this will be the ZipEntry CRC32 of classes.dex.
  uint32_t GetLocationChecksum() const {
    return location_checksum_;
  }

  const Header& GetHeader() const {
    //DCHECK(header_ != nullptr) << GetLocation();
    return *header_;
  }

  // Decode the dex magic version
  uint32_t GetDexVersion() const {
    return GetHeader().GetVersion();
  }

  // Returns true if the byte string points to the magic value.
  virtual bool IsMagicValid() const = 0;

  // Returns true if the byte string after the magic is the correct value.
  virtual bool IsVersionValid() const = 0;

  // Returns true if the dex file supports default methods.
  virtual bool SupportsDefaultMethods() const = 0;

  // Returns the maximum size in bytes needed to store an equivalent dex file strictly conforming to
  // the dex file specification. That is the size if we wanted to get rid of all the
  // quickening/compact-dexing/etc.
  //
  // TODO This should really be an exact size! b/72402467
  virtual size_t GetDequickenedSize() const = 0;
  struct PositionInfo {
    PositionInfo() = default;

    uint32_t address_ = 0;  // In 16-bit code units.
    uint32_t line_ = 0;  // Source code line number starting at 1.
    const char* source_file_ = nullptr;  // nullptr if the file from ClassDef still applies.
    bool prologue_end_ = false;
    bool epilogue_begin_ = false;
  };

  struct LocalInfo {
    LocalInfo() = default;

    const char* name_ = nullptr;  // E.g., list.  It can be nullptr if unknown.
    const char* descriptor_ = nullptr;  // E.g., Ljava/util/LinkedList;
    const char* signature_ = nullptr;  // E.g., java.util.LinkedList<java.lang.Integer>
    uint32_t start_address_ = 0;  // PC location where the local is first defined.
    uint32_t end_address_ = 0;  // PC location where the local is no longer defined.
    uint16_t reg_ = 0;  // Dex register which stores the values.
    bool is_live_ = false;  // Is the local defined and live.
  };

  enum {
    DBG_END_SEQUENCE         = 0x00,
    DBG_ADVANCE_PC           = 0x01,
    DBG_ADVANCE_LINE         = 0x02,
    DBG_START_LOCAL          = 0x03,
    DBG_START_LOCAL_EXTENDED = 0x04,
    DBG_END_LOCAL            = 0x05,
    DBG_RESTART_LOCAL        = 0x06,
    DBG_SET_PROLOGUE_END     = 0x07,
    DBG_SET_EPILOGUE_BEGIN   = 0x08,
    DBG_SET_FILE             = 0x09,
    DBG_FIRST_SPECIAL        = 0x0a,
    DBG_LINE_BASE            = -4,
    DBG_LINE_RANGE           = 15,
  };
 protected:
  // First Dex format version supporting default methods.
  static const uint32_t kDefaultMethodsVersion = 37;

  //DexFile(const uint8_t* base,
  //        size_t size,
  //        const uint8_t* data_begin,
  //        size_t data_size,
  //        const std::string& location,
  //        uint32_t location_checksum,
  //        const OatDexFile* oat_dex_file,
  //        std::unique_ptr<DexFileContainer> container,
  //        bool is_compact_dex);
  // Top-level initializer that calls other Init methods.
  bool Init(std::string* error_msg);

  // Returns true if the header magic and version numbers are of the expected values.
  bool CheckMagicAndVersion(std::string* error_msg) const;

  // Initialize section info for sections only found in map. Returns true on success.
  void InitializeSectionsFromMapList();

  // The base address of the memory mapping.
  const uint8_t* const begin_;

  // The size of the underlying memory allocation in bytes.
  const size_t size_;

  // The base address of the data section (same as Begin() for standard dex).
  const uint8_t* const data_begin_;

  // The size of the data section.
  const size_t data_size_;

  // Typically the dex file name when available, alternatively some identifying string.
  //
  // The ClassLinker will use this to match DexFiles the boot class
  // path to DexCache::GetLocation when loading from an image.
  const std::string location_;

  const uint32_t location_checksum_;

  // Points to the header section.
  const Header* const header_;

  // Points to the base of the string identifier list.
  const dex::StringId* const string_ids_;
  // Points to the base of the type identifier list.
  const dex::TypeId* const type_ids_;

  // Points to the base of the field identifier list.
  const dex::FieldId* const field_ids_;

  // Points to the base of the method identifier list.
  const dex::MethodId* const method_ids_;

  // Points to the base of the prototype identifier list.
  const dex::ProtoId* const proto_ids_;

  // Points to the base of the class definition list.
  const dex::ClassDef* const class_defs_;

  // Points to the base of the method handles list.
  const dex::MethodHandleItem* method_handles_;

  // Number of elements in the method handles list.
  size_t num_method_handles_;
  // Points to the base of the call sites id list.
  const dex::CallSiteIdItem* call_site_ids_;

  // Number of elements in the call sites list.
  size_t num_call_site_ids_;

  // Points to the base of the hiddenapi class data item_, or nullptr if the dex
  // file does not have one.
  const dex::HiddenapiClassData* hiddenapi_class_data_;

  // If this dex file was loaded from an oat file, oat_dex_file_ contains a
  // pointer to the OatDexFile it was loaded from. Otherwise oat_dex_file_ is
  // null.
  mutable const OatDexFile* oat_dex_file_;

  // Manages the underlying memory allocation.
  std::unique_ptr<DexFileContainer> container_;
  // If the dex file is a compact dex file. If false then the dex file is a standard dex file.
  const bool is_compact_dex_;

  // The domain this dex file belongs to for hidden API access checks.
  // It is decleared `mutable` because the domain is assigned after the DexFile
  // has been created and can be changed later by the runtime.
  mutable hiddenapi::Domain hiddenapi_domain_;

  friend class DexFileLoader;
  friend class DexFileVerifierTest;
  friend class OatWriter;
};
}
