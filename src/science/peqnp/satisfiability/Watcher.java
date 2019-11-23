///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

class Watcher {
    Clause clause;
    Literal blocker;

    Watcher(Clause cr, Literal p) {
        this.clause = cr;
        this.blocker = p;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Watcher)) {
            return false;
        }
        Watcher o = (Watcher) obj;
        return clause.equals(o.clause);
    }
}
