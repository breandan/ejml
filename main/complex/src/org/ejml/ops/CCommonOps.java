/*
 * Copyright (c) 2009-2014, Peter Abeles. All Rights Reserved.
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

package org.ejml.ops;

import org.ejml.EjmlParameters;
import org.ejml.alg.dense.decompose.lu.LUDecompositionAlt_CD64;
import org.ejml.alg.dense.linsol.LinearSolverSafe;
import org.ejml.alg.dense.linsol.lu.LinearSolverLu_CD64;
import org.ejml.alg.dense.misc.CTransposeAlgs;
import org.ejml.alg.dense.mult.CMatrixMatrixMult;
import org.ejml.data.*;
import org.ejml.factory.CLinearSolverFactory;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;

import java.util.Arrays;

/**
 * Common operations on complex numbers
 *
 * @author Peter Abeles
 */
// TODO multiply scale
// TODO multiply add
// TODO multiply scale add
public class CCommonOps {

    /**
     * <p>
     * Creates a new square matrix whose diagonal elements are specified by data and all
     * the other elements are zero.<br>
     * <br>
     * a<sub>ij</sub> = 0         if i &le; j<br>
     * a<sub>ij</sub> = diag[i]   if i = j<br>
     * </p>
     *
     * @param data Contains the values of the diagonal elements of the resulting matrix.
     * @return A new complex matrix.
     */
    public static CDenseMatrix64F diag( double... data ) {
        if( data.length%2 == 1 )
            throw new IllegalArgumentException("must be an even number of arguments");

        int N = data.length/2;

        CDenseMatrix64F m = new CDenseMatrix64F(N,N);

        int index = 0;
        for (int i = 0; i < N; i++) {
            m.set(i,i,data[index++],data[index++]);
        }

        return m;
    }

    /**
     * Converts the real matrix into a complex matrix.
     *
     * @param input Real matrix. Not modified.
     * @param output Complex matrix. Modified.
     */
    public static void convert( D1Matrix64F input , CD1Matrix64F output ) {
        if( input.numCols != output.numCols || input.numRows != output.numRows ) {
            throw new IllegalArgumentException("The matrices are not all the same dimension.");
        }

        Arrays.fill(output.data, 0, output.getDataLength(), 0);

        final int length = output.getDataLength();

        for( int i = 0; i < length; i += 2 ) {
            output.data[i] = input.data[i/2];
        }
    }

    /**
     * Places the real component of the input matrix into the output matrix.
     *
     * @param input Complex matrix. Not modified.
     * @param output real matrix. Modified.
     */
    public static DenseMatrix64F stripReal( CD1Matrix64F input , DenseMatrix64F output ) {
        if( output == null ) {
            output = new DenseMatrix64F(input.numRows,input.numCols);
        } else if( input.numCols != output.numCols || input.numRows != output.numRows ) {
            throw new IllegalArgumentException("The matrices are not all the same dimension.");
        }

        final int length = input.getDataLength();

        for( int i = 0; i < length; i += 2 ) {
            output.data[i/2] = input.data[i];
        }
        return output;
    }

    /**
     * Places the imaginary component of the input matrix into the output matrix.
     *
     * @param input Complex matrix. Not modified.
     * @param output real matrix. Modified.
     */
    public static DenseMatrix64F stripImaginary( CD1Matrix64F input , DenseMatrix64F output ) {
        if( output == null ) {
            output = new DenseMatrix64F(input.numRows,input.numCols);
        } else if( input.numCols != output.numCols || input.numRows != output.numRows ) {
            throw new IllegalArgumentException("The matrices are not all the same dimension.");
        }

        final int length = input.getDataLength();

        for( int i = 1; i < length; i += 2 ) {
            output.data[i/2] = input.data[i];
        }
        return output;
    }

