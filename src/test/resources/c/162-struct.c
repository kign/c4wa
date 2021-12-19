void printf(char *, ...);

struct Point { char color[10]; float x, y; float z; };

int strlen(char * str) {
    int n = 0;
    do {
        str ++;
        n ++;
    }
    while(*str != '\0');
    return n;
}

void init_point(struct Point * p, char * color, double x, double y, double z) {
    memcpy(p->color, color, 1 + strlen(color));

    p->x = (float)x;
    p->y = (float)y;
    p->z = (float)z;
}

void print_point(struct Point * p) {
    printf("Point %s: [%.6f, %.6f, %.6f]\n", p->color, p->x, p->y, p->z);
}

extern int main () {
    struct Point a;

    a.x = 1.0;
    a.color[0] = 'a';
    a.color[1] = 'b';

    init_point(&a, "green", -3.5, 8.6, 4.2);
    print_point(&a);

    return 0;
}
// Point green: [-3.500000, 8.600000, 4.200000]