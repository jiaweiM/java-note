package mjw.dflib;

import org.dflib.DataFrame;
import org.dflib.Extractor;
import org.dflib.Printers;
import org.dflib.junit5.DataFrameAsserts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

/**
 *
 *
 * @author Jiawei Mao
 * @version 0.1.0
 * @since 09 Oct 2024, 7:04 PM
 */
public class DataFrameByArrayRowTest {

    @Test
    public void test() {

        List<Object[]> data = List.of(new Object[]{"L1", -1}, new Object[]{"L2", -2});

        DataFrame df = DataFrame
                .byArrayRow(
                        Extractor.$col(a -> a[0]),
                        Extractor.$int(a -> (Integer) a[1])
                )
                .columnNames("o", "i")
                .appender()
                .append("a", 1)
                .append("b", 2)
                .append(data)
                .toDataFrame();

        new DataFrameAsserts(df, "o", "i").expectHeight(4)

                .expectIntColumns("i")

                .expectRow(0, "a", 1)
                .expectRow(1, "b", 2)
                .expectRow(2, "L1", -1)
                .expectRow(3, "L2", -2);
    }

    @Test
    public void implicitExtractors() {

        List<Object[]> data = List.of(new Object[]{"L1", -1}, new Object[]{"L2", -2});

        DataFrame df = DataFrame
                .byArrayRow("o", "i")
                .appender()
                .append("a", 1)
                .append("b", 2)
                .append(data)
                .toDataFrame();

        new DataFrameAsserts(df, "o", "i").expectHeight(4)
                .expectRow(0, "a", 1)
                .expectRow(1, "b", 2)
                .expectRow(2, "L1", -1)
                .expectRow(3, "L2", -2);
    }

    @Test
    public void sample() {

        // fixed seed for predictable results
        Random rnd = new Random(1L);

        List<Object[]> data = List.of(new Object[]{"L1", -1}, new Object[]{"L2", -2});

        DataFrame df = DataFrame
                .byArrayRow(
                        Extractor.$col(a -> a[0]),
                        Extractor.$int(a -> (Integer) a[1])
                )
                .columnNames("o", "i")
                .sampleRows(2, rnd)
                .appender()
                .append("a", 1)
                .append("b", 2)
                .append(data)
                .toDataFrame();

        new DataFrameAsserts(df, "o", "i").expectHeight(2)
                .expectIntColumns("i")
                .expectRow(0, "b", 2)
                .expectRow(1, "L2", -2);
    }

    @Test
    public void selectRows_Sample() {

        // fixed seed for predictable results
        Random rnd = new Random(1L);

        DataFrame df = DataFrame
                .byArrayRow(
                        Extractor.$col(a -> a[0]),
                        Extractor.$int(a -> (Integer) a[1])
                )
                .columnNames("o", "i")
                .selectRows(r -> r.get("o").toString().startsWith("a"))
                .sampleRows(2, rnd)
                .appender()
                .append("a", 1)
                .append("b", 2)
                .append("ab", 3)
                .append("c", 4)
                .append("ac", 5)
                .append("ad", 6)
                .toDataFrame();

        new DataFrameAsserts(df, "o", "i").expectHeight(2)
                .expectIntColumns("i")
                .expectRow(0, "ab", 3)
                .expectRow(1, "ad", 6);
    }

    @Test
    public void ofIterable() {

        List<Object[]> data = List.of(new Object[]{"L1", -1}, new Object[]{"L2", -2});

        DataFrame df = DataFrame
                .byArrayRow(
                        Extractor.$col(a -> a[0]),
                        Extractor.$int(a -> (Integer) a[1])
                )
                .columnNames("o", "i")
                .ofIterable(data);
        System.out.println(Printers.tabular.toString(df));

        new DataFrameAsserts(df, "o", "i")
                .expectHeight(2)
                .expectIntColumns("i")

                .expectRow(0, "L1", -1)
                .expectRow(1, "L2", -2);
    }
}
