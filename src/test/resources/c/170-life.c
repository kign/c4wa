void printf();

struct Stat {unsigned int hash; int count; };
#define cell_t char

void read(int X, int Y, char * pos, char * init) {
    for(int i = 0; i < X * Y; i ++)
        pos[i] = init[i] == 'x' ? (char)1 : (char)0;
}

void print(int X, int Y, char * pos, int dbg) {
    for(int y = 0; y < Y; y ++) {
        for(int x = 0; x < X; x ++) {
            int val = (int) pos[X * y + x];
            printf(val == 1? "x" : (((val == 2) & dbg)?"2":"."));
        }
        printf("\n");
    }
}

const unsigned int hash_rand = 179424673;

void life_prepare (
   cell_t               * cells,
   int                  X,
   int                  Y,
   struct Stat          * stat ) {
    int cnt = 0;
    unsigned int hash = 0;

    for(int y = 0; y < Y; y ++)
        for(int x = 0; x < X; x ++) {
            int idx = X * y + x;
            if (cells[idx] == 1) {
                cnt ++;
                hash ^= (unsigned int)idx * hash_rand;
                for (int dx = -1; dx <= 1; dx ++)
                    for (int dy = -1; dy <= 1; dy ++) {
                        int didx = X * ((y + dy + Y) % Y) + ((x + dx + X) % X);
                        if (cells[didx] == 0)
                            cells[didx] = 2;
                    }
            }
        }
    stat->count = cnt;
    stat->hash = hash;
}

void life_step (
    cell_t                 * cells,
    cell_t                 * cellsnew,
    int                  X,
    int                  Y,
    struct Stat          * stat
) {
    int                  ind, x, y,  n, newv;
    int                  n00, n01, n02, n10, n12, n20, n21, n22;
    int                  v00, v01, v02, v10, v11, v12, v20, v21, v22;
    int                  cnt = 0;
    unsigned int         hash = 0;
    char                 * p = cells - 1;

    memset ( cellsnew, (char)0, X * Y );

    /* Assuming there could be many empty cells, optimize looping the
     * best we can */

    do {
        do {
            p ++;
        }
        while (*p == (char)0);

        if (*p == 3)
            break;

        //assert ( *p == 1 || *p == 2 );

        ind = p - cells;

        y = ind / X; x = ind - y * X;

        if ( x > 0 & x < X - 1 & y > 0 & y < Y - 1 ) {
            n00 = X * (y - 1) + (x - 1);
            n01 = n00 + 1;
            n02 = n01 + 1;
            n10 = ind - 1;
            n12 = ind + 1;
            n20 = n10 + X;
            n21 = n20 + 1;
            n22 = n21 + 1;
        }
        else {
#define N(dy,dx)  (X * ((y + dy + Y) % Y) + ((x + dx + X) % X))
            n00 = N(-1,-1);
            n01 = N(-1,0);
            n02 = N(-1,1);
            n10 = N(0,-1);
            n12 = N(0,1);
            n20 = N(1,-1);
            n21 = N(1,0);
            n22 = N(1,1);
#undef N
        }
        v00 = (1 == cells[n00]);
        v01 = (1 == cells[n01]);
        v02 = (1 == cells[n02]);
        v10 = (1 == cells[n10]);
        v11 = (1 == *p);
        v12 = (1 == cells[n12]);
        v20 = (1 == cells[n20]);
        v21 = (1 == cells[n21]);
        v22 = (1 == cells[n22]);

        n = v00 + v01 + v02 + v10 + v12 + v20 + v21 + v22;

        newv = (n == 3) | ((n == 2) & v11);

        if ( newv ) {
            cnt ++;

            hash ^= (unsigned int)ind * hash_rand;

            cellsnew[ind] = (char)newv;
            if (cellsnew[n00] != 1) cellsnew[n00] = 2;
            if (cellsnew[n01] != 1) cellsnew[n01] = 2;
            if (cellsnew[n02] != 1) cellsnew[n02] = 2;
            if (cellsnew[n10] != 1) cellsnew[n10] = 2;
            if (cellsnew[n12] != 1) cellsnew[n12] = 2;
            if (cellsnew[n20] != 1) cellsnew[n20] = 2;
            if (cellsnew[n21] != 1) cellsnew[n21] = 2;
            if (cellsnew[n22] != 1) cellsnew[n22] = 2;
        }
    }
    while(1);

    stat->count = cnt;
    stat->hash = hash;
}

const int N = 100;
const int M = sizeof(cell_t);

extern int main () {
    int X = 10;
    int Y = 10;

    char * initial_pos = ".........."
                         "......x..."
                         "....xxx..."
                         ".....x...."
                         ".........."
                         ".........."
                         ".........."
                         ".........."
                         ".........."
                         "..........";

    char * pos_0 = alloc(0, X*Y, cell_t);
    pos_0[X * Y] = 3;
    char * pos_1 = alloc(M*(X*Y+1), X*Y, cell_t);
    pos_1[X * Y] = 3;
    struct Stat * stat = alloc(M*(2*X*Y + 2), 1, struct Stat);

    read(X, Y, pos_0, initial_pos);
    life_prepare(pos_0, X, Y, stat);

    for(int iter = 0; iter < N; iter ++) {
        if (iter % 2 == 0)
            life_step(pos_0, pos_1, X, Y, stat);
        else
            life_step(pos_1, pos_0, X, Y, stat);
    }

    print(X, Y, N % 2 == 0? pos_0 : pos_1, 0);

    return 0;
}
// ..........
// ..x.......
// .x.x......
// .x..x.x...
// .xxxxx.x..
// .xx..xx...
// ....x.....
// ....x.....
// ..........
// ..........
