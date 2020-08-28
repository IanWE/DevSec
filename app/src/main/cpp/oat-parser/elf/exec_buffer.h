struct ExecBuffer {
    ExecBuffer() {
    }

    unsigned char * ptr;
    DWORD size;
    unsigned char * cur;

    bool Init(DWORD size, LPVOID addr = 0) {
        cur = ptr = (unsigned char *) VirtualAlloc(addr, size, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
        if (!ptr) {
            PrintError();
            return false;
        }
        this->size = size;
        return true;
    }

    unsigned char * Ptr() {
        return ptr;
    }

    unsigned char * &Cur() {
        return cur;
    }

    ExecBuffer &PushByte(DWORD b) {
        return Push((BYTE) b);
    }

    ExecBuffer &Push(BYTE b) {
        *Cur()++ = b;
        return *this;
    }

    ExecBuffer &Push(void *pb, DWORD size) {
        return Push((BYTE *) pb, size);
    }

    ExecBuffer &Push(BYTE *pb, DWORD size) {
        while (size--)
            *Cur()++ = *pb++;
        return *this;
    }

    unsigned char * Align4() {
        while ((DWORD) Cur() & 0x3)*Cur()++ = 0x90;
        return Cur();
    }

    ExecBuffer _Jmp(DWORD tgt) {
        DWORD pc = (DWORD) Cur() + 5;
        DWORD offset = tgt - pc;
        Push(0xE9).Push((unsigned char *) &offset, 4);
        return *this;
    }

    ExecBuffer _Call(DWORD tgt) {
        DWORD pc = (DWORD) Cur() + 5;
        DWORD offset = tgt - pc;
        Push(0xE8).Push((unsigned char *) &offset, 4);
        return *this;
    }

    ExecBuffer _Push(DWORD val) {
        Push(0x68).Push((unsigned char *) &val, 4);
        return *this;
    }

    void Flush() {
        FlushInstructionCache(GetCurrentProcess(), (unsigned char *) Ptr(), size);
    }
};