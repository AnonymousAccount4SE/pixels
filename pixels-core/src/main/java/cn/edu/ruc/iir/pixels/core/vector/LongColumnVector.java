package cn.edu.ruc.iir.pixels.core.vector;

import java.util.Arrays;

/**
 * LongColumnVector from org.apache.hadoop.hive.ql.exec.vector
 *
 * This class represents a nullable int column vector.
 * This class will be used for operations on all integer types (tinyint, smallint, int, bigint)
 * and as such will use a 64-bit long value to hold the biggest possible value.
 * During copy-in/copy-out, smaller int types will be converted as needed. This will
 * reduce the amount of code that needs to be generated and also will run fast since the
 * machine operates with 64-bit words.
 *
 * The vector[] field is public by design for high-performance access in the inner
 * loop of query execution.
 */
public class LongColumnVector extends ColumnVector
{
    public long[] vector;
    public static final long NULL_VALUE = 1;

    /**
     * Use this constructor by default. All column vectors
     * should normally be the default size.
     */
    public LongColumnVector() {
        this(VectorizedRowBatch.DEFAULT_SIZE);
    }

    /**
     * Don't use this except for testing purposes.
     *
     * @param len the number of rows
     */
    public LongColumnVector(int len) {
        super(len);
        vector = new long[len];
    }

    @Override
    public void add(String value)
    {
        Long v = Long.parseLong(value);
        add(v);
    }

    @Override
    public void add(boolean value)
    {
        add(value ? 1 : 0);
    }

    @Override
    public void add(boolean[] values)
    {
        for (boolean v : values)
        {
            add(v ? 1 : 0);
        }
    }

    @Override
    public void add(long v)
    {
        if (writeIndex >= getLength()) {
            ensureSize(writeIndex * 2, true);
        }
        vector[writeIndex++] = v;
    }

    @Override
    public void add(long[] values)
    {
        for (long v : values) {
            add(v);
        }
    }

    // Copy the current object contents into the output. Only copy selected entries,
    // as indicated by selectedInUse and the sel array.
    public void copySelected(
            boolean selectedInUse, int[] sel, int size, LongColumnVector output) {
        // Output has nulls if and only if input has nulls.
        output.noNulls = noNulls;
        output.isRepeating = false;

        // Handle repeating case
        if (isRepeating) {
            output.vector[0] = vector[0];
            output.isNull[0] = isNull[0];
            output.isRepeating = true;
            return;
        }

        // Handle normal case

        // Copy data values over
        if (selectedInUse) {
            for (int j = 0; j < size; j++) {
                int i = sel[j];
                output.vector[i] = vector[i];
            }
        }
        else {
            System.arraycopy(vector, 0, output.vector, 0, size);
        }

        // Copy nulls over if needed
        if (!noNulls) {
            if (selectedInUse) {
                for (int j = 0; j < size; j++) {
                    int i = sel[j];
                    output.isNull[i] = isNull[i];
                }
            }
            else {
                System.arraycopy(isNull, 0, output.isNull, 0, size);
            }
        }
    }

    // Fill the column vector with the provided value
    public void fill(long value) {
        noNulls = true;
        isRepeating = true;
        vector[0] = value;
    }

    // Fill the column vector with nulls
    public void fillWithNulls() {
        noNulls = false;
        isRepeating = true;
        vector[0] = NULL_VALUE;
        isNull[0] = true;
    }

    // Simplify vector by brute-force flattening noNulls and isRepeating
    // This can be used to reduce combinatorial explosion of code paths in VectorExpressions
    // with many arguments.
    public void flatten(boolean selectedInUse, int[] sel, int size) {
        flattenPush();
        if (isRepeating) {
            isRepeating = false;
            long repeatVal = vector[0];
            if (selectedInUse) {
                for (int j = 0; j < size; j++) {
                    int i = sel[j];
                    vector[i] = repeatVal;
                }
            } else {
                Arrays.fill(vector, 0, size, repeatVal);
            }
            flattenRepeatingNulls(selectedInUse, sel, size);
        }
        flattenNoNulls(selectedInUse, sel, size);
    }

    @Override
    public void setElement(int outElementNum, int inputElementNum, ColumnVector inputVector) {
        if (inputVector.isRepeating) {
            inputElementNum = 0;
        }
        if (inputVector.noNulls || !inputVector.isNull[inputElementNum]) {
            isNull[outElementNum] = false;
            vector[outElementNum] =
                    ((LongColumnVector) inputVector).vector[inputElementNum];
        } else {
            isNull[outElementNum] = true;
            noNulls = false;
        }
    }

    @Override
    public void copyFrom(ColumnVector inputVector)
    {
        if (inputVector instanceof LongColumnVector) {
            LongColumnVector srcVector = (LongColumnVector) inputVector;
            System.arraycopy(srcVector.vector, 0, this.vector, 0, vector.length);
            System.arraycopy(srcVector.isNull, 0, this.isNull, 0, isNull.length);
            this.writeIndex = srcVector.writeIndex;
            this.noNulls = srcVector.noNulls;
            this.isRepeating = srcVector.isRepeating;
        }
    }

    @Override
    public void stringifyValue(StringBuilder buffer, int row) {
        if (isRepeating) {
            row = 0;
        }
        if (noNulls || !isNull[row]) {
            buffer.append(vector[row]);
        } else {
            buffer.append("null");
        }
    }

    @Override
    public void ensureSize(int size, boolean preserveData) {
        super.ensureSize(size, preserveData);
        if (size > vector.length) {
            long[] oldArray = vector;
            vector = new long[size];
            length = size;
            if (preserveData) {
                if (isRepeating) {
                    vector[0] = oldArray[0];
                } else {
                    System.arraycopy(oldArray, 0, vector, 0 , oldArray.length);
                }
            }
        }
    }
}
