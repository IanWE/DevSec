#pragma once

#include <string.h>
#include <string>
#include <algorithm>

namespace Art {
    class StringPiece {
    public:
        // standard STL container boilerplate
        typedef char value_type;
        typedef const char *pointer;
        typedef const char &reference;
        typedef const char &const_reference;
        typedef size_t size_type;
//        typedef ptrdiff_t difference_type;
        static const size_type npos = size_type(-1);
        typedef const char *const_iterator;
        typedef const char *iterator;
        typedef std::reverse_iterator<const_iterator> const_reverse_iterator;
        typedef std::reverse_iterator<iterator> reverse_iterator;

        // We provide non-explicit singleton constructors so users can pass
        // in a "const char*" or a "string" wherever a "StringPiece" is
        // expected.
        StringPiece() : ptr_(nullptr), length_(0) { }

        StringPiece(const char *str)  // NOLINT implicit constructor desired
                : ptr_(str), length_((str == nullptr) ? 0 : strlen(str)) { }

        StringPiece(const std::string &str)  // NOLINT implicit constructor desired
                : ptr_(str.data()), length_(str.size()) { }

        StringPiece(const char *offset, size_t len) : ptr_(offset), length_(len) { }

        // data() may return a pointer to a buffer with embedded NULs, and the
        // returned buffer may or may not be null terminated.  Therefore it is
        // typically a mistake to pass data() to a routine that expects a NUL
        // terminated string.
        const char *data() const { return ptr_; }

        size_type size() const { return length_; }

        size_type length() const { return length_; }

        bool empty() const { return length_ == 0; }

        void clear() {
            ptr_ = nullptr;
            length_ = 0;
        }

        void set(const char *data_in, size_type len) {
            ptr_ = data_in;
            length_ = len;
        }

        void set(const char *str) {
            ptr_ = str;
            if (str != nullptr) {
                length_ = strlen(str);
            }
            else {
                length_ = 0;
            }
        }

        void set(const void *data_in, size_type len) {
            ptr_ = reinterpret_cast<const char *>(data_in);
            length_ = len;
        }

        char operator[](size_type i) const {
            return ptr_[i];
        }

        void remove_prefix(size_type n) {
            ptr_ += n;
            length_ -= n;
        }

        void remove_suffix(size_type n) {
            length_ -= n;
        }

        int compare(const StringPiece &x) const;

        std::string as_string() const {
            return std::string(data(), size());
        }

        // We also define ToString() here, since many other string-like
        // interfaces name the routine that converts to a C++ string
        // "ToString", and it's confusing to have the method that does that
        // for a StringPiece be called "as_string()".  We also leave the
        // "as_string()" method defined here for existing code.
        std::string ToString() const {
            return std::string(data(), size());
        }

        void CopyToString(std::string *target) const;

        void AppendToString(std::string *target) const;

        // Does "this" start with "x"
        bool starts_with(const StringPiece &x) const {
            return ((length_ >= x.length_) &&
                    (memcmp(ptr_, x.ptr_, x.length_) == 0));
        }

        // Does "this" end with "x"
        bool ends_with(const StringPiece &x) const {
            return ((length_ >= x.length_) &&
                    (memcmp(ptr_ + (length_ - x.length_), x.ptr_, x.length_) == 0));
        }

        iterator begin() const { return ptr_; }

        iterator end() const { return ptr_ + length_; }

        const_reverse_iterator rbegin() const {
            return const_reverse_iterator(ptr_ + length_);
        }

        const_reverse_iterator rend() const {
            return const_reverse_iterator(ptr_);
        }

        size_type copy(char *buf, size_type n, size_type pos = 0) const;

        size_type find(const StringPiece &s, size_type pos = 0) const;

        size_type find(char c, size_type pos = 0) const;

        size_type rfind(const StringPiece &s, size_type pos = npos) const;

        size_type rfind(char c, size_type pos = npos) const;

        StringPiece substr(size_type pos, size_type n = npos) const;

    private:
        // Pointer to char data, not necessarily zero terminated.
        const char *ptr_;
        // Length of data.
        size_type length_;
    };

    // This large function is defined inline so that in a fairly common case where
    // one of the arguments is a literal, the compiler can elide a lot of the
    // following comparisons.
    inline bool operator==(const StringPiece &x, const StringPiece &y) {
        StringPiece::size_type len = x.size();
        if (len != y.size()) {
            return false;
        }

        const char *p1 = x.data();
        const char *p2 = y.data();
        if (p1 == p2) {
            return true;
        }
        if (len == 0) {
            return true;
        }

        // Test last byte in case strings share large common prefix
        if (p1[len - 1] != p2[len - 1]) return false;
        if (len == 1) return true;

        // At this point we can, but don't have to, ignore the last byte.  We use
        // this observation to fold the odd-length case into the even-length case.
        len &= ~1;

        return memcmp(p1, p2, len) == 0;
    }

    inline bool operator==(const StringPiece &x, const char *y) {
        if (y == nullptr) {
            return x.size() == 0;
        }
        else {
            return strncmp(x.data(), y, x.size()) == 0 && y[x.size()] == '\0';
        }
    }

    inline bool operator!=(const StringPiece &x, const StringPiece &y) {
        return !(x == y);
    }

    inline bool operator!=(const StringPiece &x, const char *y) {
        return !(x == y);
    }

    inline bool operator<(const StringPiece &x, const StringPiece &y) {
        const int r = memcmp(x.data(), y.data(),
                             std::min(x.size(), y.size()));
        return ((r < 0) || ((r == 0) && (x.size() < y.size())));
    }

    inline bool operator>(const StringPiece &x, const StringPiece &y) {
        return y < x;
    }

    inline bool operator<=(const StringPiece &x, const StringPiece &y) {
        return !(x > y);
    }

    inline bool operator>=(const StringPiece &x, const StringPiece &y) {
        return !(x < y);
    }

    extern std::ostream &operator<<(std::ostream &o, const StringPiece &piece);
}
