void printf();

extern int main () {
    int increase_by = 57;
    int initial_size = memsize ();

    memgrow(increase_by);

    printf("Tried to grow memory by %d pages, in fact it grew by ... %d pages\n", increase_by, memsize() - initial_size);

    return 0;
}
// Tried to grow memory by 57 pages, in fact it grew by ... 57 pages