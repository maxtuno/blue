///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

import java.util.ArrayList;

class Clause extends ArrayList<Literal> {

    private byte mark;
    private boolean learnt;
    private double act;

    Clause(Vector<Literal> ps, boolean learnt) {
        this.learnt = learnt;
        for (int i = 0; i < ps.size(); i++) {
            add(ps.get(i));
        }
    }

    boolean learnt() {
        return learnt;
    }

    int mark() {
        return mark;
    }

    void mark(int m) {
        mark = (byte) m;
    }

    double activity() {
        return act;
    }

    double activity(double value) {
        return act = value;
    }

}