    /**
     * <p>
     * Computes the magnitude of the complex number in the input matrix and stores the results in the output
     * matrix.
     * </p>
     *
     * magnitude = sqrt(real^2 + imaginary^2)
     *
     * @param input Complex matrix. Not modified.
     * @param output real matrix. Modified.
     */
    public static void magnitude( CD1Matrix64F input , D1Matrix64F output ) {
        if( input.numCols != output.numCols || input.numRows != output.numRows ) {
            throw new IllegalArgumentException("The matrices are not all the same dimension.");
        }

        final int length = input.getDataLength();

        for( int i = 0; i < length; i += 2 ) {
            double real = input.data[i];
            double imaginary = input.data[i+1];

            output.data[i/2] = Math.sqrt(real*real + imaginary*imaginary);
        }
    }

    /**
     * <p>
     * Sets every element in the matrix to the specified value.<br>
     * <br>
     * a<sub>ij</sub> = value
     * <p>
     *
     * @param a A matrix whose elements are about to be set. Modified.
     * @param real The real component
     * @param imaginary The imaginary component
     */
    public static void fill(CD1Matrix64F a, double real, double imaginary)
    {
        int N = a.getDataLength();
        for (int i = 0; i < N; i += 2) {
            a.data[i] = real;
            a.data[i+1] = imaginary;
        }
    }

    /**
     * <p>Performs the following operation:<br>
     * <br>
     * c = a + b <br>
     * c<sub>ij</sub> = a<sub>ij</sub> + b<sub>ij</sub> <br>
     * </p>
     *
     * <p>
     * Matrix C can be the same instance as Matrix A and/or B.
     * </p>
     *
     * @param a A Matrix. Not modified.
     * @param b A Matrix. Not modified.
     * @param c A Matrix where the results are stored. Modified.
     */
    public static void add( CD1Matrix64F a , CD1Matrix64F b , CD1Matrix64F c )
    {
        if( a.numCols != b.numCols || a.numRows != b.numRows
                || a.numCols != c.numCols || a.numRows != c.numRows ) {
            throw new IllegalArgumentException("The matrices are not all the same dimension.");
        }

        final int length = a.getDataLength();

        for( int i = 0; i < length; i++ ) {
            c.data[i] = a.data[i]+b.data[i];
        }
    }

    /**
     * <p>Performs the following operation:<br>
     * <br>
     * c = a - b <br>
     * c<sub>ij</sub> = a<sub>ij</sub> - b<sub>ij</sub> <br>
     * </p>
     *
     * <p>
     * Matrix C can be the same instance as Matrix A and/or B.
     * </p>
     *
     * @param a A Matrix. Not modified.
     * @param b A Matrix. Not modified.
     * @param c A Matrix where the results are stored. Modified.
     */
    public static void subtract( CD1Matrix64F a , CD1Matrix64F b , CD1Matrix64F c )
    {
        if( a.numCols != b.numCols || a.numRows != b.numRows
                || a.numCols != c.numCols || a.numRows != c.numRows ) {
            throw new IllegalArgumentException("The matrices are not all the same dimension.");
        }

        final int length = a.getDataLength();

        for( int i = 0; i < length; i++ ) {
            c.data[i] = a.data[i]-b.data[i];
        }
    }

    /**
     * <p>Performs the following operation:<br>
     * <br>
     * c = a * b <br>
     * <br>
     * c<sub>ij</sub> = &sum;<sub>k=1:n</sub> { * a<sub>ik</sub> * b<sub>kj</sub>}
     * </p>
     *
     * @param a The left matrix in the multiplication operation. Not modified.
     * @param b The right matrix in the multiplication operation. Not modified.
     * @param c Where the results of the operation are stored. Modified.
     */
    public static void mult(CDenseMatrix64F a, CDenseMatrix64F b, CDenseMatrix64F c)
    {
        if( b.numCols >= EjmlParameters.CMULT_COLUMN_SWITCH) {
            CMatrixMatrixMult.mult_reorder(a, b, c);
        } else {
            CMatrixMatrixMult.mult_small(a, b, c);
        }
    }

