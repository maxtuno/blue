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

public class Literal {
    private static final int UNDEF_VALUE = -2;
    static final Literal UNDEF = new Literal(UNDEF_VALUE);
    private static final int ERROR_VALUE = -1;
    private static final Literal ERROR = new Literal(ERROR_VALUE);
    private static final List<Literal> cache = new ArrayList<>();
    private int literal;

    public Literal(int literal) {
        this.literal = literal;
    }

    public static Literal valueOf(int literal) {
        switch (literal) {
            case UNDEF_VALUE:
                return UNDEF;
            case ERROR_VALUE:
                return ERROR;
        }

        for (int i = cache.size(); i <= literal; i++) {
            cache.add(new Literal(i));
        }

        return cache.get(literal);
    }

    public static Literal valueOf(int var, boolean sign) {
        return valueOf(var + var + (sign ? 1 : 0));
    }

    public static Literal fromInt(int value) {
        int var = Math.abs(value) - 1;
        return Literal.valueOf(var, value < 0);
    }

    public Literal not() {
        return valueOf(literal ^ 1);
    }

    public boolean sign() {
        return (literal & 1) == 1;
    }

    public int variable() {
        return literal >> 1;
    }

    public int value() {
        return literal;
    }

    @Override
    public String toString() {
        return String.format("%d", sign() ? +(variable() + 1) : -(variable() + 1));
    }
}