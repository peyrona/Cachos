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

import java.lang.reflect.Method;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * This class evaluates code fragments as well full (normal) Java source code
 * clases in execution time (on the fly).
 * <p>
 * It creates an instance of the Java compiler (through the public API made
 * available starting with Java SE ver. 6.0); it compiles source code in 
 * memory, loads compiled code from memory into the JVM and executes it from
 * memory (disk is not used at any time).
 * <p>
 * Nota: A la hora de ejecutar código y en el caso de que tengamos que pasar
 * un único parámetro y este sea un array, como este tiene que ser de objetos
 * (cualquier instancia que tenga a la clase Object como superpadre), se
 * producirá una ambiguedad, ya que la lista de parámetros (elipsis "...") se
 * traduce en Java internamente como un array (un Object[]), por eso, cuando
 * tengamos un sólo parámentro que pasar al evaluador y este sea un array, lo
 * haremos del siguiente modo:<br>
 * <code>new Object[] { param }</code>
 *
  * @author Francisco Morero Peyrona (http://peyrona.com)
 */
public final class InMemoryExecutor
{
    private InMemoryCompiler compiler = null;
    private StringBuilder    errors   = null;

    //------------------------------------------------------------------------//

    /**
     * Argument zero constructor.
     */
    public InMemoryExecutor()
    {
        compiler = new InMemoryCompiler();
        errors   = new StringBuilder( 1024 );
    }

    /**
     * Invoques class main method passing an empty String[] as parameter.
     *
     * @param className Name of the class to be executed.
     * @param javaCode  Source class code.
     */
    public void execute( String className, String javaCode )
    {
        // main( ... ) requires one an only one parameter and it must be a String array.
        // Therefore we pass an empty String array.
        Object[] params = new Object[] { new String[] {} };

        execute( className, javaCode, params );
    }

    /**
     * Invoques class main method passing parameters.
     *
     * @param className Name of the class to be executed.
     * @param javaCode  Source class code.
     * @param params Objects to be passed as parameters to main method.
     */
    public void execute( String className, String javaCode, Object... params )
    {
        Class<?> claseParam = (new String[0]).getClass();

        _execute( className, javaCode, "main", new Class<?>[] { claseParam }, params );
    }

    /**
     * Invoques a method (the method receives no parameters).
     *
     * @param className  Name of the class to be executed.
     * @param javaCode   Source class code.     
     * @param methodName Method name to be invoqued.
     * @return The result of invoking the method or null.
     */
    public Object executeMethod( String className, String javaCode, String methodName )
    {
        return _execute( className, javaCode, methodName, null );
    }

    /**
     * Invoques a method passing parameters.
     *
     * @param className  Name of the class to be executed.
     * @param javaCode   Source class code.
     * @param params     Objects to be passed as parameters to main method.
     * @param methodName Method name to be invoqued.
     * @param params     Objects to be passed as parameters to the method.
     * @return The result of invoking the method or null.
     */
    public Object executeMethod( String className, String javaCode, String methodName, Object... params )
    {
        // Finding out formal parameters based on real parameters
        Class<?>[] formalParams = new Class<?>[ params.length ];

        for( int n = 0; n < params.length; n++ )
        {
            formalParams[n] = params[n].getClass();
        }

        // Invoking the executor
        return _execute( className, javaCode, methodName, formalParams, params );
    }

    /**
     * Executes a Java code block without passing parameters.
     *
     * @param bloque Java code block to be executed.
     * @return The result of the execution or null.
     */
    public Object executeBlock( String bloque )
    {
        String patron =
            "public class __ExecBlockWithoutParams__"+
            "{"+
            "    public Object __execute__()"+
            "    {"+
                     bloque +
            "    }"+
            "}";

        return executeMethod( "__ExecBlockWithoutParams__", patron, "__execute__" );
    }

    /**
     * Executes a Java code block passing parameters.
     * <p>
     * Formal parameter names are:  p1, p2, ..., p<N>
     *
     * @param bloque Java code block to be executed.
     * @param params Objects to be passed as parameters to the block.
     * @return The result of the execution or null.
     */
    public Object executeBlock( String bloque, Object... params )
    {
        String patron =
            "public class __ExecBlockWithParams__"+
            "{"+
            "    public Object __execute__( {*} )"+
            "    {"+
                     bloque +
            "    }"+
            "}";

        // Creating formal parameters based on real ones.
        StringBuilder formales = new StringBuilder( 512 );

        for( int n = 0; n < params.length; n++ )
        {
            formales.append( params[n].getClass().getName() ).
                     append( " p" ).append( n+1 ).append( ", " );
        }

        formales.delete( formales.length() - 2, formales.length() - 1 );   // Quita el último ", "

        patron = patron.replace( "{*}", formales.toString() );

        return executeMethod( "__ExecBlockWithParams__", patron, "__execute__", params );
    }

    /**
     * Return errors (if any) occurred during code invocation.
     *
     * @return Errors (if any) occurred during code invocation
     */
    public String getErrors()
    {
        String s = errors.toString();

        return (s.length() == 0 ? null : s);
    }

    //------------------------------------------------------------------------//

    private void addError( String error )
    {
        errors.append( error ).
                append( '\n' ).
                append( "==================================================================" );
    }

    private Object _execute( String className, String javaCode, String methodName,
                             Class<?>[] formalParams, Object... realParams )
    {
        Object result = null;

        if( className.endsWith( ".java" ) )
        {
            className = className.substring( 0, className.length() - ".java".length() );
        }

        byte[] compiledClass = compiler.compile( className, javaCode );

        if( compiledClass != null )
        {
            ByteArrayClassLoader bacl  = new ByteArrayClassLoader( compiledClass );
            Class<?>          clazz = bacl.findClass( className );

            try
            {
                Object instance = clazz.newInstance();
                Method method   = clazz.getDeclaredMethod( methodName, formalParams );

                result = method.invoke( instance,
                                        (realParams == null ? new Object[] {} :
                                                               realParams) );
            }
            catch( Exception exc )
            {
                addError( "Error executing method '"+ methodName +"'\n"+
                          "\ten clase '"+ className +"'"+
                          exc.getMessage() );
            }
        }
        else
        {
            List<Diagnostic<? extends JavaFileObject>> diagnostics = compiler.getDiagnostics();
            StringBuilder sb = new StringBuilder( 1024*3 );

            for( Diagnostic<? extends JavaFileObject> d : diagnostics )
            {
                sb.append( "Error compiling class '"+ className +"'\n"+
                           "Line "+ d.getLineNumber() +":\n"+
                           d.getMessage( null ) );
            }

            addError( sb.toString() );
        }

        return result;
    }

    //------------------------------------------------------------------------//
    // Inner Class
    // ClassLoader to load a class form a byte[]
    //------------------------------------------------------------------------//
    private static final class ByteArrayClassLoader extends ClassLoader
    {
        byte[] bytes;

        public ByteArrayClassLoader( byte[] bytes )
        {
            this.bytes = bytes;
        }

        @Override
        public Class<?> findClass( String className )
        {
            return defineClass( className, bytes, 0, bytes.length );
        }
    }

    //========================================================================//
    // TESTING
    // The content of main() acts as somekind of JUnit test.
    //========================================================================//
    public static void main( String[] as )
    {
        InMemoryExecutor ime = new InMemoryExecutor();

        System.out.println( "Test #1 ===============================================================" );

        String javaMainWithoutParm =
               "public class TestClass" +
               "{"+
               "    public static void main( String[] as )"+
               "    {"+
               "        System.out.println( \"MainWithoutParm\" );"+
               "    }"+
               "}";

        ime.execute( "TestClass", javaMainWithoutParm );
        assert( ime.getErrors() == null );

        System.out.println( "Test #2 ===============================================================" );

        String javaMainWithParm =
               "public class TestClass" +
               "{"+
               "    public static void main( String[] as )"+
               "    {"+
               "        System.out.println( as[0] );"+
               "    }"+
               "}";

        String[]  param = new String[] {"MainWithParm"};
        ime.execute( "TestClass", javaMainWithParm, new Object[] { param } );
        assert( ime.getErrors() == null );

        System.out.println( "Test #3 ===============================================================" );

        String javaMethodWithoutParam =
               "public class TestClass" +
               "{"+
               "    public String test()" +
               "    {"+
               "        return \"MethodWithoutParam\";" +
               "    }" +
               "}";

        String ret1 = (String) ime.executeMethod( "TestClass", javaMethodWithoutParam, "test" );
        assert( "MetodoSinParam".equals( ret1 ) );

        System.out.println( "Test #4 ===============================================================" );

        String javaMethodWithParam =
               "public class TestClass" +
               "{"+
               "    public String test( String s1, String s2 )" +
               "    {"+
               "        return (s1 + s2);" +
               "    }" +
               "}";

        String ret2 = (String) ime.executeMethod( "TestClass", javaMethodWithParam, "test", "Method", "WithParam");
        assert( "MethodWithParam".equals( ret2 ) );

        System.out.println( "Test #5 ===============================================================" );

        String javaBlockWithoutParam = "return \"BlockWithoutParam\";";
        String ret3 = (String) ime.executeBlock( javaBlockWithoutParam );
        assert( "BlockWithoutParam".equals( ret3 ) );

        System.out.println( "Test #6 ===============================================================" );

        String ret4 = (String) ime.executeBlock( "return p1 + p2 + p3;", "Block", "With", "Param" );
        assert( "BlockWithParam".equals( ret4 ) );
    }
}