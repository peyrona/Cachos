/*
 * Copyright (C) 2010 Francisco José Morero Peyrona. All Rights Reserved.
 *
 * This file is part of 'Cachos' project: http://code.google.com/p/cachos/
 *
 * 'Cachos' is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the free
 * Software Foundation; either version 3, or (at your option) any later version.
 *
 * 'Cachos' is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Cachos; see the file COPYING.  If not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.peyrona.cachos;

import java.util.Stack;

/**
 * A class that implements an Arithmetical Evaluator in Java.
 * <p>
 * It evaluates expressions in both, infix and postfix forms.
 *
  * @author peyrona (http://peyrona.com)
 */
public class ArithmeticalEvaluator
{
  /**
   * Eval an Infix expression.
   *
   * @param sExpr Expression to be evaluated.
   * @return The result.
   * @throws NumberFormatException
   */
  public static double evalInfix( String sExpr ) throws NumberFormatException
  {
      return evalPostfix( infix2Postfix( sExpr ) );
  }

  /**
   * Eval a Postfix expression.
   *
   * @param sExpr Expression to be evaluated.
   * @return The result.
   * @throws NumberFormatException
   */
  public static double evalPostfix( String sExpr ) throws NumberFormatException
  {
      double nResult = 0;
      char[] acExpr  = sExpr.toCharArray();

      if( isValidExpression( acExpr ) )
      {
          nResult = evaluate( acExpr );
      }
      else
      {
          throw new IllegalArgumentException( sExpr );
      }

      return nResult;
  }

  /**
   * Transforms an infix expression into a postfix one.
   *
   * @param sInfix Infix expression to be transformed.
   * @return The postfix form.
   */
  public static String infix2Postfix( String sExpr )
  {
      char[] acInfix = sExpr.toCharArray();

      // Initialize an empty stack and empty result string variable.
      Stack<Character> stkOps    = new Stack<Character>();
      StringBuilder    sbPostFix = new StringBuilder( acInfix.length );

      // Read the infix expression from left to right, one character at a time
      for( int n = 0; n < acInfix.length; n++ )
      {
          if( acInfix[n] != ' ' )
          {
              if( isOperand( acInfix[n] ) )          // If next token is an operand (begins with digit),
              {
                  sbPostFix.append( acInfix[n] );    // append it to postfix string.
              }
              else if( isOperator( acInfix[n]) )     // If it is and operator ("+-*/")
              {
                      sbPostFix.append( ' ' );       // To separate numbers

                      while( ! stkOps.isEmpty() )    // Move all operators from stack to postfix String until
                      {                              // a lower level operator or '(' is found
                          char c = stkOps.peek().charValue();

                          if( c == '(' || isLowerOp( acInfix[n], c ) )
                          {
                               break;
                          }

                          sbPostFix.append ( c ).append( ' ' ); // ' ' Separate operators
                          stkOps.pop();
                      }

                      stkOps.push( new Character( acInfix[n] ) );
              }
              else if( acInfix[n] == '(' )                      // If is an '('
              {
                  stkOps.push( new Character( acInfix[n] ) );   // Push it into the stack
              }
              else                                              // It must be an ')'
              {
                  sbPostFix.append( ' ' );

                  char c = stkOps.pop().charValue();

                  while( ! stkOps.isEmpty() && c != '(' )  // Move all from stack to postfix String until '(' is found,
                  {                                        // removing from stack even the '('
                      sbPostFix.append( c ).append( ' ' );
                      c = stkOps.pop().charValue();
                  }
              }
          }
      }

      if( sbPostFix.charAt( sbPostFix.length() - 1 ) != ' ' )
          sbPostFix.append( ' ' );

      // When the end of the input string is found, pop all operators and append them to the result string
      while( ! stkOps.empty() )
      {
          sbPostFix.append( stkOps.pop().charValue() ).append( ' ' );
      }

      sbPostFix.deleteCharAt( sbPostFix.length() - 1 );  // Deletes last ' '

      return sbPostFix.toString();
  }