    /**
     * <p>Performs the following operation:<br>
     * <br>
     * c = &alpha; * a * b <br>
     * <br>
     * c<sub>ij</sub> = &alpha; &sum;<sub>k=1:n</sub> { * a<sub>ik</sub> * b<sub>kj</sub>}
     * </p>
     *
     * @param realAlpha real component of scaling factor.
     * @param imgAlpha imaginary component of scaling factor.
     * @param a The left matrix in the multiplication operation. Not modified.
     * @param b The right matrix in the multiplication operation. Not modified.
     * @param c Where the results of the operation are stored. Modified.
     */
    public static void mult( double realAlpha , double imgAlpha , CDenseMatrix64F a , CDenseMatrix64F b , CDenseMatrix64F c )
    {
        if( b.numCols >= EjmlParameters.CMULT_COLUMN_SWITCH ) {
            CMatrixMatrixMult.mult_reorder(realAlpha,imgAlpha,a,b,c);
        } else {
            CMatrixMatrixMult.mult_small(realAlpha,imgAlpha,a,b,c);
        }
    }

    /**
     * <p>
     * Performs the following operation:<br>
     * <br>
     * c = c + a * b<br>
     * c<sub>ij</sub> = c<sub>ij</sub> + &sum;<sub>k=1:n</sub> { a<sub>ik</sub> * b<sub>kj</sub>}
     * </p>
     *
     * @param a The left matrix in the multiplication operation. Not modified.
     * @param b The right matrix in the multiplication operation. Not modified.
     * @param c Where the results of the operation are stored. Modified.
     */
    public static void multAdd( CDenseMatrix64F a , CDenseMatrix64F b , CDenseMatrix64F c )
    {
        if( b.numCols >= EjmlParameters.MULT_COLUMN_SWITCH ) {
            CMatrixMatrixMult.multAdd_reorder(a, b, c);
        } else {
            CMatrixMatrixMult.multAdd_small(a,b,c);
        }
    }

    /**
     * <p>
     * Performs the following operation:<br>
     * <br>
     * c = c + &alpha; * a * b<br>
     * c<sub>ij</sub> = c<sub>ij</sub> +  &alpha; * &sum;<sub>k=1:n</sub> { a<sub>ik</sub> * b<sub>kj</sub>}
     * </p>
     *
     * @param realAlpha real component of scaling factor.
     * @param imgAlpha imaginary component of scaling factor.
     * @param a The left matrix in the multiplication operation. Not modified.
     * @param b The right matrix in the multiplication operation. Not modified.
     * @param c Where the results of the operation are stored. Modified.
     */
    public static void multAdd( double realAlpha , double imgAlpha , CDenseMatrix64F a , CDenseMatrix64F b , CDenseMatrix64F c )
    {
        if( b.numCols >= EjmlParameters.CMULT_COLUMN_SWITCH ) {
            CMatrixMatrixMult.multAdd_reorder(realAlpha,imgAlpha,a,b,c);
        } else {
            CMatrixMatrixMult.multAdd_small(realAlpha,imgAlpha,a,b,c);
        }
    }



    /**
     * <p>Performs an "in-place" transpose.</p>
     *
     * <p>
     * For square matrices the transpose is truly in-place and does not require
     * additional memory.  For non-square matrices, internally a temporary matrix is declared and
     * {@link #transpose(org.ejml.data.CDenseMatrix64F, org.ejml.data.CDenseMatrix64F)} is invoked.
     * </p>
     *
     * @param mat The matrix that is to be transposed. Modified.
     */
    public static void transpose( CDenseMatrix64F mat ) {
        if( mat.numCols == mat.numRows ){
            CTransposeAlgs.square(mat);
        } else {
            CDenseMatrix64F b = new CDenseMatrix64F(mat.numCols,mat.numRows);
            transpose(mat, b);
            mat.reshape(b.numRows, b.numCols);
            mat.set(b);
        }
    }

