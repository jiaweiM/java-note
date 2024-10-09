package mjw.dflib;

import org.dflib.SeriesGroupBy;
import org.dflib.junit5.SeriesGroupByAsserts;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 *
 *
 * @author Jiawei Mao
 * @version 0.1.0
 * @since 09 Oct 2024, 4:01 PM
 */
public class SeriesGroupTest {
    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void group(SeriesType type) {
        SeriesGroupBy<Integer> g = type.createSeries(1, 5, 5, 8, 5).group();
        new SeriesGroupByAsserts(g)
                .expectGroups(1, 5, 8)
                .expectGroupData(1, 1)
                .expectGroupData(5, 5, 5, 5)
                .expectGroupData(8, 8);
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void group_SkipNulls(SeriesType type) {
        SeriesGroupBy<Integer> g = type.createSeries(8, null, 5, 8, 5, null).group();
        new SeriesGroupByAsserts(g)
                .expectGroups(8, 5)
                .expectGroupData(5, 5, 5)
                .expectGroupData(8, 8, 8);
    }

    @ParameterizedTest
    @EnumSource(SeriesType.class)
    public void group_WithHash(SeriesType type) {
        SeriesGroupBy<Integer> g = type.createSeries(1, 16, 5, 8, 7)
                .group((Integer i) -> i % 2);
        new SeriesGroupByAsserts(g)
                .expectGroups(0, 1)
                .expectGroupData(0, 16, 8)
                .expectGroupData(1, 1, 5, 7);
    }
}
