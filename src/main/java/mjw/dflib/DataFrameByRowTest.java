package mjw.dflib;

import org.dflib.DataFrame;
import org.dflib.Extractor;
import org.dflib.Printers;
import org.dflib.junit5.DataFrameAsserts;
import org.dflib.series.SingleValueSeries;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 *
 * @author Jiawei Mao
 * @version 0.1.0
 * @since 09 Oct 2024, 7:27 PM
 */
public class DataFrameByRowTest {

    @Test
    public void objectSource() {

        List<From> data = List.of(new From("L1", -1), new From("L2", -2));

        DataFrame df = DataFrame
                .byRow(
                        Extractor.$col(From::s),
                        Extractor.$int(From::i)
                )
                .columnNames("o", "i")
                .ofIterable(data);



        new DataFrameAsserts(df, "o", "i").expectHeight(2)
                .expectIntColumns("i")
                .expectRow(0, "L1", -1)
                .expectRow(1, "L2", -2);
    }

    @Test
    public void objectSource_WithAppender() {

        List<From> data = List.of(new From("L1", -1), new From("L2", -2));

        DataFrame df = DataFrame
                .byRow(
                        Extractor.$col(From::s),
                        Extractor.$int(From::i),
                        Extractor.$long(From::getL),
                        Extractor.$double(From::getD),
                        Extractor.$bool(From::isB)
                )
                .appender()
                .append(new From("a", 1))
                .append(new From("b", 2))
                .append(new From("c", 3))
                .append(data)
                .toDataFrame();

        new DataFrameAsserts(df, "0", "1", "2", "3", "4").expectHeight(5)

                .expectIntColumns("1")
                .expectLongColumns("2")
                .expectDoubleColumns("3")
                .expectBooleanColumns("4")

                .expectRow(0, "a", 1, 10_000_000_001L, 1.01, false)
                .expectRow(1, "b", 2, 10_000_000_002L, 2.01, true)
                .expectRow(2, "c", 3, 10_000_000_003L, 3.01, false)
                .expectRow(3, "L1", -1, 9_999_999_999L, -0.99, false)
                .expectRow(4, "L2", -2, 9_999_999_998L, -1.99, true);
    }

    @Test
    public void objectSource_WithVal() {

        List<From> data = List.of(new From("L1", -1), new From("L2", -2));

        DataFrame df = DataFrame
                .byRow(
                        Extractor.$val("const"),
                        Extractor.$col(From::s)
                )
                .appender()
                .append(new From("a", 1))
                .append(new From("b", 2))
                .append(new From("c", 3))
                .append(data)
                .toDataFrame();

        new DataFrameAsserts(df, "0", "1").expectHeight(5)
                .expectRow(0, "const", "a")
                .expectRow(1, "const", "b")
                .expectRow(2, "const", "c")
                .expectRow(3, "const", "L1")
                .expectRow(4, "const", "L2");

        assertInstanceOf(SingleValueSeries.class, df.getColumn(0));
    }

    @Test
    public void noExtractors() {
        assertThrows(IllegalArgumentException.class, () -> DataFrame
                .byRow()
                .columnNames("a", "b")
                .appender());
    }

    @Test
    public void selectRows() {

        DataFrame df = DataFrame
                .byRow(
                        Extractor.$col(From::s),
                        Extractor.$int(From::i)
                )
                .selectRows(r -> r.get("0").toString().startsWith("a"))
                .appender()
                .append(new From("a", 1))
                .append(new From("ab", 2))
                .append(new From("ca", 3))
                .toDataFrame();

        System.out.println(Printers.tabular.toString(df));
        new DataFrameAsserts(df, "0", "1").expectHeight(2)

                .expectIntColumns("1")

                .expectRow(0, "a", 1)
                .expectRow(1, "ab", 2);
    }

    record From(String s, int i) {

        public double getD() {
            return i + 0.01;
        }

        public long getL() {
            return i + 10_000_000_000L;
        }

        public boolean isB() {
            return i % 2 == 0;
        }
    }
}
