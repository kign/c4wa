// incremental memory allocation; nothing is ever released
static int __last_offset = __builtin_offset;
static int __available_size = -1;

void * malloc(int size) {
    if (__available_size < 0)
        __available_size = 64000 * memsize();

    __last_offset = (__last_offset - 1) / __builtin_alignment * __builtin_alignment + __builtin_alignment;
    char * res = __builtin_memory + __last_offset;

    __last_offset += size;

    if (__last_offset > __available_size) {
        int pages = 1 + __last_offset/64000;
        memgrow(pages - memsize());
        __available_size = 64000 * pages;
    }

    return res;
}

void free(void * ptr) {
}
