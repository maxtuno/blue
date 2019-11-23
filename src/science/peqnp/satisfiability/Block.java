///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

class Block {
    Clause reason;
    int level;

    private Block(Clause reason, int level) {
        this.reason = reason;
        this.level = level;
    }

    static Block makeBlock(Clause reason, int level) {
        return new Block(reason, level);
    }
}
