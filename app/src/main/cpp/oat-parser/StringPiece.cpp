#include "StringPiece.h"
#include <iostream>

namespace Art {
    void StringPiece::CopyToString(std::string *target) const {
        target->assign(ptr_, length_);
    }

    void StringPiece::AppendToString(std::string *target) const {
        target->append(ptr_, length_);
    }

    StringPiece::size_type StringPiece::copy(char *buf, size_type n, size_type pos) const {
        size_type ret = std::min(length_ - pos, n);
        memcpy(buf, ptr_ + pos, ret);
        return ret;
    }

    StringPiece::size_type StringPiece::find(const StringPiece &s, size_type pos) const {
        if (length_ == 0 || pos > static_cast<size_type>(length_)) {
            return npos;
        }
        const char *result = std::search(ptr_ + pos, ptr_ + length_, s.ptr_, s.ptr_ + s.length_);
        const size_type xpos = result - ptr_;
        return xpos + s.length_ <= length_ ? xpos : npos;
    }

    int StringPiece::compare(const StringPiece &x) const {
        int r = memcmp(ptr_, x.ptr_, std::min(length_, x.length_));
        if (r == 0) {
            if (length_ < x.length_) r = -1;
            else if (length_ > x.length_) r = +1;
        }
        return r;
    }

    StringPiece::size_type StringPiece::find(char c, size_type pos) const {
        if (length_ == 0 || pos >= length_) {
            return npos;
        }
        const char *result = std::find(ptr_ + pos, ptr_ + length_, c);
        return result != ptr_ + length_ ? result - ptr_ : npos;
    }

    StringPiece::size_type StringPiece::rfind(const StringPiece &s, size_type pos) const {
        if (length_ < s.length_) return npos;
        const size_t ulen = length_;
        if (s.length_ == 0) return std::min(ulen, pos);

        const char *last = ptr_ + std::min(ulen - s.length_, pos) + s.length_;
        const char *result = std::find_end(ptr_, last, s.ptr_, s.ptr_ + s.length_);
        return result != last ? result - ptr_ : npos;
    }

    StringPiece::size_type StringPiece::rfind(char c, size_type pos) const {
        if (length_ == 0) return npos;
        for (int i = std::min(pos, static_cast<size_type>(length_ - 1));
             i >= 0; --i) {
            if (ptr_[i] == c) {
                return i;
            }
        }
        return npos;
    }

    StringPiece StringPiece::substr(size_type pos, size_type n) const {
        if (pos > static_cast<size_type>(length_)) pos = length_;
        if (n > length_ - pos) n = length_ - pos;
        return StringPiece(ptr_ + pos, n);
    }

    std::ostream &operator<<(std::ostream &o, const StringPiece &piece) {
        o.write(piece.data(), piece.size());
        return o;
    }
}