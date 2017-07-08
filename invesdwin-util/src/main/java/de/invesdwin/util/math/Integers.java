package de.invesdwin.util.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import de.invesdwin.norva.apt.staticfacade.StaticFacadeDefinition;
import de.invesdwin.util.lang.ADelegateComparator;
import de.invesdwin.util.lang.Objects;
import de.invesdwin.util.math.internal.AIntegersStaticFacade;
import de.invesdwin.util.math.internal.CheckedCastIntegers;
import de.invesdwin.util.math.internal.CheckedCastIntegersObj;

@StaticFacadeDefinition(name = "de.invesdwin.util.math.internal.AIntegersStaticFacade", targets = {
        CheckedCastIntegers.class, CheckedCastIntegersObj.class, com.google.common.primitives.Ints.class })
@Immutable
public final class Integers extends AIntegersStaticFacade {

    public static final ADelegateComparator<Integer> COMPARATOR = new ADelegateComparator<Integer>() {
        @Override
        protected Comparable<?> getCompareCriteria(final Integer e) {
            return e;
        }
    };

    private Integers() {}

    public static int[] toArray(final Collection<? extends Number> vector) {
        if (vector == null) {
            return null;
        }
        return AIntegersStaticFacade.toArray(vector);
    }

    public static int[] toArrayVector(final Collection<Integer> vector) {
        return toArray(vector);
    }

    public static int[][] toArrayMatrix(final List<? extends List<Integer>> matrix) {
        if (matrix == null) {
            return null;
        }
        final int[][] arrayMatrix = new int[matrix.size()][];
        for (int i = 0; i < matrix.size(); i++) {
            final List<Integer> vector = matrix.get(i);
            arrayMatrix[i] = toArrayVector(vector);
        }
        return arrayMatrix;
    }

    public static List<Integer> asList(final int... vector) {
        if (vector == null) {
            return null;
        } else {
            return AIntegersStaticFacade.asList(vector);
        }
    }

    public static List<Integer> asListVector(final int[] vector) {
        return asList(vector);
    }

    public static List<List<Integer>> asListMatrix(final int[][] matrix) {
        if (matrix == null) {
            return null;
        }
        final List<List<Integer>> matrixAsList = new ArrayList<List<Integer>>(matrix.length);
        for (final int[] vector : matrix) {
            matrixAsList.add(asList(vector));
        }
        return matrixAsList;
    }

    public static Integer max(final Integer first, final Integer second) {
        if (first == null) {
            return second;
        } else if (second == null) {
            return first;
        } else {
            return Math.max(first, second);
        }
    }

    public static Integer min(final Integer first, final Integer second) {
        if (first == null) {
            return second;
        } else if (second == null) {
            return first;
        } else {
            return Math.min(first, second);
        }
    }

    public static Integer avg(final Integer first, final Integer second) {
        final long sum = (long) first + (long) second;
        return (int) sum / 2;
    }

    public static Integer avg(final Integer... values) {
        long sum = 0;
        for (final Integer value : values) {
            sum += value;
        }
        return (int) (sum / values.length);
    }

    public static Integer avg(final Collection<Integer> values) {
        long sum = 0;
        for (final Integer value : values) {
            sum += value;
        }
        return (int) (sum / values.size());
    }

    public static Integer sum(final Collection<Integer> values) {
        int sum = 0;
        for (final Integer value : values) {
            sum += value;
        }
        return sum;
    }

    public static Integer between(final Integer value, final Integer min, final Integer max) {
        return max(min(value, max), min);
    }

    public static <T> int[][] fixInconsistentMatrixDimensions(final int[][] matrix) {
        return fixInconsistentMatrixDimensions(matrix, 0);
    }

    public static <T> int[][] fixInconsistentMatrixDimensions(final int[][] matrix, final int missingValue) {
        final int rows = matrix.length;
        int cols = 0;
        boolean colsInconsistent = false;
        for (int i = 0; i < rows; i++) {
            final int[] vector = matrix[i];
            if (cols != 0 && cols != vector.length) {
                colsInconsistent = true;
            }
            cols = Integers.max(cols, vector.length);
        }
        if (!colsInconsistent) {
            return matrix;
        }
        final int[][] fixedMatrix = new int[rows][];
        for (int i = 0; i < matrix.length; i++) {
            final int[] vector = matrix[i];
            final int[] fixedVector;
            if (vector.length == cols) {
                fixedVector = vector.clone();
            } else {
                fixedVector = new int[cols];
                System.arraycopy(vector, 0, fixedVector, 0, vector.length);
                if (missingValue != 0) {
                    for (int j = vector.length - 1; j < cols; j++) {
                        fixedVector[j] = missingValue;
                    }
                }
            }
            fixedMatrix[i] = fixedVector;
        }
        return fixedMatrix;
    }

    public static <T> Integer[][] fixInconsistentMatrixDimensionsObj(final Integer[][] matrix) {
        return fixInconsistentMatrixDimensionsObj(matrix, 0);
    }

    public static <T> Integer[][] fixInconsistentMatrixDimensionsObj(final Integer[][] matrix, final Integer missingValue) {
        return Objects.fixInconsistentMatrixDimensions(matrix, missingValue);
    }

    public static List<List<Integer>> fixInconsistentMatrixDimensionsAsList(
            final List<? extends List<? extends Integer>> matrix) {
        return fixInconsistentMatrixDimensionsAsList(matrix, 0);
    }

    public static List<List<Integer>> fixInconsistentMatrixDimensionsAsList(final List<? extends List<? extends Integer>> matrix,
            final Integer missingValue) {
        return Objects.fixInconsistentMatrixDimensionsAsList(matrix, missingValue);
    }

}
