void printf(char *, ...);

int strlen(char * str) {
    int n = 0;
    do {
        str ++;
        n ++;
    }
    while(*str);
    return n;
}

extern int main () {
    char * hello = "Hello!";
    int len = 57;
    printf("String <%s> consists of %d characters: ", hello, strlen(hello));
    do {
        printf("'%c'", *hello);
        hello ++;
        if (*hello != '\0')
            printf(", ");
    }
    while(*hello != '\0');
    printf(".\n");
    return 0;
}
// String <Hello!> consists of 6 characters: 'H', 'e', 'l', 'l', 'o', '!'.