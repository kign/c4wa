void printf();

const int N = 50;
const char * sep = ", ";

int cycle(unsigned long seed) {
    int len = 0;
    do {
        if (seed == 1)
            return len;
        else if (seed % 2 == 0)
            seed /= 2;
        else
            seed = 3 * seed + 1;
        len ++;
    }
    while(1);
}

extern int main () {
    int printed = 0;
    int max = -1;
    int start = 1;
    for (int i = 1; printed<N; i ++) {
        int v = cycle((unsigned long)i);
        if (v <= max) continue;
        printf("%s%d->%d", start?"":sep, i, v);
        max = v;
        printed ++;
        start = 0;
        // Using bitwise AND on purpose
        if (printed < N & printed % 10 == 0) {
            printf("\n");
            start = 1;
        }
    }
    printf("\n");
    return 0;
}
// 1->0, 2->1, 3->7, 6->8, 7->16, 9->19, 18->20, 25->23, 27->111, 54->112
// 73->115, 97->118, 129->121, 171->124, 231->127, 313->130, 327->143, 649->144, 703->170, 871->178
// 1161->181, 2223->182, 2463->208, 2919->216, 3711->237, 6171->261, 10971->267, 13255->275, 17647->278, 23529->281
// 26623->307, 34239->310, 35655->323, 52527->339, 77031->350, 106239->353, 142587->374, 156159->382, 216367->385, 230631->442
// 410011->448, 511935->469, 626331->508, 837799->524, 1117065->527, 1501353->530, 1723519->556, 2298025->559, 3064033->562, 3542887->583
