package mjw.dflib;

import org.dflib.Series;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 *
 *
 * @author Jiawei Mao
 * @version 0.1.0
 * @since 09 Oct 2024, 4:11 PM
 */
public class SeriesInferredTypeTest {

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void empty(SeriesType type) {
        Series<?> s = type.createSeries();
        assertSame(Object.class, s.getInferredType());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void allNulls(SeriesType type) {
        Series<?> s = type.createSeries(null, null);
        assertSame(Object.class, s.getInferredType());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void someNulls(SeriesType type) {
        Series<?> s = type.createSeries(null, 5);
        assertSame(Integer.class, s.getInferredType());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void sameType(SeriesType type) {
        Series<String> s = type.createSeries("a", "b");
        assertSame(String.class, s.getInferredType());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void subclassSuperclassTypes(SeriesType type) {
        Series<Object> s = type.createSeries(
                new java.sql.Date(System.currentTimeMillis()),
                new java.util.Date(System.currentTimeMillis()));
        assertSame(java.util.Date.class, s.getInferredType());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void commonAncestorTypes(SeriesType type) {
        Series<Object> s = type.createSeries(Long.valueOf(5), Integer.valueOf(6));
        assertSame(Number.class, s.getInferredType());
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void noCommonAncestorTypes(SeriesType type) {
        Series<Object> s = type.createSeries(Long.valueOf(5), "YYY");
        assertSame(Object.class, s.getInferredType());
    }
}
