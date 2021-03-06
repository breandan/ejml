/*
 * Copyright (c) 2009-2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.dense.row.misc;

import org.ejml.CodeGeneratorBase;

import java.io.FileNotFoundException;

/**
 * Generates unrolled matrix from minor analytical functions. these can run much faster than LU but will only
 * work for small matrices.
 *
 * When computing the determinants for each minor there are some repeat calculations going on. I manually
 * removed those by storing them in a local variable and only computing it once. Despite reducing the FLOP count
 * it didn't seem to noticeably improve performance in a runtime benchmark..
 *
 * @author Peter Abeles
 */
public class GenerateUnrolledCholesky_DDRM extends CodeGeneratorBase {

    int maxSize;

    public GenerateUnrolledCholesky_DDRM( int maxSize ) throws FileNotFoundException {
        this.maxSize = maxSize;
    }

    @Override
    public void generate() throws FileNotFoundException {
        setOutputFile("UnrolledCholesky_DDRM");

        printTop();

        printCalls("lower");
        printCalls("upper");

        for (int i = 1; i <= maxSize; i++) {
            printLower(i);
        }
        for (int i = 1; i <= maxSize; i++) {
            printUpper(i);
        }

        out.print("}\n");
    }

    private void printTop() {

        String foo =
                "import org.ejml.UtilEjml;\n" +
                        "import org.ejml.data.DMatrix1Row;\n" +
                        "\n" +
                        "\n" +
                        "/**\n" +
                        " * Performs an unrolled lower cholesky decomposition for small matrices.\n" +
                        standardClassDocClosing("Peter Abeles") +
                        "public class " + className + " {\n" +
                        "    \n" +
                        "    public static final int MAX = " + maxSize + ";\n";

        out.print(foo);
    }

    private void printCalls( String which ) {
        out.print("    public static boolean " + which + "( DMatrix1Row A , DMatrix1Row L ) {\n" +
                "        switch( A.numRows ) {\n");
        for (int i = 1; i <= maxSize; i++) {
            out.print("            case " + i + ": return " + which + i + "(A,L);\n");
        }
        out.print(
                "            default: return false;\n" +
                        "        }\n" +
                        "    }\n\n");
    }

    private void printLower( int N ) {
        out.print("    public static boolean lower" + N + "( DMatrix1Row A , DMatrix1Row L )\n" +
                "    {\n" +
                "        double []data = A.data;\n" +
                "\n");

        // extracts the first minor
        int[] matrix = new int[N*N];
        for (int i = 1, count = 0; i <= N; i++) {
            for (int j = 1; j <= i; j++, count++) {
                int index = (i - 1)*N + (j - 1);
                matrix[count] = index;
                out.print("        double " + a(i, j) + " = " + "data[ " + index + " ];\n");
            }
        }
        out.println();

        int count = 0;
        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                if (j > i) {
                    out.println("        L.data[" + ((i - 1)*N + j - 1) + "] = 0;");
                } else if (i == j) {
                    String assign = j < N ? "= " + a(i, i) : "";
                    out.print("        L.data[" + matrix[count] + "] " + assign + " = Math.sqrt(" + a(i, i));
                    for (int k = 1; k < j; k++) {
                        out.print("-" + a(i, k) + "*" + a(i, k));
                    }
                    out.println(");");
                    count++;
                } else {
                    out.print("        L.data[" + matrix[count] + "] = " + a(i, j) + " = (" + a(i, j));
                    for (int k = 1; k < j; k++) {
                        out.print("-" + a(i, k) + "*" + a(j, k));
                    }
                    out.println(")/" + a(j, j) + ";");
                    count++;
                }
            }
        }
        out.println("        return !UtilEjml.isUncountable(L.data[" + matrix[count - 1] + "]);");
        out.print("    }\n");
        out.print("\n");
    }

    private void printUpper( int N ) {
        out.print("    public static boolean upper" + N + "( DMatrix1Row A , DMatrix1Row R )\n" +
                "    {\n" +
                "        double []data = A.data;\n" +
                "\n");

        // extracts the first minor
        int[] matrix = new int[N*N];
        for (int j = 1, count = 0; j <= N; j++) {
            for (int i = 1; i <= j; i++, count++) {
                int index = (i - 1)*N + (j - 1);
                matrix[count] = index;
                out.print("        double " + a(i, j) + " = " + "data[ " + index + " ];\n");
            }
        }
        out.println();

        int count = 0;
        for (int j = 1; j <= N; j++) {
            for (int i = 1; i <= N; i++) {
                if (j < i) {
                    out.println("        R.data[" + ((i - 1)*N + j - 1) + "] = 0;");
                } else if (i == j) {
                    String assign = j < N ? "= " + a(i, i) : "";
                    out.print("        R.data[" + matrix[count] + "] " + assign + " = Math.sqrt(" + a(i, i));
                    for (int k = 1; k < i; k++) {
                        out.print("-" + a(k, i) + "*" + a(k, i));
                    }
                    out.println(");");
                    count++;
                } else {
                    out.print("        R.data[" + matrix[count] + "] = " + a(i, j) + " = (" + a(i, j));
                    for (int k = 1; k < i; k++) {
                        out.print("-" + a(k, i) + "*" + a(k, j));
                    }
                    out.println(")/" + a(i, i) + ";");
                    count++;
                }
            }
        }

        out.println("        return !UtilEjml.isUncountable(R.data[" + matrix[count - 1] + "]);");
        out.print("    }\n");
        out.print("\n");
    }

    private String a( int row, int col ) {
        return "a" + row + "" + col;
    }

    public static void main( String[] args ) throws FileNotFoundException {
        GenerateUnrolledCholesky_DDRM gen = new GenerateUnrolledCholesky_DDRM(7);

        gen.generate();
    }
}