/*
 * S-Expression Parser in Java.
 *
 * Copyright (C) 2012 Joel F. Klein
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.interedition.text.lisp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;

/**
 * @author <a href="http://rosettacode.org/wiki/S-Expressions#Java" title="rosettacode.org">Joel F. Klein</a>
 */
public class LispParser {

    private StreamTokenizer tokenizer;

    /**
     * Constructs a tokenizer that scans input from the given string.
     *
     * @param src A string containing S-expressions.
     */
    public LispParser(String src) {
        this(new StringReader(src));
    }

    /**
     * Constructs a tokenizer that scans input from the given Reader.
     *
     * @param reader Reader for the character input source
     */
    public LispParser(Reader reader) {
        tokenizer = new StreamTokenizer(new BufferedReader(reader));
        tokenizer.resetSyntax();
        tokenizer.parseNumbers();
        tokenizer.whitespaceChars(0, ' ');
        tokenizer.wordChars(' ' + 1, 255);
        tokenizer.ordinaryChar('(');
        tokenizer.ordinaryChar(')');
        tokenizer.commentChar(';');
        tokenizer.quoteChar('"');
    }

    public Expression expression() throws LispParserException, IOException {
        tokenizer.nextToken();
        switch (tokenizer.ttype) {
            case '(':
                final ExpressionList list = new ExpressionList();
                while (true) {
                    tokenizer.nextToken();
                    if (tokenizer.ttype == ')') {
                        break;
                    }
                    if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
                        throw new LispParserException();
                    }
                    tokenizer.pushBack();
                    list.add(expression());
                }
                return list;
            case StreamTokenizer.TT_NUMBER:
                return new NumberAtom(Math.round(tokenizer.nval));
            case '"':
                return new StringAtom(tokenizer.sval);
            default:
                return new Atom(tokenizer.sval);
        }
    }
}