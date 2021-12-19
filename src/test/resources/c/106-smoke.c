void printf(char *, ...);

void do_nothing (int ignore_me) {
}

extern int main () {
    char a = 'a';
    char Z = '\x5a';
    char m = '\x6D';
    char _0 = '\060';
    char cr = '\n';
    printf("a = %c, %d; Z = %c, %d; m = %c, %d; _0 = %c, %d;%c", a, a, Z, Z, m, m, _0, _0, cr);
    return 0;
}
// a = a, 97; Z = Z, 90; m = m, 109; _0 = 0, 48;