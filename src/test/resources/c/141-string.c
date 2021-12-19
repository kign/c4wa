void printf(char *, ...);

extern int main() {
    char * test = "Hello!\n";
    printf("Comparison: %s\n", test[6] == '\n'? "\"OK\"" : "\"FAILED\"");

    return 0;
}
// Comparison: "OK"