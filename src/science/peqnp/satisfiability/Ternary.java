///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

public enum Ternary {
    TRUE, FALSE, UNDEF;

    private static final Ternary[][] XOR = {
            {FALSE, TRUE},
            {TRUE, FALSE},
            {UNDEF, UNDEF},
    };

    public static Ternary valueOf(boolean extendedBoolean) {
        return extendedBoolean ? TRUE : FALSE;
    }

    public Ternary xor(boolean extendedBoolean) {
        return XOR[this.ordinal()][extendedBoolean ? 0 : 1];
    }
}