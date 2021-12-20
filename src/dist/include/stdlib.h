#ifdef C4WA
extern char * malloc(int size);
extern void free(char * ptr);

#if defined(C4WA_MM_INCR)
// no additional API here

#elif defined(C4WA_MM_FIXED)
extern void mm_stat(int * allocated, int * freed, int * current, int * in_use, int * capacity);
extern void mm_init(int extra_offset, int size);

#elif defined(C4WA_MM_UNI)
extern void mm_init(int mm_min);
extern char * mm_print_units();
extern int * mm_histogram(int * p_count);

#else
_ERROR
#error "One of C4WA_MM_INCR, C4WA_MM_FIXED, C4WA_MM_UNI must be defined"

#endif
#endif
