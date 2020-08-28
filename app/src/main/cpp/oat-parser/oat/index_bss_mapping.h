/*
 * Copyright (C) 2017 The Android Open Source Project
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

#ifndef ART_RUNTIME_INDEX_BSS_MAPPING_H_
#define ART_RUNTIME_INDEX_BSS_MAPPING_H_

namespace Art {

template<typename T> class LengthPrefixedArray;
struct IndexBssMappingEntry {
  uint32_t index_and_mask;
  uint32_t bss_offset;
};

using IndexBssMapping = LengthPrefixedArray<IndexBssMappingEntry>;

class IndexBssMappingLookup {
 public:
  static constexpr size_t npos = static_cast<size_t>(-1);

  static size_t GetBssOffset(const IndexBssMapping* mapping,
                             uint32_t index,
                             uint32_t number_of_indexes,
                             size_t slot_size);
};

}  // namespace art

#endif  // ART_RUNTIME_INDEX_BSS_MAPPING_H_
