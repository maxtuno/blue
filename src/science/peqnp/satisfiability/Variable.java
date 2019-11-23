///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

import java.util.ArrayList;
import java.util.List;

class Variable {

    private static final int UNDEF_VALUE = -1;
    static final Variable UNDEF = new Variable(UNDEF_VALUE);
    private static final List<Variable> cache = new ArrayList<>();
    private int value;

    private Variable(int value) {
        this.value = value;
    }

    static Variable valueOf(int value) {
        if (value == UNDEF_VALUE) return UNDEF;

        for (int i = cache.size(); i <= value; ++i)
            cache.add(new Variable(i));

        return cache.get(value);
    }

    int value() {
        return value;
    }
}