    /**
     * <p>
     * Transposes input matrix 'a' and stores the results in output matrix 'b':<br>
     * <br>
     * b<sub>ij</sub> = a<sub>ji</sub><br>
     * where 'b' is the transpose of 'a'.
     * </p>
     *
     * @param input The original matrix.  Not modified.
     * @param output Where the transpose is stored. If null a new matrix is created. Modified.
     * @return The transposed matrix.
     */
    public static CDenseMatrix64F transpose( CDenseMatrix64F input , CDenseMatrix64F output )
    {
        if( output == null ) {
            output = new CDenseMatrix64F(input.numCols,input.numRows);
        } else if( input.numCols != output.numRows || input.numRows != output.numCols ) {
            throw new IllegalArgumentException("Input and output shapes are not compatible");
        }

        CTransposeAlgs.standard(input,output);

        return output;
    }

    /**
     * <p>
     * Performs a matrix inversion operation on the specified matrix and stores the results
     * in the same matrix.<br>
     * <br>
     * a = a<sup>-1<sup>
     * </p>
     *
     * <p>
     * If the algorithm could not invert the matrix then false is returned.  If it returns true
     * that just means the algorithm finished.  The results could still be bad
     * because the matrix is singular or nearly singular.
     * </p>
     *
     * @param A The matrix that is to be inverted.  Results are stored here.  Modified.
     * @return true if it could invert the matrix false if it could not.
     */
    public static boolean invert( CDenseMatrix64F A )
    {
        LUDecompositionAlt_CD64 alg = new LUDecompositionAlt_CD64();
        LinearSolverLu_CD64 solver = new LinearSolverLu_CD64(alg);
        if( solver.setA(A) ) {
            solver.invert(A);
        } else {
            return false;
        }
        return false;
    }

    /**
     * <p>
     * Performs a matrix inversion operation that does not modify the original
     * and stores the results in another matrix.  The two matrices must have the
     * same dimension.<br>
     * <br>
     * b = a<sup>-1<sup>
     * </p>
     *
     * <p>
     * If the algorithm could not invert the matrix then false is returned.  If it returns true
     * that just means the algorithm finished.  The results could still be bad
     * because the matrix is singular or nearly singular.
     * </p>
     *
     * <p>
     * For medium to large matrices there might be a slight performance boost to using
     * {@link LinearSolverFactory} instead.
     * </p>
     *
     * @param input The matrix that is to be inverted. Not modified.
     * @param output Where the inverse matrix is stored.  Modified.
     * @return true if it could invert the matrix false if it could not.
     */
    public static boolean invert( CDenseMatrix64F input , CDenseMatrix64F output )
    {
        LUDecompositionAlt_CD64 alg = new LUDecompositionAlt_CD64();
        LinearSolverLu_CD64 solver = new LinearSolverLu_CD64(alg);

        if( solver.modifiesA() )
            input = input.copy();

        if( !solver.setA(input))
            return false;
        solver.invert(output);
        return true;
    }

    /**
     * <p>WARNING: Only supports square systems for now!</p>
     * <p>
     * Solves for x in the following equation:<br>
     * <br>
     * A*x = b
     * </p>
     *
     * <p>
     * If the system could not be solved then false is returned.  If it returns true
     * that just means the algorithm finished operating, but the results could still be bad
     * because 'A' is singular or nearly singular.
     * </p>
     *
     * <p>
     * If repeat calls to solve are being made then one should consider using {@link CLinearSolverFactory}
     * instead.
     * </p>
     *
     * <p>
     * It is ok for 'b' and 'x' to be the same matrix.
     * </p>
     *
     * @param a A matrix that is m by n. Not modified.
     * @param b A matrix that is n by k. Not modified.
     * @param x A matrix that is m by k. Modified.
     *
     * @return true if it could invert the matrix false if it could not.
     */
    public static boolean solve( CDenseMatrix64F a , CDenseMatrix64F b , CDenseMatrix64F x )
    {
        LinearSolver<CDenseMatrix64F> solver = CLinearSolverFactory.linear(a.numRows);

        // make sure the inputs 'a' and 'b' are not modified
        solver = new LinearSolverSafe<CDenseMatrix64F>(solver);

        if( !solver.setA(a) )
            return false;

        solver.solve(b,x);
        return true;
    }

