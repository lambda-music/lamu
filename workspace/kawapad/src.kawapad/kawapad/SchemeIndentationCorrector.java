/*
 * Kawapad written by Atsushi Oka 
 * Copyright 2018 Atsushi Oka
 *
 * This file is part of Metro Musical Sequencing Framework. 
 * 
 * Kawapad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Kawapad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Kawapad.  If not, see <https://www.gnu.org/licenses/>.
 */
package kawapad;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * October 3, 2018 at 9:52:28 PM
 * @author ats
 */
public class SchemeIndentationCorrector {
    static enum TokenType { BEGIN, END, ID }
    static class Token {
        TokenType type;
        String content;
        int index;
        int lineNo;
        public Token(TokenType type, String content, int index, int lineNo ) {
            super();
            this.type = type;
            this.content = content;
            this.index = index;
            this.lineNo = lineNo;
        }
        @Override
        public String toString() {
            return String.format("[%s %d \"%s\"]", type, index, content  );
        }
    }
    static class Tokenized extends ArrayList<Token> {
        void shift( int i ) {
            for ( Token t : this ) {
                t.index += i;
            }
        }
    }

    public static class Tokenizer {
        private static final int UNKNOWN = Integer.MIN_VALUE;
        int lineNo;
        int beginIndex = UNKNOWN;
        Tokenized tokenized = new Tokenized();
        StringBuilder sb = new StringBuilder();
        public Tokenizer(int lineNo) {
            this.lineNo = lineNo;
        }
        boolean addToken( TokenType type, String content, int index ) {
            return tokenized.add( new Token( type, content, index, lineNo ) );
        }
        private void appendProc( char c, int index ) {
            if ( beginIndex == UNKNOWN )
                beginIndex = index;
            sb.append(c);
        }
        void breakProc(int index) {
            if ( 0 < sb.length() ) {
                addToken( TokenType.ID, sb.toString(), beginIndex );
                sb.setLength(0);
                beginIndex = UNKNOWN;
            }
        }
    }
    
    
    public static Tokenized tokenize( String text, int lineNo ) {
        Tokenizer tokenizer = new Tokenizer( lineNo );
        for ( int i=0; i<text.length(); i++ ) {
            switch ( text.charAt(i) ) {
                case '(' :
                    tokenizer.breakProc(i);
                    tokenizer.addToken( TokenType.BEGIN, "(", i );
                    break;
                case ')' :
                    tokenizer.breakProc(i);
                    tokenizer.addToken( TokenType.END, ")", i );
                    break;
                case '\\' :
                    i++;
                    tokenizer.appendProc( text.charAt( i ), i);
                    break;
                case '\t':
                case '\n':
                case '\r':
                case ' ' :
                    tokenizer.breakProc(i);
                    break;
                default :
                    tokenizer.appendProc( text.charAt( i ), i );
                    break;
            }
        }
        tokenizer.breakProc(text.length() );
        return tokenizer.tokenized;
    }
    
    public static class Level {
        Tokenized tokenized = new Tokenized();
        void addToken( Token t  ) {
            tokenized.add( t );
        }
    }
    
    
    /**
     * 
     * @param lispWordChecker
     *    A lambda-function which returns the size of the indentation if the given word is a special keyword,
     *    Otherwise, the lambda-function should return {@link java.lang.Integer#MIN_VALUE }.  
     * @param text
     * 
     * @return
     *   TODO
     */
    public static String correctIndentation( Function<String,Integer> lispWordChecker, String text ) {
        /*
         * In this function, we always presume that ( lines.length == tokenizedLines ).
         */
        String[] lines = text.split( "\n" );
        Tokenized[] tokenizedLines = new Tokenized[ lines.length ];
        for ( int i=0; i<lines.length ;i++ ) {
            tokenizedLines[i] = tokenize( lines[i], i );
        }
        
        ArrayDeque<Level> stack = createStack();

        /*
         *  1. Process all tokenized lines. 
         */
        for ( int i=0; i<tokenizedLines.length ;i++ ) {
            Tokenized tokenized = tokenizedLines[i] ;
            
            /*
             *  2. Determine the indent size. 
             *     This block is only executed after processed the first line.
             *     See calculateIndexLevel() function. 
             */
            if ( 0<i) {
                if ( 1<stack.size() ) {
                    int index = calculateIndentLength(lispWordChecker, stack);
                    
                    if ( 0 <=index && ( /* IGNORE BLANK LINES ADDED (Tue, 09 Oct 2018 00:58:59 +0900) */ 
                                        0 < tokenizedLines[i].size() 
                                      ))
                    {
                        int diff = index - tokenizedLines[i].get(0).index;
                        for ( int j=i; j<tokenizedLines.length; j++ ) {
                            tokenizedLines[j].shift(diff);
                        }
                    }
                }
            }


            /*
             * 3. Scan all the tokens and "execute" them to generate a simplified version of
             *    the parse-tree.
             */
            semiExecute(stack, tokenized);
        }
        
        /*
         * 4. 
         * 
         */
        for ( int i=0; i<tokenizedLines.length ;i++ ) {
            Tokenized tokenized = tokenizedLines[i] ;
            if ( 0 < tokenized.size() ) {
                lines[i] = SchemeIndentChanger.changeIndentAbsolute( lines[i], tokenized.get(0).index );
            }
        }
        
        return String.join( "\n", lines ); 
    }

