///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

class Heap {

    private Order lt;
    private Vector<Integer> heap = new Vector<>();
    private Vector<Integer> indices = new Vector<>();

    Heap(Order lt) {
        this.lt = lt;
    }

    private static int left(int i) {
        return i * 2 + 1;
    }

    private static int right(int i) {
        return (i + 1) * 2;
    }

    private static int parent(int i) {
        return (i - 1) >> 1;
    }

    int size() {
        return heap.size();
    }

    boolean empty() {
        return heap.size() == 0;
    }

    boolean inHeap(int n) {
        return n < indices.size() && indices.get(n) >= 0;
    }

    private void percolateUp(int i) {
        int x = heap.get(i);
        int p = parent(i);

        while (i != 0 && lt.call(x, heap.get(p))) {
            heap.set(i, heap.get(p));
            indices.set(heap.get(p), i);
            i = p;
            p = parent(p);
        }
        heap.set(i, x);
        indices.set(x, i);
    }

    private void percolateDown(int i) {
        int x = heap.get(i);
        while (left(i) < heap.size()) {
            int child = right(i) < heap.size() && lt.call(heap.get(right(i)), heap.get(left(i))) ? right(i) : left(i);
            if (!lt.call(heap.get(child), x)) break;
            heap.set(i, heap.get(child));
            indices.set(heap.get(i), i);
            i = child;
        }
        heap.set(i, x);
        indices.set(x, i);
    }

    public int get(int index) {
        if (index >= heap.size()) {
            throw new IndexOutOfBoundsException("index");
        }
        return heap.get(index);
    }

    void decrease(int n) {
        if (!inHeap(n))
            throw new IllegalArgumentException("n");
        percolateUp(indices.get(n));
    }

    void insert(int n) {
        indices.growTo(n + 1);
        indices.set(n, heap.size());
        heap.push(n);
        percolateUp(indices.get(n));
    }

    int removeMin() {
        int x = heap.get(0);
        heap.set(0, heap.last());
        indices.set(heap.get(0), 0);
        indices.set(x, -1);
        heap.pop();
        if (heap.size() > 1) {
            percolateDown(0);
        }
        return x;
    }

    void build(Vector<Integer> ns) {
        for (int i = 0; i < heap.size(); i++) {
            indices.set(heap.get(i), -1);
        }
        heap.clear();
        for (int i = 0; i < ns.size(); i++) {
            indices.set(ns.get(i), i);
            heap.push(ns.get(i));
        }
        for (int i = heap.size() / 2 - 1; i >= 0; i--) {
            percolateDown(i);
        }
    }

    static class Order {

        private final Vector<Double> activities;

        Order(Vector<Double> activities) {
            this.activities = activities;
        }

        boolean call(int x, int y) {
            return activities.get(x) > activities.get(y);
        }
    }
}
