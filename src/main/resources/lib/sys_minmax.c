// "__max_32s", "__min_32s", "__max_32u", "__min_32u", "__max_64s", "__min_64s", "__max_64u", "__min_64u

int __max_32s(int a, int b) { return a>b?a:b; }
int __min_32s(int a, int b) { return a>b?b:a; }
unsigned int __max_32u(unsigned int a, unsigned int b) { return a>b?a:b; }
unsigned int __min_32u(unsigned int a, unsigned int b) { return a>b?b:a; }
long __max_64s(long a, long b) { return a>b?a:b; }
long __min_64s(long a, long b) { return a>b?b:a; }
unsigned long __max_64u(unsigned long a, unsigned long b) { return a>b?a:b; }
unsigned long __min_64u(unsigned long a, unsigned long b) { return a>b?b:a; }
