

int fake_dlclose(void *handle);

/* flags are ignored */

void *fake_dlopen(char const* libpath, int flags);

void *fake_dlsym(void *handle, char const*name, size_t& offset);
