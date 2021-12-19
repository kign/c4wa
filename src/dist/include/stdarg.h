#ifdef C4WA
#define va_list char*
#define va_start(__va_list, __ignore) __va_list=__offset-8
#define va_arg(__va_list, type) __va_list+=8,*(type *)__va_list
#define va_end(__va_list)
#endif