    /**
     * Returns the determinant of the matrix.  If the inverse of the matrix is also
     * needed, then using {@link org.ejml.alg.dense.decomposition.lu.LUDecompositionAlt_D64} directly (or any
     * similar algorithm) can be more efficient.
     *
     * @param mat The matrix whose determinant is to be computed.  Not modified.
     * @return The determinant.
     */
    public static Complex64F det( CDenseMatrix64F mat  )
    {
        LUDecompositionAlt_CD64 alg = new LUDecompositionAlt_CD64();

        if( alg.inputModified() ) {
            mat = mat.copy();
        }

        if( !alg.decompose(mat) )
            return new Complex64F();
        return alg.computeDeterminant();
    }

    /**
     * <p>Performs  element by element multiplication operation with a complex numbert<br>
     * <br>
     * output<sub>ij</sub> = input<sub>ij</sub> * (real + imaginary*i) <br>
     * </p>
     * @param input The left matrix in the multiplication operation. Not modified.
     * @param real Real component of the number it is multiplied by
     * @param imaginary Imaginary component of the number it is multiplied by
     * @param output Where the results of the operation are stored. Modified.
     */
    public static void elementMultiply( CD1Matrix64F input , double real , double imaginary, CD1Matrix64F output )
    {
        if( input.numCols != output.numCols || input.numRows != output.numRows ) {
            throw new IllegalArgumentException("The 'input' and 'output' matrices do not have compatible dimensions");
        }

        int N = input.getDataLength();
        for (int i = 0; i < N; i += 2 ) {
            double inReal = input.data[i];
            double intImag = input.data[i+1];

            output.data[i] = inReal*real - intImag*imaginary;
            output.data[i+1] = inReal*imaginary + intImag*real;
        }
    }

    /**
     * <p>Performs  element by element division operation with a complex number on the right<br>
     * <br>
     * output<sub>ij</sub> = input<sub>ij</sub> / (real + imaginary*i) <br>
     * </p>
     * @param input The left matrix in the multiplication operation. Not modified.
     * @param real Real component of the number it is multiplied by
     * @param imaginary Imaginary component of the number it is multiplied by
     * @param output Where the results of the operation are stored. Modified.
     */
    public static void elementDivide( CD1Matrix64F input , double real , double imaginary, CD1Matrix64F output )
    {
        if( input.numCols != output.numCols || input.numRows != output.numRows ) {
            throw new IllegalArgumentException("The 'input' and 'output' matrices do not have compatible dimensions");
        }

        double norm = real*real + imaginary*imaginary;

        int N = input.getDataLength();
        for (int i = 0; i < N; i += 2 ) {
            double inReal = input.data[i];
            double inImag = input.data[i+1];

            output.data[i]   = (inReal*real + inImag*imaginary)/norm;
            output.data[i+1] = (inImag*real - inReal*imaginary)/norm;
        }
    }

    /**
     * <p>Performs  element by element division operation with a complex number on the right<br>
     * <br>
     * output<sub>ij</sub> = (real + imaginary*i) / input<sub>ij</sub> <br>
     * </p>
     * @param real Real component of the number it is multiplied by
     * @param imaginary Imaginary component of the number it is multiplied by
     * @param input The right matrix in the multiplication operation. Not modified.
     * @param output Where the results of the operation are stored. Modified.
     */
    public static void elementDivide( double real , double imaginary, CD1Matrix64F input , CD1Matrix64F output )
    {
        if( input.numCols != output.numCols || input.numRows != output.numRows ) {
            throw new IllegalArgumentException("The 'input' and 'output' matrices do not have compatible dimensions");
        }

        int N = input.getDataLength();
        for (int i = 0; i < N; i += 2 ) {
            double inReal = input.data[i];
            double inImag = input.data[i+1];

            double norm = inReal*inReal + inImag*inImag;

            output.data[i]   = (real*inReal + imaginary*inImag)/norm;
            output.data[i+1] = (imaginary*inReal - real*inImag)/norm;
        }
    }

