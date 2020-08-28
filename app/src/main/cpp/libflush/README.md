# libflush

libflush is a library to launch cache attacks on ARMv8 architecture.  

use this command to complie it as a .so.
```bash
ndk-build NDK_APPLICATION_MK=`pwd`/Application.mk NDK_PROJECT_PATH=`pwd`
``` 

This is a edited version, please refer to its [original git](https://github.com/IAIK/armageddon.git) for more information.
## License

[Licensed](LICENSE) under the zlib license.

## References

* [1] [ARMageddon: Cache Attacks on Mobile Devices - Lipp, Gruss, Spreitzer, Maurice, Mangard](https://www.usenix.org/conference/usenixsecurity16/technical-sessions/presentation/lipp)
* [2] [Prefetch Side-Channel Attacks: Bypassing SMAP and Kernel ASLR - Gruss, Fogh, Maurice, Lipp, Mangard]()
