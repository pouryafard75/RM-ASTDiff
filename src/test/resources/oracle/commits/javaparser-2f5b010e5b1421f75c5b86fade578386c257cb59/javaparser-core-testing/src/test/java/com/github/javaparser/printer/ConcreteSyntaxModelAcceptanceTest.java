/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2023 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser.printer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static com.github.javaparser.StaticJavaParser.parse;

class ConcreteSyntaxModelAcceptanceTest {
    private final Path rootDir = CodeGenerationUtils.mavenModuleRoot(ConcreteSyntaxModelAcceptanceTest.class).resolve("src/test/test_sourcecode");

    private String prettyPrint(Node node) {
        return ConcreteSyntaxModel.genericPrettyPrint(node);
    }

    private String prettyPrintedExpectation(String name) throws IOException {
        return TestUtils.readResource("com/github/javaparser/printer/" + name + "_prettyprinted");
    }

    @Test
    void printingExamplePrettyPrintVisitor() throws IOException {
        CompilationUnit cu = parse(rootDir.resolve("com/github/javaparser/printer/PrettyPrintVisitor.java"));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("PrettyPrintVisitor"), prettyPrint(cu));
    }

    @Test
    void printingExampleJavaConcepts() throws IOException {
        CompilationUnit base = parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsBase.java"));
        CompilationUnit enums = parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsEnums.java"));
        CompilationUnit innerClass = parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsInnerClasses.java"));
        CompilationUnit methods = parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsMethods.java"));
        CompilationUnit ugly = parse(rootDir.resolve("com/github/javaparser/printer/JavaConceptsUgly.java"));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("JavaConceptsBase"), prettyPrint(base));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("JavaConceptsEnums"), prettyPrint(enums));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("JavaConceptsInnerClasses"), prettyPrint(innerClass));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("JavaConceptsMethods"), prettyPrint(methods));
        TestUtils.assertEqualsStringIgnoringEol(prettyPrintedExpectation("JavaConceptsUgly"), prettyPrint(ugly));
        }

}
