void printf(char *, ...);

extern int main () {
    int count = 0;
    for(int x = 0; x < 5; x ++)
        for(int y = 0; y < 5; y ++)
            count ++;

    printf("Count = %d\n", count);

    return 0;
}
// Count = 25