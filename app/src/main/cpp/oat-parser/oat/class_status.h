
namespace Art {
enum class ClassStatus : uint8_t {
  kNotReady = 0,  // Zero-initialized Class object starts in this state.
  kRetired = 1,  // Retired, should not be used. Use the newly cloned one instead.
  kErrorResolved = 2,
  kErrorUnresolved = 3,
  kIdx = 4,  // Loaded, DEX idx in super_class_type_idx_ and interfaces_type_idx_.
  kLoaded = 5,  // DEX idx values resolved.
  kResolving = 6,  // Just cloned from temporary class object.
  kResolved = 7,  // Part of linking.
  kVerifying = 8,  // In the process of being verified.
  kRetryVerificationAtRuntime = 9,  // Compile time verification failed, retry at runtime.
  kVerifyingAtRuntime = 10,  // Retrying verification at runtime.
  kVerified = 11,  // Logically part of linking; done pre-init.
  kSuperclassValidated = 12,  // Superclass validation part of init done.
  kInitializing = 13,  // Class init in progress.
  kInitialized = 14,  // Ready to go.
  kLast = static_cast<int>(kInitialized)
};
std::ostream& operator<<(std::ostream& os, const ClassStatus& rhs);
}
