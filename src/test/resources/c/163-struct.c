void printf();

struct Node { struct Node * next; int val; };
struct LinkedList { struct Node * head, * tail; };

static int node_idx = 0;

struct Node * new_node() {
    struct Node * node = alloc(node_idx * sizeof(struct Node), 1, struct Node);
    node_idx ++;
    node->next = (struct Node *) 0;

    return node;
}

void init_linked_list(struct LinkedList * list) {
    list->head = (struct Node *) 0;
    list->tail = (struct Node *) 0;
}

void pushTail(struct LinkedList * list, int val) {
    if (!list->head) {
        list->head = new_node ();
        list->tail = list->head;
    }
    else {
        list->tail->next = new_node ();
        list->tail = list->tail->next;
    }
    list->tail->val = val;
}

void pushHead(struct LinkedList * list, int val) {
    if (!list->head) {
        list->head = new_node ();
        list->tail = list->head;
    }
    else {
        struct Node * head = new_node ();
        head->next = list->head;
        list->head = head;
    }
    list->head->val = val;
}

int popTail(struct LinkedList * list) {
    int res = list->tail? list->tail->val : 0;

    if (list->tail && list->tail == list->head) {
        list->head = (struct Node *) 0;
        list->tail = (struct Node *) 0;
    }
    else if (list->tail) {
        struct Node * v;
        for (v = list->head; v->next != list->tail; v = v->next);
        v->next = (struct Node *) 0;
        list->tail = v;
    }
    else
        printf("ERROR: List is already empty\n");

    return res;
}

void print_list(struct LinkedList * list) {
    int i = 0;
    for (struct Node * v = list->head; v; i ++, v = v->next) {
        if (i > 0)
            printf(", ");
        printf("%d", v->val);
    }

    if (i == 0)
        printf("<empty list>");

    printf("\n");
}

extern int main () {
    int i;
    struct LinkedList linkedList;
    init_linked_list(&linkedList);

    print_list(&linkedList);
    pushTail(&linkedList, 57);
    print_list(&linkedList);


    pushTail(&linkedList, -19);
    print_list(&linkedList);

    popTail(&linkedList);
    print_list(&linkedList);

    popTail(&linkedList);
    print_list(&linkedList);

    popTail(&linkedList);
    print_list(&linkedList);

    for(i = 1; i <= 20; i ++)
        pushTail(&linkedList, i);

    print_list(&linkedList);

    init_linked_list(&linkedList);

    for(i = 1; i <= 20; i ++)
        pushHead(&linkedList, i);

    print_list(&linkedList);

    return 0;
}
// <empty list>
// 57
// 57, -19
// 57
// <empty list>
// ERROR: List is already empty
// <empty list>
// 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20
// 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1