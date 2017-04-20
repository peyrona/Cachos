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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * A Java source code compiler that recieves source code in a String and that performs
 * compilation on the fly and using only memory (without touching disk).
 * <p>
 * Note: This class to compile requires "tools.jar" file from JDK lib to be added.
 * <p>
 * Info needed to understand how this class works can be found here:
 *      * hhttp://www.javabeat.net/articles/73-the-java-60-compiler-api-1.html
 *      * http://blogs.helion-prime.com/category/general-programming/java
 *      * http://today.java.net/article/2008/04/09/source-code-analysis-using-java-6-apis
 *      * http://www.ibm.com/developerworks/java/library/j-jcomp/index.html
 *
  * @author Francisco Morero Peyrona (http://peyrona.com)
 */
public final class InMemoryCompiler
{
    private JavaCompiler                        compiler;
    private JavaMemFileManager                  fileManager;
    private DiagnosticCollector<JavaFileObject> diagsCollector;

    //------------------------------------------------------------------------//

    /**
     * Constructor.
     * <p>
     * Initialises the Java compiler.
     */
    public InMemoryCompiler()
    {
        compiler       = ToolProvider.getSystemJavaCompiler();
        fileManager    = new JavaMemFileManager();
        diagsCollector = new DiagnosticCollector<JavaFileObject>();
    }

    //------------------------------------------------------------------------//

    /**
     * Compiles passed source code and returns the result of compilation.
     
     * @param className The name of the class
     * @param classCode The class source code
     * @return Compiled code
     */
    public byte[] compile( String className, String classCode )
    {
        byte[] compiledClass = null;

        if( className.endsWith( ".java" ) )
        {
            className = className.substring( 0, className.length() - ".java".length() );
        }

        try
        {
            JavaFileObject javaObjFromStr = new JavaObjectFromString( className, classCode );
            Iterable<? extends JavaFileObject> fileObjects = Arrays.asList( javaObjFromStr );
            CompilationTask compileTask = compiler.getTask( null, fileManager, diagsCollector, null, null, fileObjects );

            if( compileTask.call() )
            {
                compiledClass = fileManager.getClassBytes( className );
            }
        }
        catch( Exception exc )
        {
            exc.printStackTrace( System.err );
        }

        return compiledClass;
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics()
    {
        return diagsCollector.getDiagnostics();
    }

    //------------------------------------------------------------------------//
    // Inner Class
    // Makes a String behaving like a SimpleJavaObject.
    //------------------------------------------------------------------------//
    private final class JavaObjectFromString extends SimpleJavaFileObject
    {
        private String contents = null;

        JavaObjectFromString( String className, String contents ) throws URISyntaxException
        {
            super( new URI( "string:///"+ className + Kind.SOURCE.extension ), Kind.SOURCE );
            this.contents = contents;
        }

        @Override
        public CharSequence getCharContent( boolean ignoreEncodingErrors )
        {
            return contents;
        }
    }

    //------------------------------------------------------------------------//
    // Inner Class
    // Makes a SimpleJavaObject able to store compiled java code.
    //------------------------------------------------------------------------//
    private final class ClassMemFileObject extends SimpleJavaFileObject
    {
        private ByteArrayOutputStream os = new ByteArrayOutputStream();

        ClassMemFileObject( String className )
        {
            super( URI.create( "mem:///"+ className + Kind.CLASS.extension ), Kind.CLASS );
        }

        private byte[] getBytes()
        {
            return os.toByteArray();
        }

        @Override
        public OutputStream openOutputStream()
        {
            return os;
        }
    }

    //------------------------------------------------------------------------//
    // Inner Class
    // Standard FileManager reads classes from disk, but as we perform the
    // compilation in memory, we'll need our own FileManager.
    //------------------------------------------------------------------------//
    @SuppressWarnings("rawtypes")
    private final class JavaMemFileManager extends ForwardingJavaFileManager
    {
        private HashMap<String, ClassMemFileObject> classes = new HashMap<String, ClassMemFileObject>();

        JavaMemFileManager()
        {
            super( ToolProvider.getSystemJavaCompiler().getStandardFileManager( null, null, null ) );
        }

        @Override
        public JavaFileObject getJavaFileForOutput( Location location, String className,
                                                    Kind kind, FileObject sibling ) throws IOException
        {
            JavaFileObject ret = null;

            if( StandardLocation.CLASS_OUTPUT == location && JavaFileObject.Kind.CLASS == kind )
            {
                ClassMemFileObject clase = new ClassMemFileObject( className );
                classes.put( className, clase );
                ret = clase;
            }
            else
            {
                ret = super.getJavaFileForOutput( location, className, kind, sibling );
            }

            return ret;
        }

        private byte[] getClassBytes( String className )
        {
            byte[] ret = null;

            if( classes.containsKey( className ) )
            {
                ret = classes.get(className).getBytes();
            }

            return ret;
        }
    }
}