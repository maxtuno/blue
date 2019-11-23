///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

class Watchers {

    private Vector<Vector<Watcher>> watchers = new Vector<>();

    private Vector<Integer> dirty = new Vector<>();

    private Vector<Literal> dirties = new Vector<>();

    void init(Literal idx) {
        int size = idx.value() + 1;
        for (int i = watchers.size(); i < size; ++i) {
            watchers.push(new Vector<>());
        }
        dirty.growTo(size);
    }

    Vector<Watcher> get(Literal idx) {
        return watchers.get(idx.value());
    }

    void smudge(Literal literal) {
        if (dirty.get(literal.value()) == 0) {
            dirty.set(literal.value(), 1);
            dirties.push(literal);
        }
    }

    void cleanAll() {
        for (int i = 0; i < dirties.size(); i++) {
            if (dirty.get(dirties.get(i).value()) != 0) {
                clean(dirties.get(i));
            }
        }
        dirties.clear();
    }

    private boolean deleted(Watcher w) {
        return w.clause.mark() == 1;
    }

    private void clean(Literal idx) {
        Vector<Watcher> vector = watchers.get(idx.value());
        int i, j;
        for (i = j = 0; i < vector.size(); i++) {
            if (!deleted(vector.get(i))) {
                vector.set(j++, vector.get(i));
            }
        }
        vector.shrink(i - j);
        dirty.set(idx.value(), 0);
    }
}
