#ifndef ART_COMPILER_FILTER_H_
#define ART_COMPILER_FILTER_H_

#include <ostream>
#include <string>
#include <vector>

namespace Art {

class CompilerFilter {
 public:
  // Note: Order here matters. Later filter choices are considered "as good
  // as" earlier filter choices.
  enum Filter {
    kVerifyNone,          // Skip verification but mark all classes as verified anyway.
    kVerifyAtRuntime,     // Delay verication to runtime, do not compile anything.
    kVerifyProfile,       // Verify only the classes in the profile, compile only JNI stubs.
    kInterpretOnly,       // Verify everything, compile only JNI stubs.
    kTime,                // Compile methods, but minimize compilation time.
    kSpaceProfile,        // Maximize space savings based on profile.
    kSpace,               // Maximize space savings.
    kBalanced,            // Good performance return on compilation investment.
    kSpeedProfile,        // Maximize runtime performance based on profile.
    kSpeed,               // Maximize runtime performance.
    kEverythingProfile,   // Compile everything capable of being compiled based on profile.
    kEverything,          // Compile everything capable of being compiled.
  };

  // Return the flag name of the given filter.
  // For example: given kVerifyAtRuntime, returns "verify-at-runtime".
  // The name returned corresponds to the name accepted by
  // ParseCompilerFilter.
  static std::string NameOfFilter(Filter filter);

  // Parse the compiler filter from the given name.
  // Returns true and sets filter to the parsed value if name refers to a
  // valid filter. Returns false if no filter matches that name.
  // 'filter' must be non-null.
  static bool ParseCompilerFilter(const char* name, /*out*/Filter* filter);
};

std::ostream& operator<<(std::ostream& os, const CompilerFilter::Filter& rhs);

}  // namespace Art

#endif 