    /**
     * <p>
     * Returns the value of the real element in the matrix that has the minimum value.<br>
     * <br>
     * Min{ a<sub>ij</sub> } for all i and j<br>
     * </p>
     *
     * @param a A matrix. Not modified.
     * @return The the minimum value out of all the real values.
     */
    public static double elementMinReal( CD1Matrix64F a ) {
        final int size = a.getDataLength();

        double min = a.data[0];
        for( int i = 2; i < size; i += 2 ) {
            double val = a.data[i];
            if( val < min ) {
                min = val;
            }
        }

        return min;
    }

    /**
     * <p>
     * Returns the value of the imaginary element in the matrix that has the minimum value.<br>
     * <br>
     * Min{ a<sub>ij</sub> } for all i and j<br>
     * </p>
     *
     * @param a A matrix. Not modified.
     * @return The the minimum value out of all the real values.
     */
    public static double elementMinImaginary( CD1Matrix64F a ) {
        final int size = a.getDataLength();

        double min = a.data[1];
        for( int i = 3; i < size; i += 2 ) {
            double val = a.data[i];
            if( val < min ) {
                min = val;
            }
        }

        return min;
    }

    /**
     * <p>
     * Returns the value of the real element in the matrix that has the minimum value.<br>
     * <br>
     * Min{ a<sub>ij</sub> } for all i and j<br>
     * </p>
     *
     * @param a A matrix. Not modified.
     * @return The the minimum value out of all the real values.
     */
    public static double elementMaxReal( CD1Matrix64F a ) {
        final int size = a.getDataLength();

        double max = a.data[0];
        for( int i = 2; i < size; i += 2 ) {
            double val = a.data[i];
            if( val > max ) {
                max = val;
            }
        }

        return max;
    }

    /**
     * <p>
     * Returns the value of the imaginary element in the matrix that has the minimum value.<br>
     * <br>
     * Min{ a<sub>ij</sub> } for all i and j<br>
     * </p>
     *
     * @param a A matrix. Not modified.
     * @return The the minimum value out of all the real values.
     */
    public static double elementMaxImaginary( CD1Matrix64F a ) {
        final int size = a.getDataLength();

        double max = a.data[1];
        for( int i = 3; i < size; i += 2 ) {
            double val = a.data[i];
            if( val > max ) {
                max = val;
            }
        }

        return max;
    }

    /**
     * <p>
     * Returns the magnitude squared of the complex element with the largest magnitude<br>
     * <br>
     * Max{ |a<sub>ij</sub>|^2 } for all i and j<br>
     * </p>
     *
     * @param a A matrix. Not modified.
     * @return The max magnitude squared
     */
    public static double elementMaxMagnitude2( CD1Matrix64F a ) {
        final int size = a.getDataLength();

        double max = 0;
        for( int i = 0; i < size; ) {
            double real = a.data[i++];
            double imaginary = a.data[i++];

            double m = real*real + imaginary*imaginary;

            if( m > max ) {
                max = m;
            }
        }

        return max;
    }

    /**
     * Sets all the diagonal elements equal to one and everything else equal to zero.
     * If this is a square matrix then it will be an identity matrix.
     *
     * @param mat A square matrix.
     */
    public static void setIdentity( CDenseMatrix64F mat )
    {
        int width = mat.numRows < mat.numCols ? mat.numRows : mat.numCols;

        Arrays.fill(mat.data,0,mat.getDataLength(),0);

        int index = 0;
        int stride = mat.getRowStride();

        for( int i = 0; i < width; i++ , index += stride + 2) {
            mat.data[index] = 1;
        }
    }

}
