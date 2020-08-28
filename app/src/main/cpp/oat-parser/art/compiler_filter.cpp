#include "compiler_filter.h"
#include <string.h>

namespace Art {

std::string CompilerFilter::NameOfFilter(Filter filter) {
  switch (filter) {
    case CompilerFilter::kVerifyNone: return "verify-none";
    case CompilerFilter::kVerifyAtRuntime: return "verify-at-runtime";
    case CompilerFilter::kVerifyProfile: return "verify-profile";
    case CompilerFilter::kInterpretOnly: return "interpret-only";
    case CompilerFilter::kSpaceProfile: return "space-profile";
    case CompilerFilter::kSpace: return "space";
    case CompilerFilter::kBalanced: return "balanced";
    case CompilerFilter::kTime: return "time";
    case CompilerFilter::kSpeedProfile: return "speed-profile";
    case CompilerFilter::kSpeed: return "speed";
    case CompilerFilter::kEverythingProfile: return "everything-profile";
    case CompilerFilter::kEverything: return "everything";
  }
 return 0;
}

bool CompilerFilter::ParseCompilerFilter(const char* option, Filter* filter) {
  if (filter == nullptr) return false;

  if (strcmp(option, "verify-none") == 0) {
    *filter = kVerifyNone;
  } else if (strcmp(option, "interpret-only") == 0) {
    *filter = kInterpretOnly;
  } else if (strcmp(option, "verify-profile") == 0) {
    *filter = kVerifyProfile;
  } else if (strcmp(option, "verify-at-runtime") == 0) {
    *filter = kVerifyAtRuntime;
  } else if (strcmp(option, "space") == 0) {
    *filter = kSpace;
  } else if (strcmp(option, "space-profile") == 0) {
    *filter = kSpaceProfile;
  } else if (strcmp(option, "balanced") == 0) {
    *filter = kBalanced;
  } else if (strcmp(option, "speed") == 0) {
    *filter = kSpeed;
  } else if (strcmp(option, "speed-profile") == 0) {
    *filter = kSpeedProfile;
  } else if (strcmp(option, "everything") == 0) {
    *filter = kEverything;
  } else if (strcmp(option, "everything-profile") == 0) {
    *filter = kEverythingProfile;
  } else if (strcmp(option, "time") == 0) {
    *filter = kTime;
  } else {
    return false;
  }
  return true;
}

std::ostream& operator<<(std::ostream& os, const CompilerFilter::Filter& rhs) {
  return os << CompilerFilter::NameOfFilter(rhs);
}

}  // namespace art

