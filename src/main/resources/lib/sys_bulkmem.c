void memcpy(void * dst, void * src, int count) {
    if (count <= 0) return;
    do {
        count --;
        ((char *)dst)[count] = ((char *)src)[count];
    } while(count);
}

void memset(void * ptr, char c, int count) {
    if (count <= 0) return;
    do {
        count --;
        ((char *)ptr)[count] = c;
    } while(count);
}