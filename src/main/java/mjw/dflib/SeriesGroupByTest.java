package mjw.dflib;

import org.dflib.DataFrame;
import org.dflib.Printers;
import org.dflib.Series;
import org.dflib.SeriesGroupBy;
import org.dflib.junit5.DataFrameAsserts;
import org.dflib.junit5.SeriesAsserts;
import org.junit.jupiter.api.Test;

import static org.dflib.Exp.$col;

/**
 *
 *
 * @author Jiawei Mao
 * @version 0.1.0
 * @since 09 Oct 2024, 6:22 PM
 */
public class SeriesGroupByTest {

    @Test
    public void toSeries() {
        SeriesGroupBy<String> gb = Series.of("a", "b", "cd", "e", "fg")
                .group((String s) -> s.length());

        new SeriesAsserts(gb.toSeries()).expectData("a", "b", "e", "cd", "fg");
    }

    @Test
    public void agg() {

        Series<String> aggregated = Series.of("a", "b", "cd", "e", "fg")
                .group((String s) -> s.length())
                .agg($col("").vConcat("_"));

        new SeriesAsserts(aggregated).expectData("a_b_e", "cd_fg");
    }

    @Test
    public void aggMultiple() {

        DataFrame aggregated = Series.of("a", "b", "cd", "e", "fg")
                .group((String s) -> s.length())
                .aggMultiple(
                        $col("first").first(),
                        $col("pipe").vConcat("|"),
                        $col("underscore").vConcat("_"));

        new DataFrameAsserts(aggregated, "first", "pipe", "underscore")
                .expectHeight(2)
                .expectRow(0, "a", "a|b|e", "a_b_e")
                .expectRow(1, "cd", "cd|fg", "cd_fg");
    }

    @Test
    public void aggMultiple_Named() {

        DataFrame aggregated = Series.of("a", "b", "cd", "e", "fg")
                .group((String s) -> s.length())
                .aggMultiple(
                        $col("").first().as("f"),
                        $col("").vConcat("|").as("c1"),
                        $col("").vConcat("_").as("c2"));
        System.out.println(Printers.tabular.toString(aggregated));

        new DataFrameAsserts(aggregated, "f", "c1", "c2")
                .expectHeight(2)
                .expectRow(0, "a", "a|b|e", "a_b_e")
                .expectRow(1, "cd", "cd|fg", "cd_fg");
    }
}
