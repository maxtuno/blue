///////////////////////////////////////////////////////////////////////////////
//        copyright (c) 2012-2019 Oscar Riveros. all rights reserved.        //
//                           oscar.riveros@peqnp.com                         //
//                                                                           //
//   without any restriction, Oscar Riveros reserved rights, patents and     //
//  commercialization of this knowledge or derived directly from this work.  //
///////////////////////////////////////////////////////////////////////////////

package science.peqnp.satisfiability;

import java.io.IOException;
import java.io.Reader;

class Parser {

    private static final int EOF = -1;

    private Reader in;
    private int ch;

    Parser(Reader in) throws IOException {
        this.in = in;
        this.ch = in.read();
    }

    String read() throws IOException {
        if (ch == EOF) return null;
        while (Character.isWhitespace(ch))
            ch = in.read();
        if (ch == EOF) return null;
        StringBuilder sb = new StringBuilder();
        do {
            sb.append((char) ch);
            ch = in.read();
        } while (ch != EOF && !Character.isWhitespace(ch));
        return sb.toString();
    }

    int readInt() throws IOException {
        return Integer.parseInt(read());
    }

    void skipLine() throws IOException {
        while (ch != EOF && ch != '\r' && ch != '\n')
            ch = in.read();
        int first = ch;
        ch = in.read();
        if (first == '\r' && ch == '\n')
            ch = in.read();
    }
}