    public static ArrayDeque<Level> createStack() {
        ArrayDeque<Level> stack = new ArrayDeque<>();
        // Add the base level.
        stack.push( new Level() );
        return stack;
    }

    
    public static int calculateIndentLength( Function<String,Integer> lispWordChecker, ArrayDeque<Level> stack ) {
        int length = -1;
        Level level = stack.peek();
        if ( 0 == level.tokenized.size()  ) {
            // This means the number of parentheses exceeds our expectation so ignore it.
        } else if ( 1 == level.tokenized.size()  ) {
            // There is the only one parenthesis or the only one token. 
            length = level.tokenized.get(0).index + 1;
        } else if ( 2 == level.tokenized.size()  ) {
            length = level.tokenized.get(1).index;
        } else if ( 3 <= level.tokenized.size()  ) {
            int indent = lispWordChecker.apply( level.tokenized.get(1).content );
            if ( indent == Integer.MIN_VALUE ) {
                length = level.tokenized.get(2).index;
            } else {
                length = level.tokenized.get(0).index + indent ;
            }
        }
        return length;
    }


    public static void semiExecute(ArrayDeque<Level> stack, Tokenized tokenized) {
        for ( Token t : tokenized ) {
            switch ( t.type ) {
                case BEGIN:
                    /*
                     * Note thtat add the token twice; the first addition of the token is a dummy
                     * token which keeps the information about its position of the parenthesis and tells
                     * the information at the outer level which is old.
                     */
                    stack.peek().addToken(t); // to the old stack

                    /*
                     * Create a new stack level.
                     */
                    stack.push( new Level() );

                    /*
                     * This is the second addition of the token. This goes to the first element of
                     * the inner level which is new.
                     */
                    stack.peek().addToken(t); // to the new stack 
                    break;
                case END:
                    // Protect the base level and ignore any unnecessary parentheses.
                    if ( 1< stack.size() ) {
                        stack.peek().addToken(t);
                        stack.pop();
                    }
                    break;
                case ID:
                    stack.peek().addToken(t);
                    break;
                default:
                    break;
            }
        }
    }
    
    
    public static String calculateIndentSizeO(String text, int pos, Function<String,Integer> lispWordChecker ) {
        int beginIndex = SchemeIndentChanger.lookupLineStart(text, pos );
        int endIndex   = SchemeIndentChanger.lookupLineEnd(  text, pos );
        String subText = text.substring(beginIndex, endIndex);
        Tokenized tokenizedLine = tokenize( subText, 0 );
        ArrayDeque<Level> stack = createStack();
        semiExecute(stack, tokenizedLine );
        int length = calculateIndentLength( lispWordChecker, stack );
        return SchemeIndentChanger.fillStr(' ', length );
    }
    
    public static String calculateIndentSize(String text, int pos, Function<String,Integer> lispWordChecker ) {
        String subText = text.substring(0, pos) ;
        String[] lines = subText.split( "\n" );
        Tokenized[] tokenizedLines = new Tokenized[ lines.length ];
        for ( int i=0; i<lines.length ;i++ ) {
            tokenizedLines[i] = tokenize( lines[i], i );
        }
        ArrayDeque<Level> stack = createStack();

        /*
         *  1. Process all tokenized lines. 
         */
        for ( int i=0; i<tokenizedLines.length ;i++ ) {
            Tokenized tokenized = tokenizedLines[i] ;
            semiExecute(stack, tokenized);
        }
        
        /*
         *  2. Determine the indent size. 
         *     This block is only executed after processed the first line.
         *     See calculateIndexLevel() function. 
         */
        if ( 1<stack.size() ) {
            int index = calculateIndentLength(lispWordChecker, stack);
            if ( 0 <=index  ) {
                return SchemeIndentChanger.fillStr(' ', index );
            }
        }
        return "";
    }


    public static void main(String[] args) {
        ArrayList<Token> result = tokenize( "( HELLO WORLD\\( \\) \\\\FOO \\\\ )", 0 );
        System.out.println( result );

        List<String> lispWords = Arrays.asList("lambda" );

        Function<String,Integer> lispWordChecker = (s)->lispWords.contains(s) ? 2 : Integer.MIN_VALUE;
        
        System.out.println( correctIndentation( lispWordChecker, "(lambda()\nhello world \n foo)" ));

        System.out.println( correctIndentation( lispWordChecker, "(lambda args\nhello world \n foo)" ) );
        System.out.println( correctIndentation( lispWordChecker, "(lambda* args\nhello world \n foo)" ) );
        System.out.println( correctIndentation( lispWordChecker, "(lambda*  () aa \nhello world \n foo)" ) );
        System.out.println( correctIndentation( lispWordChecker, "(\n'foo\n 'bar\n 'bum\n )" ) );
        System.out.println( correctIndentation( lispWordChecker, "('foo\n 'bar\n 'bum\n )" ) );
        System.out.println( correctIndentation( lispWordChecker, "(\n(\n(\n(\n(\n)\n)\n)\n)\n)\n" ) );
        System.out.println( correctIndentation( lispWordChecker, "(\n(\n(\n(\n(\n)\n)\n)\n)\n)\n         )\n         )\n(\n(\n" ) );

    }
}
