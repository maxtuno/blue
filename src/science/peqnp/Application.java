///////////////////////////////////////////////////////////////////////////////
//        Copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                       oscar.riveros@peqnp.science                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp;

import science.peqnp.satisfiability.Solver;

public class Application {

    public static void main(String[] args) {
        System.out.println("c                  ");
        System.out.println("c  ╔╗ ╦  ╦ ╦╔═  ╦  ");
        System.out.println("c  ╠╩╗║  ║ ║║╣  ║  ");
        System.out.println("c  ╚═╝╩═╝╚═╝╚═╝ ╩  ");
        System.out.println("c www.peqnp.science");
        System.out.println("c                  ");

        Solver solver = new Solver();
        solver.solve(args[0]);
    }
}
