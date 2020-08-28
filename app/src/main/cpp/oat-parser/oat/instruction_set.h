#include <iosfwd>
#include <string>

namespace Art{
enum class InstructionSet {
  kNone,
  kArm,
  kArm64,
  kThumb2,
  kX86,
  kX86_64,
  kMips,
  kMips64,
  kLast = kMips64
};
//std::ostream& operator<<(std::ostream& os, const InstructionSet& rhs);
//template<typename T> std::ostream& operator<<(typename std::enable_if<std::is_enum<T>::value, std::ostream>::type& stream, const T& e) {
//    return stream << static_cast<typename std::underlying_type<T>::type>(e);
//}


#if defined(__arm__)
static constexpr InstructionSet kRuntimeISA = InstructionSet::kArm;
#elif defined(__aarch64__)
static constexpr InstructionSet kRuntimeISA = InstructionSet::kArm64;
#elif defined(__mips__) && !defined(__LP64__)
static constexpr InstructionSet kRuntimeISA = InstructionSet::kMips;
#elif defined(__mips__) && defined(__LP64__)
static constexpr InstructionSet kRuntimeISA = InstructionSet::kMips64;
#elif defined(__i386__)
static constexpr InstructionSet kRuntimeISA = InstructionSet::kX86;
#elif defined(__x86_64__)
static constexpr InstructionSet kRuntimeISA = InstructionSet::kX86_64;
#else
static constexpr InstructionSet kRuntimeISA = InstructionSet::kNone;
#endif

//static constexpr PointerSize kArmPointerSize = PointerSize::k32;
//static constexpr PointerSize kArm64PointerSize = PointerSize::k64;
//static constexpr PointerSize kMipsPointerSize = PointerSize::k32;
//static constexpr PointerSize kMips64PointerSize = PointerSize::k64;
//static constexpr PointerSize kX86PointerSize = PointerSize::k32;
//static constexpr PointerSize kX86_64PointerSize = PointerSize::k64;

}
