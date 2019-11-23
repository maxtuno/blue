///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;


public class Vector<T> implements Iterable<T>, Serializable {

    private T[] data = null;

    private int sz = 0;

    Vector() {
    }

    private static int imax(int x, int y) {
        int mask = (y - x) >> (4 * 8 - 1);
        return (x & mask) + (y & ~mask);
    }

    int size() {
        return sz;
    }

    void shrink(int numberOfElements) {
        for (int i = 0; i < numberOfElements; ++i) {
            data[--sz] = null;
        }
    }

    private int capacity() {
        return data == null ? 0 : data.length;
    }


    void push(T elem) {
        if (sz == capacity()) {
            capacity(sz + 1);
        }
        data[sz++] = elem;
    }

    T last() {
        return data[size() - 1];
    }

    void pop() {
        if (sz <= 0) {
            throw new IndexOutOfBoundsException();
        }
        sz--;
    }

    T get(int index) {
        return data[index];
    }

    T set(int index, T elem) {
        data[index] = elem;
        return data[index];
    }

    void capacity(int minCap) {
        int cap = capacity();
        if (cap >= minCap) return;
        int add = imax((minCap - cap + 1) & ~1, ((cap >> 1) + 2) & ~1);
        T[] newData = (T[]) new Object[cap + add];
        if (data != null) {
            System.arraycopy(data, 0, newData, 0, sz);
        }
        data = newData;
    }

    void sort(Comparator<T> comp) {
        if (sz <= 1) return;
        Arrays.sort(data, 0, sz - 1, comp);
    }

    void remove(T t) {
        int j = 0;
        for (; j < data.length && !data[j].equals(t); j++) ;
        for (; j < data.length - 1; j++) data[j] = data[j + 1];
        pop();
    }

    void rotate(int index) {
        int i, j;

        for (j = 0; j < index; j++) {
            /* Store first element of array */
            T first = data[0];

            for (i = 0; i < sz - 1; i++) {
                /* Move each array element to its left */
                data[i] = data[i + 1];
            }

            /* Copies the first element of array to last */
            data[sz - 1] = first;
        }
    }


    void copyTo(Vector<T> copy) {
        copy.clear();
        copy.growTo(sz);
        if (sz <= 0) return;
        System.arraycopy(data, 0, copy.data, 0, sz);
    }

    void growTo(int size) {
        if (sz >= size) return;
        capacity(size);
        sz = size;
    }

    void clear() {
        if (data != null) {
            sz = 0;
        }
    }

    public boolean isEmpty() {
        return sz == 0;
    }

    public Vector<Literal> invert() {
        Vector<Literal> inv = new Vector<>();
        for (Object item : data) {
            if (item != null) {
                inv.push(((Literal) item).not());
            }
        }
        return inv;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (T item : data) {
            if (item != null) {
                out.append(String.format("%s ", item));
            }
        }
        return out.toString();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < sz;
            }

            @Override
            public T next() {
                return data[i++];
            }
        };
    }
}