  /**
   * The internal (private) method that performs the evaluation.
   *
   * @param acExpr A PostFix expression
   * return The result.
   * @throws NumberFormatException
   */
  private static double evaluate( char[] acExpr ) throws NumberFormatException
  {
      Stack<Double> stack = new Stack<Double>();
      StringBuilder sb    = new StringBuilder();

      for( int n = 0; n < acExpr.length; n++ )
      {
          if( isOperand( acExpr[n] ) )
          {
              sb.append( acExpr[n] );
          }
          else   // Can be an operator or a separator ' '
          {
              if( acExpr[n] == ' ' )
              {
                  if( sb.length() > 0 )
                  {
                      stack.push( new Double( sb.toString() ) );
                      sb.delete( 0, sb.length() );
                  }
              }
              else
              {
                  double n1 = stack.pop().doubleValue();
                  double n2 = stack.pop().doubleValue();

                  switch( acExpr[n] )
                  {
                      case '+': stack.push( new Double( n2 + n1 ) );  break;
                      case '-': stack.push( new Double( n2 - n1 ) );  break;
                      case '*': stack.push( new Double( n2 * n1 ) );  break;
                      case '/': stack.push( new Double( n2 / n1 ) );  break;
                  }
              }
          }
      }

      return (stack.pop().doubleValue());
  }

  /**
   * Return true if passed parameter is an operand that this evaluator can
   * manage.
   *
   * @param c char to be checked
   * @return true if passed parameter is an operand that this evaluator can
   *         manage.
   */
  private static boolean isOperand( char c )
  {
      return ((c >= '0' && c <= '9') || c == '.');
  }

  /**
   * Return true if passed parameter is an operator that this evaluator can
   * manage.
   *
   * @param c char to be checked
   * @return true if passed parameter is an operator that this evaluator can
   *         manage.
   */
  private static boolean isOperator( char c )
  {
      return (c == '+' || c == '-' || c == '*' || c == '/');
  }

  /**
   * Returns true if and only if cOp1 is higer precedence that cOp2
   * (if both are equals, it return false).
   *
   * @param cOp1 First operator to be compared.
   * @param cOp2 Second operator to be compared.
   * @return true if and only if cOp1 is higer precedence that
   *         cOp2 (if both are equals, it return false).
   */
  private static boolean isLowerOp( char cOp1, char cOp2 )
  {
      if( cOp1 == '+' || cOp1 == '-' )
      {
          return false;
      }

      return (cOp2 == '+' || cOp2 == '-');
  }

  /**
   * Check if all chars in passed expression are valid.
   * <p>
   * A valid expression must be composed
   * @param sExpr
   * @return
   */
  private static boolean isValidExpression( char[] acExpr )
  {
      boolean bValid = true;

      for( int n = 0; n < acExpr.length; n++ )
      {
          if( (acExpr[n] < '(' || acExpr[n] > '9') && (acExpr[n] != ' ') )
          {
              bValid = false;
              break;
          }
      }

      return bValid;
  }

  //=========================================================================//
  // Just for test purposes.
  //=========================================================================//
  public static void main( String[] asArg )
  {      
      String sExpr = "(57 / 3 - (17 / 2)) + 1 -(15*3/1.666666)";  // Result: 3 5 6 * + 7 8 5 + * -

      System.out.println( "Expression in INFIX form     = '"+ sExpr +"'" );
      System.out.println( "Expression in POSTFIX form   = '"+
      ArithmeticalEvaluator.infix2Postfix( sExpr )+"'" );
      System.out.println( "Expression evaluation result = " +
      ArithmeticalEvaluator.evalInfix( sExpr ) );
      System.out.println();

      System.out.print( "Time to process 1000 times the expression in INFIX form  : " );
      long t = System.currentTimeMillis();

      for( int n = 0; n < 999; n++)
      {
          ArithmeticalEvaluator.evalInfix ( sExpr );
      }
      
      System.out.println( (double) (System.currentTimeMillis() - t)  / 1000);

      System.out.print( "Time to process 1000 times the expression in POSTFIX form: " );
      t = System.currentTimeMillis();
      sExpr = ArithmeticalEvaluator.infix2Postfix( sExpr );

      for( int n = 0; n < 999; n++)
      {
          ArithmeticalEvaluator.evalPostfix( sExpr );
      }

      System.out.println( (double) (System.currentTimeMillis() - t)  / 1000);
  }
}