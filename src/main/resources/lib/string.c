// Some C string functions (incomplete)
int strlen(char * str) {
    int n = 0;
    do {
        str ++;
        n ++;
    }
    while(*str);
    return n;
}
