# DBLib

2024-10-12 添加大量示例 ⭐
2024-09-29 
2024-09-27 ⭐
@author Jiawei Mao

***
## 简介

DFLib (DataFrame Library) 是一个轻量级的 `DataFrame` 的纯 Java 实现。在数据科学和大数据领域，`DataFrame` 是非常常见的结构，提供搜索、过滤、连接、聚合、统计等功能。

在 Python (`pandas`) 和 R 等语言中都有 `DataFrame` 实现，DFLib 项目的目标是提供 `DataFrame` 的纯 Java 实现。它是一个简单的库，且核心库没有依赖项。

添加包：

```xml
<dependency>
    <groupId>org.dflib</groupId>
    <artifactId>dflib</artifactId>
</dependency>
```

创建 `DataFrame`，选择偶数，打印结果：

```java
DataFrame df1 = DataFrame.foldByRow("a", "b", "c")
        .ofStream(IntStream.range(1, 10000));
DataFrame df2 = df1.rows(r -> r.getInt(0) % 2 == 0).select();
System.out.println(Printers.tabular.toString(df2));
```

```
   a    b    c
---- ---- ----
   4    5    6
  10   11   12
  16   17   18
...
9982 9983 9984
9988 9989 9990
9994 9995 9996
1666 rows x 3 columns
```

## 数据结构

DFLib 有两个基本类 `Series` 和 `DataFrame`。

`Series` 包含一维数据，`DataFrame` 包含二维数据。`DataFrame` 的 column 存储为 `Series` 对象。另外还有一个 `Index` 对象，用于存储 `DataFrame` 的 column 名称。`Series` 可以看作对数组的包装，为泛型类。并为基础类型提供优化实现 `IntSeries`, `LongSeries`, `DoubleSeries`, `BooleanSeries`。

`DataFrame` 和 `Series` (包括 `Index`) 都是 immutable 对象，因此对它们的所有操作都返回一个新对象。在实现时，DFLib 在实例之间尽可能共享数据，因此复制对象不会导致显著的性能下降，同时使得 DFLib 线程安全。从而支持并发操作。此外，immutable 保证每个步骤都拥有数据的完整快照，从而简化了数据 pipeline 的调试。

`DataFrame` 是一个 in-memory 表格，由 `Index` header 和多个命名 column 组成。每个 column 都是一个 `Series`，column 名称保存在 `Index` 中。`DataFrame` 可以包含不同类型的 column，因为没有针对任何单一类型进行参数化。

"row" 是一个虚拟概念，因为数据是按 column 组织的，但是为了便于使用，通常提供操作 row 的 API。

创建 `DataFrame` 的方法有几种，下面如何将常见的 Java 集合对象（Array, Stream, Collection, Series）转换为 DataFrame。

> [!NOTE]
>
> `DataFrame` 通常是从外部源，如数据库、CSV 文件等创建，而不是从内存中的对象创建。

## Index

`Index` 与 `Series` 类似，用来表示 `DataFrame` 的 header，并且包含 column 标题到 index 的映射。获取所有 column 标题：

```java
String[] labels = df.getColumnsIndex().toArray();
```

- `equals` 采用 `Arrays.equals` 实现

```java
Index i1 = Index.of("a", "b", "c");
Index i2 = Index.of("a", "b", "c");
Index i3 = Index.of("a", "b", "C");

assertEquals(i1, i1);
assertEquals(i1, i2);
assertNotEquals(i1, i3);
```

- `hashCode` 采用 `Arrays.hashCode` 实现

```java
Index i1 = Index.of("a", "b", "c");
Index i2 = Index.of("a", "b", "c");
Index i3 = Index.of("a", "b", "C");

assertEquals(i1.hashCode(), i1.hashCode());
assertEquals(i1.hashCode(), i2.hashCode());
assertNotEquals(i1.hashCode(), i3.hashCode());
```

### 创建 Index

- 使用 enum 创建

```java
enum E1 {
    a, b, c
}

Index i = Index.of(E1.class);
IndexAsserts.expect(i, "a", "b", "c");
```

### expand

```java
Index expand(String... values);
```

添加新的值，重复值加 `_` 以保证 unique。

```java
Index i = Index.of("a", "b", "c")
        .expand("d", "e", "d", "a");
new SeriesAsserts(i.toSeries())
        .expectData("a", "b", "c", "d", "e", "d_", "a_");
```

### positions

```java
public int[] positions(Predicate<String> condition);
public int[] positions(String... value);
```

返回指定值的位置。

- String... 为参数

```java
Index i = Index.of("a", "b", "c", "d");

assertArrayEquals(new int[0], i.positions());
assertArrayEquals(new int[]{0, 2}, i.positions("a", "c"));
assertArrayEquals(new int[]{2, 0}, i.positions("c", "a"));
```

- Predicate 为参数

返回满足条件的值的位置。

```java
Index i = Index.of("a", "b", "c", "d");
assertArrayEquals(new int[]{2, 3}, i.positions(c -> c.charAt(0) >= 'c'));
```

- 若包含未知值，抛出错误

```java
Index i = Index.of("a", "b");
assertThrows(IllegalArgumentException.class, () -> i.positions("a", "c"));
```

### positionsExcept

```java
public int[] positionsExcept(String... exceptVals);
public int[] positionsExcept(int... exceptPositions);
```

返回指定值以外，余下值的位置。

```java
assertArrayEquals(new int[]{0, 1, 2, 3}, i.positionsExcept(new String[0]));
assertArrayEquals(new int[]{1, 3}, i.positionsExcept("a", "c"));
assertArrayEquals(new int[0], i.positionsExcept("a", "b", "c", "d"));
```

- 以 int... 为参数，

```java
Index i = Index.of("a", "b", "c", "d");

assertArrayEquals(new int[]{0, 1, 2, 3}, i.positionsExcept(new int[0]));
assertArrayEquals(new int[]{0, 2}, i.positionsExcept(1, 3));
assertArrayEquals(new int[0], i.positionsExcept(0, 1, 3, 2));
```

### selectRange

```java
public Index selectRange(int fromInclusive, int toExclusive)
```

选择指定范围的值。

- 选择 [1,3)

```java
Index i = Index.of("a", "b", "c", "d").selectRange(1, 3);
IndexAsserts.expect(i, "b", "c");
```

- 超出范围报错

```java
assertThrows(IllegalArgumentException.class, () -> Index.of("a", "b", "c", "d")
             	.selectRange(0, 5));
```

### toSeries

```java
Series<String> s = Index.of("a", "b", "c", "d").toSeries();
new SeriesAsserts(s).expectData("a", "b", "c", "d");
```

## Series

### Series 类型

![image-20240930135610042](./images/image-20240930135610042.png)

`Series` 的实现包括通用实现 `ObjectSeries` 和针对基础类型的优化实现 `LongSeries`, `IntSeries`, `DoubleSeries` 和 `BooleanSeries`。

针对这些实现，又有不同的实现类型：

- `SingleValueSeries`: 只包含一个重复值的 `Series`
- `ArraySeries`: 采用数组实现的 `Series`
- `ArrayRangeSeries`: 采用数组的一个 slice 定义的 `Series`
- `RangeSeries`: 采用另一个 `Series` 的 slice 定义的 `Series`
- `IndexedSeries`: 以 `IntSeries` 为索引从另一个 `Series` 定义的 `Series`

另外还有几个特殊的 `Series`:

- `EmptySeries`: 不包含任何值的 `Series`
- `RowMappedSeries`: 将 `DataFrame` 的 row 根据映射函数生成的值创建 `Series`
- `ColumnMappedSeries`: 将 `Series` 的值根据映射函数生成新的 `Series`
- `ByRowSeries`: 
- `OffsetSeries`: 

### 创建 Series

- 创建基础类型 `Series`

```java
static BooleanSeries ofBool(boolean... bools); 
static IntSeries ofInt(int... ints);
static DoubleSeries ofDouble(double... doubles);
static LongSeries ofLong(long... longs);
```

- 创建泛型 `Series`

```java
static <T> Series<T> of(T... data);
static <T> Series<T> ofIterable(Iterable<T> data);
```

- 创建填充相同元素的 `Series`

```java
static <T> Series<T> ofVal(T value, int size);
```

#### of

```java
static <T> Series<T> of(T... data);
```

以数组为参数创建 `Series`。

```java
new SeriesAsserts(Series.of()).expectData();
new SeriesAsserts(Series.of("a")).expectData("a");
new SeriesAsserts(Series.of("a", "b")).expectData("a", "b");
```

#### ofIterable

```java
static <T> Series<T> ofIterable(Iterable<T> data);
```

以 `Iterable` 为参数创建 `Series`。

```java
Iterable<String> it = () -> asList("a", "c", "b").iterator();
new SeriesAsserts(Series.ofIterable(it)).expectData("a", "c", "b");
```

- `List` 实现 `Iterable` 接口，可以直接作为参数

```java
new SeriesAsserts(Series.ofIterable(asList("a", "c", "b")))
    			.expectData("a", "c", "b");
```

#### byElement

对长度不可预测的序列，使用 `Series.byElement()`。例如，从 `InputStream` 逐行读取字符串：

```java
SeriesAppender<String, String> appender = Series
        .byElement(Extractor.<String>$col())
        .appender();
Scanner scanner = new Scanner(System.in);
while (scanner.hasNext()) {
    appender.append(scanner.next());
}
Series<String> s = appender.toSeries();
```

`Series.byElement(Extractor.<String>$col()).appender()` 创建收集数据的 `SeriesByElementBuilder`。对 primitive 数据可以使用 `Extractor.$int(...)`, `Extractor.$long(..)` 等。

`appender.append()` 逐个收集数据。

- 创建 `String` 类型 `Series`

```java
Series<String> s = Series
        .byElement(Extractor.<String>$col())
        .appender()
        .append(List.of("a", "c", "e"))
        .toSeries();

new SeriesAsserts(s).expectData("a", "c", "e");
```

- 可以指定 capacity，合适的 capacity 值可以减少重新创建数组的次数

```java
Series<String> s = Series
        .byElement(Extractor.<String>$col())
        .capacity(1)
        .appender()
        .append("a")
        .append("b")
        .append("c")
        .append(List.of("d", "e"))
        .toSeries();

new SeriesAsserts(s).expectData("a", "b", "c", "d", "e");
```

- 类型转换

```java
Series<Integer> s = Series
        .byElement(Extractor.$int((String str) -> Integer.parseInt(str)))
        .appender()
        .append(List.of("1", "55", "6"))
        .toSeries();

assertTrue(s instanceof IntSeries);
new SeriesAsserts(s).expectData(1, 55, 6);
```



### shift

```java
default Series<T> shift(int offset)
default Series<T> shift(int offset, T filler)
```

移位操作。`offset` 为正数表示所有值向右移动，负数表示向左移动。

移位操作产生的空位用 `filler` 填充，`filler` 默认为 `null`。

- 默认用 `null` 填充

```java
Series<String> s = type.createSeries("a", "b", "c", "d").shift(2);
new SeriesAsserts(s).expectData(null, null, "a", "b");
```

- 自定义填充值 "X"

```java
Series<String> s = type.createSeries("a", "b", "c", "d").shift(2, "X");
new SeriesAsserts(s).expectData("X", "X", "a", "b");
```

- 负数表示向左移位

```java
Series<String> s = type.createSeries("a", "b", "c", "d").shift(-2, "X");
new SeriesAsserts(s).expectData("c", "d", "X", "X");
```

- 0 表示不移位

```java
Series<String> s = type.createSeries("a", "b", "c", "d").shift(0, "X");
new SeriesAsserts(s).expectData("a", "b", "c", "d");
```

### first

```java
default T first();
```

返回 `Series` 的第一个值，空 `Series` 返回 null.

```java
String f1 = type.createSeries("a", "b", "cd", "e", "fg").first();
assertEquals("a", f1);

Object f2 = type.createSeries().first();
assertNull(f2);
```

### head 和 tail

```java
Series<T> head(int len);
Series<T> tail(int len);
```

返回包含前/后 `len` 个元素的 `Series`：

- 如果 `Series` 长度小于 `len`，则返回整个 `Series`
- 如果 `len` 为负数，则跳过前 `|len|` 个元素，返回余下元素

`Series<T>`, `BooleanSeries`, `IntSeries`, `LongSeries` 和 `DoubleSeries` 均有此功能。

#### head

- 返回前 2 个元素

```java
Series<String> s = type.createSeries("a", "b", "c").head(2);
new SeriesAsserts(s).expectData("a", "b");
```

- 返回 0 个元素

```java
Series<String> s = type.createSeries("a", "b", "c").head(0);
new SeriesAsserts(s).expectData();
```

- 超出范围，返回原 series

```java
Series<String> s = type.createSeries("a", "b", "c").head(4);
new SeriesAsserts(s).expectData("a", "b", "c");
```

- -2 表示跳过前 2 个

```java
Series<String> s = type.createSeries("a", "b", "c").head(-2);
new SeriesAsserts(s).expectData("c");
```

- 负数超过范围，也返回原 series

```java
Series<String> s = type.createSeries("a", "b", "c").head(-4);
new SeriesAsserts(s).expectData("a", "b", "c");
```

`DoubleSeries`, `LongSeries` 和 `IntSeries` 的功能相同。

#### tail

- 最后 2 个元素

```java
Series<String> s = type.createSeries("a", "b", "c").tail(2);
new SeriesAsserts(s).expectData("b", "c");
```

- 最后 0 个

```java
Series<String> s = type.createSeries("a", "b", "c").tail(0);
new SeriesAsserts(s).expectData();
```

- 超出范围，返回原 series

```java
Series<String> s = type.createSeries("a", "b", "c").tail(4);
new SeriesAsserts(s).expectData("a", "b", "c");
```

- -2 表示跳过后 2 个元素

```java
Series<String> s = type.createSeries("a", "b", "c").tail(-2);
new SeriesAsserts(s).expectData("a");
```

- 负数超过范围，也返回原 series

```java
Series<String> s = type.createSeries("a", "b", "c").tail(-4);
new SeriesAsserts(s).expectData("a", "b", "c");
```

### iterator

```java
default Iterator<T> iterator();
```

基于索引定义的迭代器。

- 迭代所有元素

```java
Iterator<String> it = type.createSeries("a", "b", "c", "d", "e").iterator();

List<String> vals = new ArrayList<>();
while (it.hasNext()) {
    String n = it.next();
    vals.add(n);
}

assertEquals(asList("a", "b", "c", "d", "e"), vals);
```

- 该迭代器只允许访问元素，不允许修改

```java
Iterator<String> it = type.createSeries("a", "b").iterator();

while (it.hasNext()) {
    it.next();
    assertThrows(UnsupportedOperationException.class, () -> it.remove(),
            "Allowed to remove from immutable iterator");
}
```

### eq 和 ne

```java
BooleanSeries eq(Series<?> s); 
BooleanSeries ne(Series<?> s);
```

逐元素比较两个**等长** `Series`，返回 `BooleanSeries`。

非等长 `Series` 的比较抛出 `IllegalArgumentException`。

- 比较 `String` 类型 series

```java
Series<String> s1 = Series.of("a", "b", "n", "c");
Series<String> s2 = Series.of("a", "b", "n", "c");

BooleanSeries cond = s1.eq(s2);
new BooleanSeriesAsserts(cond).expectData(true, true, true, true);
```

```java
Series<String> s1 = Series.of("a", "b", "n", "c");
Series<String> s2 = Series.of("a ", "b", "N", "c");

BooleanSeries cond = s1.eq(s2);
new BooleanSeriesAsserts(cond).expectData(false, true, false, true);
```

- 长度不一致，抛出错误

```java
Series<String> s1 = Series.of("a", "b", "n", "c");
Series<String> s2 = Series.of("a", "b", "n");

assertThrows(IllegalArgumentException.class, () -> s1.eq(s2));
```

- 不等

```java
Series<String> s1 = Series.of("a", "b", "n", "c");
Series<String> s2 = Series.of("a", "b", "n", "c");

BooleanSeries cond = s1.ne(s2);
new BooleanSeriesAsserts(cond).expectData(false, false, false, false);
```

```java
Series<String> s1 = Series.of("a", "b", "n", "c");
Series<String> s2 = Series.of("a ", "b", "N", "c");

BooleanSeries cond = s1.ne(s2);
new BooleanSeriesAsserts(cond).expectData(true, false, true, false);
```

**BooleanSeries**

- 逐元素比较，返回 `BooleanSeries`

```java
BooleanSeries s1 = Series.ofBool(true, false, true);
BooleanSeries s2 = Series.ofBool(true, false, true);

new SeriesAsserts(s1.eq(s2)).expectData(true, true, true);
```

- 与自身比较

```java
BooleanSeries s = Series.ofBool(true, false, true);
new SeriesAsserts(s.eq(s)).expectData(true, true, true);
```

- `BooleanSeries` 和 `Series<Boolean>`

```java
BooleanSeries s1 = Series.ofBool(true, false, true);
BooleanSeries s2 = Series.ofBool(true, true, true);
new SeriesAsserts(s1.eq(s2)).expectData(true, false, true);

BooleanSeries s1 = Series.ofBool(true, false, true);
Series<Boolean> s2 = Series.of(true, true, true);
new SeriesAsserts(s1.eq(s2)).expectData(true, false, true);
```

- ne

```java
BooleanSeries s1 = Series.ofBool(true, false, true);
BooleanSeries s2 = Series.ofBool(true, true, true);
new SeriesAsserts(s1.ne(s2)).expectData(false, true, false);

BooleanSeries s1 = Series.ofBool(true, false, true);
Series<Boolean> s2 = Series.of(true, true, true);
new SeriesAsserts(s1.ne(s2)).expectData(false, true, false);
```

### position

```java
int position(T value);
```

返回 `value` 第一次出现位置的 index，不存在则返回 -1.

由于大多 `Series` 没有构建索引，所以该方法较慢，时间复杂度为 $O(N)$。

示例：

```java
assertEquals(-1, type.createSeries(3, 4, 2).position(null));
assertEquals(1, type.createSeries(3, 4, 2).position(4));
assertEquals(-1, type.createSeries(3, 4, 2).position(5));
```

### contains

```java
default boolean contains(T value) {
    return position(value) >= 0;
}
```

在 `position()` 的基础上定义的方法，查看 series 是否包含指定值。$O(N)$ 操作，速度慢。

示例：

```java
assertFalse(type.createSeries(3, 4, 2).contains(null));
assertTrue(type.createSeries(3, 4, 2).contains(4));
assertFalse(type.createSeries(3, 4, 2).contains(5));
```

### locate

```java
default BooleanSeries locate(Predicate<T> predicate);
```

查看 series 元素与 `Predicate` 的匹配情况。

例如：

```java
BooleanSeries evens = type.createSeries(3, 4, 2).locate(i -> i % 2 == 0);
new BooleanSeriesAsserts(evens).expectData(false, true, true);
```

基础类型有专门的方法，例如 `locateInt()`:

```java
BooleanSeries s = Series.ofInt(3, 4, 2).locateInt(i -> i % 2 == 0);
new BoolSeriesAsserts(s).expectData(false, true, true);
```

### intersect

```java
Series<T> intersect(Series<? extends T> other);
```

求交集。

> [!WARNING]
>
> 这个交集，不执行去重操作。换言之，返回的 `Series` 可以包含重复元素，只要是交集所含元素。

- 交集包括 null

```java
Series<String> s1 = type.createSeries("a", null, "b");
Series<String> s2 = type.createSeries("b", "c", null);

Series<String> c = s1.intersect(s2);
new SeriesAsserts(c).expectData(null, "b");
```

- 与空集的交集为空集

```java
Series<String> s = type.createSeries("a", "b");
new SeriesAsserts(s.intersect(Series.of())).expectData();
```

- 与自身的交集

```java
Series<String> s = type.createSeries("a", "b");
new SeriesAsserts(s.intersect(s)).expectData("a", "b");
```

**DoubleSeries**

- 与空集的交集为空

```java
DoubleSeries s = new DoubleArraySeries(1, 2);
new SeriesAsserts(s.intersect(Series.of())).expectData();
```

- 与自身的交集为包含相同元素的 `Series`

```java
DoubleSeries s = new DoubleArraySeries(1, 2);
new SeriesAsserts(s.intersect(s)).expectData(1., 2.);
```

- 交集

```java
DoubleSeries s1 = new DoubleArraySeries(5, 6, 7);
Series<Double> s2 = Series.of(6., null, 8.);
new SeriesAsserts(s1.intersect(s2)).expectData(6.);
```

- 基础类型

```java
DoubleSeries s1 = new DoubleArraySeries(5, 6, 7);
DoubleSeries s2 = new DoubleArraySeries(6, 8);
new SeriesAsserts(s1.intersect(s2)).expectData(6.);
```

`IntSeries` 和 `LongSeries` 的性质相同。

**BooleanSeries**

- 与空集的就交集为空集

```java
BooleanSeries s = new BooleanArraySeries(true, false);
new SeriesAsserts(s.intersect(Series.of())).expectData();
```

- 与自身的交集为自身

```java
BooleanSeries s = new BooleanArraySeries(true, false);
Series<Boolean> c = s.intersect(s);
new SeriesAsserts(c).expectData(true, false);
```

- 交集

```java
BooleanSeries s1 = new BooleanArraySeries(true, false, false);
Series<Boolean> s2 = Series.of(false, false);

Series<Boolean> c = s1.intersect(s2);
new SeriesAsserts(c).expectData(false, false); // 包含重复元素
```

- 交集-primitive

```java
BooleanSeries s1 = new BooleanArraySeries(true, false);
BooleanSeries s2 = new BooleanArraySeries(false, false);

Series<Boolean> c = s1.intersect(s2);
new SeriesAsserts(c).expectData(false);
```

### diff

```java
Series<T> diff(Series<? extends T> other);
```

返回一个当前 `Series` 包含而另一个 `Series` 不包含的值构成的 `Series`，即求差集。

- `String` 类型 series 的差集

```java
Series<String> s1 = type.createSeries("a", null, "b");
Series<String> s2 = type.createSeries("b", "c", null);

Series<String> c = s1.diff(s2);
new SeriesAsserts(c).expectData("a");
```

- 与空集的差集为自身

```java
Series<String> s = type.createSeries("a", "b");
assertSame(s, s.diff(Series.of()));
```

- 与自身的差集为空集

```java
Series<String> s = type.createSeries("a", "b");
Series<String> c = s.diff(s);
new SeriesAsserts(c).expectData();
```

**BooleanSeries**

- 与空集的差集为自身

```java
BooleanSeries s = new BooleanArraySeries(true, false);
assertSame(s, s.diff(Series.of()));
```

- 与自身的差集为空集

```java
BooleanSeries s = new BooleanArraySeries(true, false);
new SeriesAsserts(s.diff(s)).expectData();
```

- 差集

```java
BooleanSeries s1 = new BooleanArraySeries(true, false);
Series<Boolean> s2 = Series.of(false, false);

Series<Boolean> c = s1.diff(s2);
new SeriesAsserts(c).expectData(true);
```

- 基础类型差异

```java
BooleanSeries s1 = new BooleanArraySeries(true, false);
BooleanSeries s2 = new BooleanArraySeries(false, false);

Series<Boolean> c = s1.diff(s2);
new SeriesAsserts(c).expectData(true);
```

**DoubleSeries**

- 与空集的差集为自身

```java
DoubleSeries s = new DoubleArraySeries(1, 2);
assertSame(s, s.diff(Series.of()));
```

- 与自身的差集为空集

```java
DoubleSeries s = new DoubleArraySeries(1, 2);
new SeriesAsserts(s.diff(s)).expectData();
```

- 差集

```java
DoubleSeries s1 = new DoubleArraySeries(5, 6, 7);
Series<Double> s2 = Series.of(6., null, 8.);
new SeriesAsserts(s1.diff(s2)).expectData(5., 7.);
```

- 基础类型

```java
DoubleSeries s1 = new DoubleArraySeries(5, 6, 7);
DoubleSeries s2 = new DoubleArraySeries(6, 8);
new SeriesAsserts(s1.diff(s2)).expectData(5., 7.);
```

`IntSeries` 和 `LongSeries` 的功能相同。

### expand

```java
default Series<?> expand(Object... values);
```

在 `Series` 后面添加更多值。

**IntSeries**

- `expand` 支持不同类型值

```java
Series<?> s = Series.ofInt(3, 28).expand("abc");
```

```
3  
28 
abc
```

- `expandInt` 只能添加 int 值

```java
IntSeries s = Series.ofInt(3, 28).expandInt(5);
```

```
 3
28
 5
```

`DoubleSeries`, `LongSeries.expandLong` 也有类似功能。

### replace

- 替换指定位置元素，`positions` 和 `with` 必须等长

```java
Series<T> replace(IntSeries positions, Series<T> with);
```

- 将指定位置元素替换为某个值，`condition` 长度可以小于或大于 `series`

```java
Series<T> replace(BooleanSeries condition, T with);
```

- 按映射关系替换值

```java
Series<T> replace(Map<T, T> oldToNewValues);
```

- 将 `condition` 为 `false` 的位置的元素替换为 `with`

```java
Series<T> replaceExcept(BooleanSeries condition, T with);
```

#### Series-String

- 使用 `BooleanSeries` 指定替换位置

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<String> s1 = Series.of("a", "b", "n", "c").replace(cond, "X");
new SeriesAsserts(s1).expectData("X", "X", "n", "c");
```

- 用 null 替换

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<String> s1 = Series.of("a", "b", "n", "c").replace(cond, null);
new SeriesAsserts(s1).expectData(null, null, "n", "c");
```

- 当 `BooleanSeries` 比 series 短，则替换对齐的部分

```java
BooleanSeries cond = Series.ofBool(true, true, false);

Series<String> s1 = Series.of("a", "b", "n", "c").replace(cond, "X");
new SeriesAsserts(s1).expectData("X", "X", "n", "c");
```

- 使用 `Map` 定义映射关系

```java
Map<String, String> replacement = new HashMap<>();
replacement.put("a", "A");
replacement.put("n", null);

Series<String> s1 = Series.of("a", "b", "n", "c").replace(replacement);
new SeriesAsserts(s1).expectData("A", "b", null, "c");
```

- `replaceExcept` 替换 `BooleanSeries` 值为 false 的元素

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<String> s1 = Series.of("a", "b", "n", "c").replaceExcept(cond, "X");
new SeriesAsserts(s1).expectData("a", "b", "X", "X");
```

- `replaceExcept` 用 null 替换

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<String> s1 = Series.of("a", "b", "n", "c").replaceExcept(cond, null);
new SeriesAsserts(s1).expectData("a", "b", null, null);
```

- `BooleanSeries` 长度大于 series，忽略多余的 boolean 值

```java
BooleanSeries cond = Series.ofBool(true, true, false, false, false);

Series<String> s1 = Series.of("a", "b", "n", "c").replaceExcept(cond, "X");
new SeriesAsserts(s1).expectData("a", "b", "X", "X");
```

- `BooleanSeries` 长度小于 series，在 `replaceExcept` 余下值默认为 false，因此也会被替换

```java
BooleanSeries cond = Series.ofBool(true, true, false);

Series<String> s1 = Series.of("a", "b", "n", "c").replaceExcept(cond, "X");
new SeriesAsserts(s1).expectData("a", "b", "X", "X");
```

#### DoubleSeries

- 按位置替换

```java
Series<Double> s1 = Series.ofDouble(1, 0, 2, -1).replace(
        Series.ofInt(1, 3),
        Series.ofDouble(10, 100));

new SeriesAsserts(s1).expectData(1., 10., 2., 100.);
```

- 用 null 值替换

```java
Series<Double> s1 = Series.ofDouble(1, 0, 2, -1).replace(
        Series.ofInt(1, 3),
        Series.of(10., null));

new SeriesAsserts(s1).expectData(1., 10., 2., null);
```

- 用 `BooleanSeries` 指定替换位置

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<Double> s1 = Series.ofDouble(1.1, 0, 2.05, -1.0015).replace(cond, 5.2);
assertInstanceOf(DoubleSeries.class, s1);
new SeriesAsserts(s1).expectData(5.2, 5.2, 2.05, -1.0015);
```

- `BooleanSeries` 指定替换位置，替换为 null

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<Double> s1 = Series.ofDouble(1.1, 0, 2.05, -1.0015).replace(cond, null);
new SeriesAsserts(s1).expectData(null, null, 2.05, -1.0015);
```

- `BooleanSeries` 如果短了，余下位置默认为 false，即不替换

```java
BooleanSeries cond = Series.ofBool(true, true, false);

Series<Double> s1 = Series.ofDouble(1.1, 0, 2.05, -1.0015).replace(cond, 5.2);
assertInstanceOf(DoubleSeries.class, s1);
new SeriesAsserts(s1).expectData(5.2, 5.2, 2.05, -1.0015);
```

- 用 `Map` 指定替换映射

```java
Series<Double> s1 = Series.ofDouble(1.1, 0., 2., -1.01)
        .replace(Map.of(1.1, -1.5, 2., 15.));
assertInstanceOf(DoubleSeries.class, s1);
new SeriesAsserts(s1).expectData(-1.5, 0., 15., -1.01);

Series<Double> s2 = Series.ofDouble(1.1, 0., 2., -1.01)
        .replace(Collections.singletonMap(2., null));
new SeriesAsserts(s2).expectData(1.1, 0., null, -1.01);
```

- `replaceExcept` 替换 `BooleanSeries` 值为 false 的位置

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<Double> s1 = Series.ofDouble(1.1, 0, 2.05, -1.0015)
        .replaceExcept(cond, 5.2);
assertInstanceOf(DoubleSeries.class, s1);
new SeriesAsserts(s1).expectData(1.1, 0., 5.2, 5.2);
```

- 也可以替换为 null 值

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<Double> s1 = Series.ofDouble(1.1, 0, 2.05, -1.0015)
        .replaceExcept(cond, null);
new SeriesAsserts(s1).expectData(1.1, 0., null, null);
```

- `BooleanSeries` 长点没关系，多的值被忽略

```java
BooleanSeries cond = Series.ofBool(true, true, false, false, false);

Series<Double> s1 = Series.ofDouble(1.1, 0, 2.05, -1.0015)
        .replaceExcept(cond, 5.2);
assertInstanceOf(DoubleSeries.class, s1);
new SeriesAsserts(s1).expectData(1.1, 0., 5.2, 5.2);
```

- `BooleanSeries` 如果短了，因为默认为 false，所以在 `replaceExcept` 中余下值默认被替换

```java
BooleanSeries cond = Series.ofBool(true, true, false);

Series<Double> s1 = Series.ofDouble(1.1, 0, 2.05, -1.0015)
        .replaceExcept(cond, 5.2);
assertInstanceOf(DoubleSeries.class, s1);
new SeriesAsserts(s1).expectData(1.1, 0., 5.2, 5.2);
```

#### BooleanSeries

- 替换指定位置指定元素

```java
Series<Boolean> s1 = Series.ofBool(true, false, false, true).replace(
        Series.ofInt(1, 3),
        Series.ofBool(true, false));

new SeriesAsserts(s1).expectData(true, true, false, false);
```

- 替换为 `null` 值

```java
Series<Boolean> s1 = Series.ofBool(true, false, false, true).replace(
        Series.ofInt(1, 3),
        Series.of(true, null));

new SeriesAsserts(s1).expectData(true, true, false, null);
```

- 根据 `condition` 替换

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<Boolean> s1 = Series.ofBool(true, false, true, true).replace(cond, false);
assertInstanceOf(BooleanSeries.class, s1);
new SeriesAsserts(s1).expectData(false, false, true, true);

Series<Boolean> s2 = Series.ofBool(true, false, true, true).replace(cond, true);
assertInstanceOf(BooleanSeries.class, s2);
new SeriesAsserts(s2).expectData(true, true, true, true);
```

- 如果替换为 `null`，返回 `Series<Boolean>`，而非 `BooleanSeries`

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<Boolean> s1 = Series.ofBool(true, false, true, true).replace(cond, null);
assertFalse(s1 instanceof BooleanSeries);
new SeriesAsserts(s1).expectData(null, null, true, true);
```

- `condition` 长度小于 series

```java
BooleanSeries cond = Series.ofBool(true, true, false);

Series<Boolean> s1 = Series.ofBool(true, false, true, true).replace(cond, false);
assertInstanceOf(BooleanSeries.class, s1);
new SeriesAsserts(s1).expectData(false, false, true, true);

Series<Boolean> s2 = Series.ofBool(true, false, true, true).replace(cond, true);
assertInstanceOf(BooleanSeries.class, s2);
new SeriesAsserts(s2).expectData(true, true, true, true);
```

- 使用 `Map` 定义替换值

```java
Series<Boolean> s1 = Series.ofBool(true, false, true, true)
        .replace(Map.of(true, false, false, true));
assertInstanceOf(BooleanSeries.class, s1);
new SeriesAsserts(s1).expectData(false, true, false, false);

Series<Boolean> s2 = Series.ofBool(true, false, true, true)
        .replace(Collections.singletonMap(true, null));
new SeriesAsserts(s2).expectData(null, false, null, null);
```

- 替换 `condition` 为 `false` 位置的元素

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<Boolean> s1 = Series.ofBool(true, false, true, true)
        .replaceExcept(cond, false);
assertInstanceOf(BooleanSeries.class, s1);
new SeriesAsserts(s1).expectData(true, false, false, false);

Series<Boolean> s2 = Series.ofBool(true, false, true, true)
        .replaceExcept(cond, true);
assertInstanceOf(BooleanSeries.class, s2);
new SeriesAsserts(s2).expectData(true, false, true, true);
```

- 替换 `condition` 为 `false` 位置的元素为 null

```java
BooleanSeries cond = Series.ofBool(true, true, false, false);

Series<Boolean> s1 = Series.ofBool(true, false, true, true).replaceExcept(cond, null);
assertFalse(s1 instanceof BooleanSeries);
new SeriesAsserts(s1).expectData(true, false, null, null);
```

- 当 `condition` 长度超过 `series`，忽略超过的部分

```java
BooleanSeries cond = Series.ofBool(true, true, false, false, false);

Series<Boolean> s1 = Series.ofBool(true, false, true, true)
        .replaceExcept(cond, false);
assertInstanceOf(BooleanSeries.class, s1);
new SeriesAsserts(s1).expectData(true, false, false, false);

Series<Boolean> s2 = Series.ofBool(true, false, true, true)
        .replaceExcept(cond, true);
assertInstanceOf(BooleanSeries.class, s2);
new SeriesAsserts(s2).expectData(true, false, true, true);
```

- 当 `condition` 长度小于 `series`，只替换匹配的部分

```java
BooleanSeries cond = Series.ofBool(true, true, false);

Series<Boolean> s1 = Series.ofBool(true, false, true, true)
        .replaceExcept(cond, false);
assertInstanceOf(BooleanSeries.class, s1);
new SeriesAsserts(s1).expectData(true, false, false, false);

Series<Boolean> s2 = Series.ofBool(true, false, true, true)
        .replaceExcept(cond, true);
assertInstanceOf(BooleanSeries.class, s2);
new SeriesAsserts(s2).expectData(true, false, true, true);
```

### select

- 选择指定位置元素

返回 `Series` 元素位置与索引一一对应。索引超出 `series` 返回抛出 `ArrayIndexOutOfBoundsException`。

```java
Series<T> select(int... positions);
Series<T> select(IntSeries positions);
```

- 根据条件选择

```java
Series<T> select(Condition condition);

Series<T> select(Predicate<T> p);
Series<T> select(BooleanSeries positions);
```

**Series<Integer>**

`Series<Double>`, `Series<Long>` 的性质相同。

- 选择指定位置元素

```java
Series<Integer> s = type.createSeries(3, 4, 2).select(2, 1);
new SeriesAsserts(s).expectData(2, 4);
assertTrue(s instanceof IntSeries);
```

- 不提供索引，返回空

```java
Series<Integer> s = type.createSeries(3, 4, 2).select();
new SeriesAsserts(s).expectData();
assertTrue(s instanceof IntSeries);
```

- index 超出 series 范围，抛出异常

```java
Series<Integer> s = type.createSeries(3, 4, 2).select(0, 3);
assertThrows(ArrayIndexOutOfBoundsException.class, () -> s.materialize());
```

- 负数索引返回 null

```java
Series<Integer> s = type.createSeries(3, 4, 2).select(2, 1, -1);
new SeriesAsserts(s).expectData(2, 4, null);
assertFalse(s instanceof IntSeries);
```

有 `null` 时，不再为 `IntSeries` 类型。

- 使用 `BooleanSeries` 选择

```java
BooleanSeries condition = Series.ofBool(false, true, true);
Series<Integer> s = type.createSeries(3, 4, 2).select(condition);
new SeriesAsserts(s).expectData(4, 2);
assertTrue(s instanceof IntSeries);
```

- 使用 `Predicate` 选择

```java
Series<Integer> s = Series.ofInt(3, 4, 2).select(i -> i > 2);
new SeriesAsserts(s).expectData(3, 4);
assertTrue(s instanceof IntSeries);
```

- `IntSeries` 还有专门的 `selectInt` 方法

```java
Series<Integer> s = Series.ofInt(3, 4, 2).selectInt(i -> i > 2);
new SeriesAsserts(s).expectData(3, 4);
assertTrue(s instanceof IntSeries);
```

**BooleanSeries**

- 选择指定位置元素

```java
Series<Boolean> s = Series.ofBool(true, false, true).select(2, 1);
new SeriesAsserts(s).expectData(true, false);
assertInstanceOf(BooleanSeries.class, s);
```

- 不选择元素

```java
Series<Boolean> s = Series.ofBool(true, false, true).select();
new SeriesAsserts(s).expectData();
assertInstanceOf(BooleanSeries.class, s);
```

- 索引超出返回，抛出 `ArrayIndexOutOfBoundsException`

```java
assertThrows(ArrayIndexOutOfBoundsException.class,
        () -> Series.ofBool(true, false, true).select(0, 3).materialize());
```

- 对负数索引，返回 null

```java
Series<Boolean> s = Series.ofBool(true, false, true).select(2, 1, -1);
new SeriesAsserts(s).expectData(true, false, null);
assertFalse(s instanceof BooleanSeries);
```

- 使用 `BooleanSeries` 选择，要求等长

```java
BooleanSeries condition = Series.ofBool(false, true, true);
Series<Boolean> s = Series.ofBool(true, false, true).select(condition);
new SeriesAsserts(s).expectData(false, true);
assertInstanceOf(BooleanSeries.class, s);
```

### eval

```java
default <V> Series<V> eval(Exp<V> exp) {
    return exp.eval(this);
}
```

执行指定 exp 并返回结果。

例如：

```java
Series<String> s = type.createSeries("x", "b", "c", "a")
        .eval(concat(Exp.$col(""), ", ", $col("")));
new SeriesAsserts(s).expectData("x, x", "b, b", "c, c", "a, a");
```

### map

```java
<V> Series<V> map(ValueMapper<T, V> mapper);
DataFrame map(Index resultColumns, ValueToRowMapper<T> mapper);

BooleanSeries mapAsBool(BoolValueMapper<? super T> converter);
DoubleSeries mapAsDouble(DoubleValueMapper<? super T> converter);
IntSeries mapAsInt(IntValueMapper<? super T> converter);
LongSeries mapAsLong(LongValueMapper<? super T> converter);
```

将当前 series 根据指定函数映射为另一个 series 或 `DataFrame`

- 将 series 转换为另一个等长 series。

```java
Series<String> s = type.createSeries("a", "b", "c").map(String::toUpperCase);
new SeriesAsserts(s).expectData("A", "B", "C");

Series<? extends Number> s = Series.ofInt(3, 28).map(Exp.$int(0).add(10));
new SeriesAsserts(s).expectData(13, 38);
```

- 使用 `map(Index resultColumns, ValueToRowMapper<T> mapper)` 定义多个映射条件，返回 `DataFrame`

```java
DataFrame df = type.createSeries("a", "b", "c")
        .map(Index.of("upper", "is_c"), (v, r) -> r
                .set(0, v.toUpperCase())
                .set(1, v.equals("c")));
```

```
upper  is_c
----- -----
A     false
B     false
C      true
```

- 映射为基础类型，如 `mapAsInt`

```java
Series<Integer> s = Series.ofInt(3, 28).mapAsInt(i -> i + 10);
assertTrue(s instanceof IntSeries);
new IntSeriesAsserts((IntSeries) s).expectData(13, 38);
```

### group

分组功能。

- 根据 series 的值进行分组

```java
SeriesGroupBy<T> group();
```

- 使用指定函数计算 group-key

```java
SeriesGroupBy<T> group(ValueMapper<T, ?> by);
```

- 按值分组

```java
SeriesGroupBy<Integer> g = type.createSeries(1, 5, 5, 8, 5).group();
new SeriesGroupByAsserts(g)
        .expectGroups(1, 5, 8)
        .expectGroupData(1, 1)
        .expectGroupData(5, 5, 5, 5)
        .expectGroupData(8, 8);
```

- 分组时自动跳过 null 值

```java
SeriesGroupBy<Integer> g = type.createSeries(8, null, 5, 8, 5, null).group();
new SeriesGroupByAsserts(g)
        .expectGroups(8, 5)
        .expectGroupData(5, 5, 5)
        .expectGroupData(8, 8, 8);
```

- 自定义 group-key

```java
SeriesGroupBy<Integer> g = type.createSeries(1, 16, 5, 8, 7)
        .group((Integer i) -> i % 2);
new SeriesGroupByAsserts(g)
        .expectGroups(0, 1)
        .expectGroupData(0, 16, 8)
        .expectGroupData(1, 1, 5, 7);
```

### SeriesGroupBy

`group` 分组生成 `SeriesGroupBy` 对象。

- 使用 `toSeries()` 将 `SeriesGroupBy` 转换为 `Series`

这里按长度分组，然后将每个分组的元素依次串起来。

```java
SeriesGroupBy<String> gb = Series.of("a", "b", "cd", "e", "fg")
        .group((String s) -> s.length());

new SeriesAsserts(gb.toSeries()).expectData("a", "b", "e", "cd", "fg");
```

- 使用 `agg` 将每个分组的元素进行聚合

这里使用 `_` 将每个分组里的字符串串联得到新的元素。

```java
Series<String> aggregated = Series.of("a", "b", "cd", "e", "fg")
        .group((String s) -> s.length())
        .agg($col("").vConcat("_"));

new SeriesAsserts(aggregated).expectData("a_b_e", "cd_fg");
```

- 使用 `aggMultiple` 通过多个聚合表达式对每个分组生成多个聚合值，生成 `DataFrame`

```java
DataFrame aggregated = Series.of("a", "b", "cd", "e", "fg")
        .group((String s) -> s.length())
        .aggMultiple(
                $col("first").first(),
                $col("pipe").vConcat("|"),
                $col("underscore").vConcat("_"));
```

```
first pipe  underscore
----- ----- ----------
a     a|b|e a_b_e     
cd    cd|fg cd_fg 
```

- 也可以聚合后重命名

```java
DataFrame aggregated = Series.of("a", "b", "cd", "e", "fg")
        .group((String s) -> s.length())
        .aggMultiple(
                $col("").first().as("f"),
                $col("").vConcat("|").as("c1"),
                $col("").vConcat("_").as("c2"));
```

```
f  c1    c2   
-- ----- -----
a  a|b|e a_b_e
cd cd|fg cd_fg
```



### sortIndex

```java
default IntSeries sortIndex(Comparator<? super T> comparator);
```

根据 `comparator` 进行排序，返回的 `IntSeries` 是满足顺序的元素在 series 中的位置。

当需要使用当前 series 对另一个 series 进行排序时，该方法很有用。

示例：

```java
IntSeries s = type.createSeries("x", "b", "c", "a")
        .sortIndex(Comparator.naturalOrder());
new IntSeriesAsserts(s).expectData(3, 1, 2, 0);
```

基础类型有专门的方法，如 `sortIndexInt`:

```java
IntSeries s = Series.ofInt(5, -1, 5, 3, 28, 1).sortIndexInt();
new IntSeriesAsserts(s).expectData(1, 5, 3, 0, 2, 4); // 默认升序
```

- 降序索引

```java
IntSeries s = Series.ofInt(5, -1, 5, 3, 28, 1).sortIndexInt((i1, i2) -> i2 - i1);
new IntSeriesAsserts(s).expectData(4, 0, 2, 3, 5, 1);
```

### sort

```java
Series<T> sort(Sorter... sorters);
Series<T> sort(Comparator<? super T> comparator);
```

排序。返回排序后的 `Series` 副本。

- 基于 `Comparator` 的排序

```java
Series<String> s = type.createSeries("x", "b", "c", "a")
    	.sort(Comparator.naturalOrder());
new SeriesAsserts(s).expectData("a", "b", "c", "x");
```

- 基于 `Sorter` 的排序

```java
Series<String> s = type.createSeries("x", "b", "c", "a")
    	.sort(Exp.$col(0).desc());
new SeriesAsserts(s).expectData("x", "c", "b", "a");
```

**DoubleSeries**

- `DoubleSeries` 有专门的 `sortDouble()` 用于升序排序

 `IntSeries.sortInt()` 和 `LongSeries.sortLong()`。

```java
DoubleSeries s = Series.ofDouble(5., -1., 5., 3., 28., 1.)
    	.sortDouble();
new DoubleSeriesAsserts(s).expectData(-1., 1., 3., 5., 5., 28.);
```

- `Comparator`

```java
DoubleSeries s = Series.ofDouble(5., -1., 5., 3., 28., 1.)
    	.sort((d1, d2) -> (int) Math.round(d2 - d1));
new DoubleSeriesAsserts(s).expectData(28., 5., 5., 3., 1., -1.);
```

- `Sorter`

```java
DoubleSeries s = Series.ofDouble(5., -1., 5., 3., 28., 1.).sort($double(0).desc());
new DoubleSeriesAsserts(s).expectData(28., 5., 5., 3., 1., -1.);
```

**BooleanSeries**

- 使用 `Comparator` 排序

```java
BooleanSeries s = Series.ofBool(true, false, true, false)
        .sort((b1, b2) -> b1 == b2 ? 0 : b1 ? -1 : 1);

new BooleanSeriesAsserts(s).expectData(true, true, false, false);
```

- 使用 `Sorter` 排序

```java
BooleanSeries s = Series.ofBool(true, false, true, false)
        .sort($bool(0).desc());

new BooleanSeriesAsserts(s).expectData(true, true, false, false);
```

### concat-series

```java
Series<T> concat(Series<? extends T>... other);
```

将两个 `Series` 串联起来。

- 和空 series 串联，返回**自身**

```java
Series<String> s = type.createSeries("a", "b");
assertSame(s, s.concat());
```

- 和自身串联

```java
Series<String> s = type.createSeries("a", "b");
Series<String> c = s.concat(s);
new SeriesAsserts(c).expectData("a", "b", "a", "b");
```

- 多个 `Series` 串联，得到更长的 `Series`

```java
Series<String> s1 = type.createSeries("m", "n");
Series<String> s2 = type.createSeries("a", "b");
Series<String> s3 = type.createSeries("d", "c");

Series<String> c = s1.concat(s2, s3);
new SeriesAsserts(c).expectData("m", "n", "a", "b", "d", "c");
```

对基础类型，有专门的 `concat` 方法，以 `DoubleSeries.concatDouble` 为例：

- 串联空集

```java
DoubleSeries s = new DoubleArraySeries(1, 2);
assertSame(s, s.concatDouble());
```

- 串联自身

```java
DoubleSeries s = new DoubleArraySeries(1, 2);
DoubleSeries c = s.concatDouble(s);
new DoubleSeriesAsserts(c).expectData(1, 2, 1, 2);
```

- 多个 `DoubleSeries` 串联

```java
DoubleSeries s1 = new DoubleArraySeries(34, 23);
DoubleSeries s2 = new DoubleArraySeries(1, 2);
DoubleSeries s3 = new DoubleArraySeries(-1, -6);

DoubleSeries c = s1.concatDouble(s2, s3);
new DoubleSeriesAsserts(c).expectData(34, 23, 1, 2, -1, -6);
```

`IntSeries.concatInt`, 功能相同。

- 对 `Series` 数组或集合，可以使用 `SeriesConcat.concat(..)` 拼接

```java
Collection<Series<String>> ss = asList(
        Series.of("x", "y", "z"),
        Series.of("a"),
        Series.of("m", "n"));

Series<String> sConcat = SeriesConcat.concat(ss);
```

### agg

```java
default <R> Series<R> agg(Exp<R> aggregator);
```

将指定 agg exp 操作应用于 `Series`，返回包含单个值的 `Series`。

- 平均值

```java
Series<Double> s = Series.of(1.4, 5.3, -9.4);
assertEquals(-0.9, Exp.$double("").avg().eval(s).get(0).doubleValue(), 0.0000001);
```

- 串联

```java
String aggregated = type.createSeries("a", "b", "cd", "e", "fg")
        .agg(Exp.$col("").vConcat("_"))
        .get(0);
assertEquals("a_b_cd_e_fg", aggregated);
```

```java
Series<String> s = Series.of("a", "b", "z", "c");
assertEquals("abzc", Exp.$col("").vConcat("").eval(s).get(0));
assertEquals("[a|b|z|c]", Exp.$col("").vConcat("|", "[", "]").eval(s).get(0));
```

- first

```java
Series<String> s = Series.of("a", "b", "z", "c");
assertEquals("a", Exp.$col("").first().eval(s).get(0));
```

- 所有值收集到一个 list

```java
Series<String> s = Series.of("a", "b", "z", "c");
assertEquals(asList("a", "b", "z", "c"), Exp.$col("").list().eval(s).get(0));
```

- 所有值收集到一个 set

```java
Series<String> s = Series.of("a", "b", "z", "c");
assertEquals(new HashSet<>(asList("a", "b", "z", "c")),
        Exp.$col("").set().eval(s).get(0));
```

- max

```java
Series<Integer> s = Series.of(4, 5, -9);
assertEquals(5, Exp.$int("").max().eval(s).get(0));

Series<Integer> s = Series.of(4, 5, -9);
assertEquals(5, Exp.$int("").max().eval(s).get(0).intValue());

Series<Double> s = Series.of(1.4, 5.3, -9.4);
assertEquals(5.3, Exp.$double("").max().eval(s).get(0).doubleValue(), 0.0000001);

Series<Long> s = Series.of(4L, 5L, -9L);
assertEquals(5L, Exp.$long("").max().eval(s).get(0).longValue());
```

- min

```java
Series<Integer> s = Series.of(4, 5, -9);
assertEquals(-9, Exp.$int("").min().eval(s).get(0));
```

- median

```java
Series<Double> s = Series.of(1.4, 5.3, -9.4);
assertEquals(1.4, Exp.$double("").median().eval(s).get(0).doubleValue(), 0.0000001);
```

- sum

```java
Series<Double> s = Series.of(1.4, 5.3, -9.4);
assertEquals(-2.7, Exp.$double("").sum().eval(s).get(0).doubleValue(), 0.0000001);

Series<BigDecimal> s = Series.of(
        new BigDecimal("1.4").setScale(2, RoundingMode.HALF_UP),
        new BigDecimal("5.3").setScale(4, RoundingMode.HALF_UP),
        new BigDecimal("-9.4").setScale(2, RoundingMode.HALF_UP));

assertEquals(BigDecimal.valueOf(-2.7000).setScale(4, RoundingMode.HALF_UP),
        Exp.$decimal("").sum().eval(s).get(0));
```

- 





#### concat-separator

```java
default String concat(String separator) {
    return agg(Exp.$col("").vConcat(separator)).get(0);
}

default String concat(String separator, String prefix, String suffix) {
    return agg(Exp.$col("").vConcat(separator, prefix, suffix)).get(0);
}
```

以指定字符串将 `Series` 的值串联起来，返回 `String`。

- 用 `_` 串联字符串

```java
String concat = type.createSeries("a", "b", "cd", "e", "fg").concat("_");
assertEquals("a_b_cd_e_fg", concat);
```

- 用 `_` 串联字符串，并指定串联后的前缀和后缀

```java
String concat = type.createSeries("a", "b", "cd", "e", "fg").concat("_", "[", "]");
assertEquals("[a_b_cd_e_fg]", concat);
```

#### aggMultiple

```java
default DataFrame aggMultiple(Exp<?>... aggregators);
```

使用多个 agg 表达式，生成多个聚合值，以单行 `DataFrame` 返回结果。

由于 `Series` 只有一个 col，所以所有 `$col(0)` 都指向该 series。

- col-"first" 为 series 的第一个值；
- col-"concat" 用 `|` 串联 series 的值；
- 第二个 col-"concat" 由于名称重复，自动加 `_` 后缀；
- 最后一个 col-"count" 值为 `count()`。

```java
DataFrame aggregated = type.createSeries("a", "b", "cd", "e", "fg")
        .aggMultiple(
                $col(0).first().as("first"),
                $col(0).vConcat("|").as("concat"),
                $col(0).vConcat("_", "[", "]").as("concat"),
                count().as("count"));
```

```
first concat      concat_       count
----- ----------- ------------- -----
a     a|b|cd|e|fg [a_b_cd_e_fg]     5
1 row x 4 columns
```

### valueCounts

```java
DataFrame valueCounts();
```

返回一个 2 列的 `DataFrame`，列出不同值的个数。不包含 `null` 值的个数。

- 没有 null 值

```java
DataFrame counts = type.createSeries("a", "b", "a", "a", "c").valueCounts();
```

```
value count
----- -----
a         3
b         1
c         1
```

- 有 null 值

```java
DataFrame counts = type.createSeries("a", "b", "a", "a", null, "c").valueCounts();
```

```
value count
----- -----
a         3
b         1
c         1
```

- 基础类型

```java
DataFrame counts = Series.ofInt(1, 3, 1, 3, 1, 0).valueCounts();

new DataFrameAsserts(counts, "value", "count")
        .expectHeight(3)
        .expectRow(0, 1, 3)
        .expectRow(1, 3, 2)
        .expectRow(2, 0, 1);
```

### unique

```java
Series<T> unique();
```

返回包含 unique 值的 `Series`。

返回值的顺序与其在 `Series` 中首次出现的顺序一致。

**BooleanSeries**

- true 先出现

```java
BooleanSeries s1 = Series.ofBool(true, false, true, false, true).unique();
new BoolSeriesAsserts(s1).expectData(true, false);
```

- 只有 true

```java
BooleanSeries s1 = Series.ofBool(true, true, true).unique();
new BoolSeriesAsserts(s1).expectData(true);
```

- 只有 false

```java
BooleanSeries s1 = Series.ofBool(false, false, false).unique();
new BoolSeriesAsserts(s1).expectData(false);
```

- false 先出现

```java
BooleanSeries s1 = Series.ofBool(false, true).unique();
new BoolSeriesAsserts(s1).expectData(false, true);
```

**DoubleSeries**

`IntSeries`, `LongSeries` 与 `DoubleSeries` 类似。

- 去重

```java
DoubleSeries s1 = Series.ofDouble(0., -1.1, -1.1, 0., 1.1, 375.05, 
        Double.MAX_VALUE, 5.1, Double.MAX_VALUE).unique();
new DoubleSeriesAsserts(s1).expectData(0., -1.1, 1.1, 375.05, 
        Double.MAX_VALUE, 5.1);
```

- 已经 unique

```java
DoubleSeries s1 = Series.ofDouble(0., -1.1, 1.1, 375.05, 
        Double.MAX_VALUE, 5.1).unique();
new DoubleSeriesAsserts(s1).expectData(0., -1.1, 1.1, 375.05, 
        Double.MAX_VALUE, 5.1);
```

- 只有一个值

```java
DoubleSeries s1 = Series.ofDouble(-1.1).unique();
new DoubleSeriesAsserts(s1).expectData(-1.1);
```

**Series-obj**

```java
Object o1 = new Object();
Object o2 = "__";
Object o3 = new Integer(9);
Object o4 = new Integer(9);

@ParameterizedTest
@EnumSource(SeriesType.class)
public void test(SeriesType type) {
    Series<Object> s1 = type.createSeries(o4, o1, o2, o3, o1).unique();
    new SeriesAsserts(s1).expectData(o4, o1, o2);
}

@ParameterizedTest
@EnumSource(SeriesType.class)
public void alreadyUnique(SeriesType type) {
    Series<Object> s1 = type.createSeries(o4, o1, o2).unique();
    new SeriesAsserts(s1).expectData(o4, o1, o2);
}

@ParameterizedTest
@EnumSource(SeriesType.class)
public void small(SeriesType type) {
    Series<Object> s1 = type.createSeries(o4).unique();
    new SeriesAsserts(s1).expectData(o4);
}
```

### fillNulls

```java
Series<T> fillNulls(T value);
Series<T> fillNullsFromSeries(Series<? extends T> values);
Series<T> fillNullsBackwards();
Series<T> fillNullsForward();
```

- 用 -1 替换 null 值

```java
Series<Integer> s = type.createSeries(1, null, 5, 8, null).fillNulls(-1);
new SeriesAsserts(s).expectData(1, -1, 5, 8, -1);
```

- 用高 index non-null 值替换 null

```java
Series<Integer> s = type.createSeries(null, 1, null, 5, 8, null)
        .fillNullsBackwards();
new SeriesAsserts(s).expectData(1, 1, 5, 5, 8, null);
```

- 用低 index non-null 值替换 null

```java
Series<Integer> s = type.createSeries(null, 1, null, 5, 8, null)
        .fillNullsForward();
new SeriesAsserts(s).expectData(null, 1, 1, 5, 8, 8);
```

### 转换为集合类型

#### toArray

```java
default T[] toArray(T[] a);
```

生成 `Series` 的数组副本。深度复制，修改 `Series` 不应许该数组。

- 生成数组

```java
String[] a = type.createSeries("a", "b", "c", "d", "e").toArray(new String[0]);
assertArrayEquals(new Object[]{"a", "b", "c", "d", "e"}, a);
```

- 修改数组不影响 `Series` 的值

```java
Series<String> s = type.createSeries("a", "b");
String[] a = s.toArray(new String[0]);
a[0] = "c";

new SeriesAsserts(s).expectData("a", "b");
```

- 基础类型之 `toIntArray`

```java
int[] a = new IntArraySeries(1, 2).toIntArray();
assertArrayEquals(new int[]{1, 2}, a);
```

- 底层复制数据，数组与原 `IntSeries` 互不干扰

```java
IntSeries s = new IntArraySeries(1, 2);
int[] a  = s.toIntArray();
a[0] = -1;

new IntSeriesAsserts(s).expectData(1, 2);
```

#### toList

```java
default List<T> toList()
```

生成 `Series` 的 `List` 副本。深度复制，生成的 `List` 为 mutable，修改互不干扰。

- 生成 `List`

```java
List<String> l = type.createSeries("a", "b", "c", "d", "e").toList();
assertEquals(asList("a", "b", "c", "d", "e"), l);
```

- 修改 `List` 不影响 `Series`

```java
Series<String> s = type.createSeries("a", "b");
List<String> l = s.toList();
l.set(0, "c");

assertEquals(asList("c", "b"), l);
new SeriesAsserts(s).expectData("a", "b");
```

#### toSet

```java
default Set<T> toSet();
```

生成 mutable `Set` 副本。

- 生成 `Set`

```java
Set<String> set = type.createSeries("a", "b", "a", "d", "b").toSet();
assertEquals(new HashSet<>(asList("a", "b", "d")), set);
```

- 修改互不干扰

```java
Series<String> s = type.createSeries("a", "b");
Set<String> set = s.toSet();
set.remove("b");

assertEquals(new HashSet<>(asList("a")), set);
new SeriesAsserts(s).expectData("a", "b");
```

### sample

```java
Series<T> sample(int size);
Series<T> sample(int size, Random random);
```

从 series 随机抽样。

```java
Series<String> sample = type.createSeries("a", "b", "c", "d", "e", "f", "g")
        .sample(4, new Random(5));
new SeriesAsserts(sample).expectData("d", "b", "a", "g");

IntSeries sample = Series.ofInt(15, 4, 2, 6, 7, 12, 88, 9)
    	.sample(4, new Random(6));
new SeriesAsserts(sample).expectData(88, 15, 9, 7);
```

### 类型

#### 类型转换

```java
<S> Series<S> castAs(Class<S> type);

default BooleanSeries castAsBool();
default DoubleSeries castAsDouble();
default IntSeries castAsInt();
default LongSeries castAsLong();

default <S> Series<S> unsafeCastAs(Class<S> type);
```

- `unsafeCastAs` 直接转换类型，不做任何检查。如果类型不符合，在后续数据分析可能抛出 `ClassCastExceptions`

该方法适合已经直到类型的情况。

```java
IntSeries s = new IntArraySeries(1, 2);
assertDoesNotThrow(() -> s.unsafeCastAs(String.class));
assertDoesNotThrow(() -> s.unsafeCastAs(Integer.class));
```

转换时不会抛出错误。

- `castAs` 将 `Series` 转换为兼容的类型，不兼容类型抛出 `ClassCastException`

```java
Series<?> s = type.createSeries("s1", "s2");
assertDoesNotThrow(() -> s.castAs(String.class)); // 转换为 String 类型
assertThrows(ClassCastException.class, () -> s.castAs(Integer.class));

// IntSeries
IntSeries s = new IntArraySeries(1, 2);
assertDoesNotThrow(() -> s.castAs(Integer.class));
assertDoesNotThrow(() -> s.castAs(Integer.TYPE));
assertThrows(ClassCastException.class, () -> s.castAs(String.class));
```

- `Integer` 和 `Number` 为继承关系，可以安全转换，但 `Integer` 和 `Long` 为兄弟类型，转换报错

同时适用于 `IntSeries`, `DoubleSeries` 等。

```java
Series<?> s = type.createSeries(1, 2);
assertDoesNotThrow(() -> s.castAs(Integer.class));
assertDoesNotThrow(() -> s.castAs(Number.class));
assertThrows(ClassCastException.class, () -> s.castAs(Long.class));
```

- 转换为基础类型

只能转换为完全匹配的类型，否则抛出错误。

```java
IntSeries s = new IntArraySeries(1, 2);
assertDoesNotThrow(() -> s.castAsInt());
assertThrows(ClassCastException.class, () -> s.castAsBool());
assertThrows(ClassCastException.class, () -> s.castAsDouble());
assertThrows(ClassCastException.class, () -> s.castAsLong());
```

#### inferredType

```java
Class<?> getInferredType();
```

返回最接近的 `Series` 类型。

如果所有值为 `null`，返回 `Object.class`。首次调用该方法好很慢， 因为该方法需要扫描 series 的所有值。

- 空 series 返回 `Object.class`

```java
Series<?> s = type.createSeries();
assertSame(Object.class, s.getInferredType());
```

- 全部为 null 也返回 `Object.class`

```java
Series<?> s = type.createSeries(null, null);
assertSame(Object.class, s.getInferredType());
```

- null 和 `Integer` 同时存在，返回 `Integer`

```java
Series<?> s = type.createSeries(null, 5);
assertSame(Integer.class, s.getInferredType());
```

- `String` 类型

```java
Series<String> s = type.createSeries("a", "b");
assertSame(String.class, s.getInferredType());
```

- 父子类型同时存在，返回父类型

```java
Series<Object> s = type.createSeries(
        new java.sql.Date(System.currentTimeMillis()),
        new java.util.Date(System.currentTimeMillis()));
assertSame(java.util.Date.class, s.getInferredType());
```

- 两个子类型，返回公共父类型

```java
Series<Object> s = type.createSeries(Long.valueOf(5), Integer.valueOf(6));
assertSame(Number.class, s.getInferredType());
```

- 没有公共父类型，返回 `Object.class`

```java
Series<Object> s = type.createSeries(Long.valueOf(5), "YYY");
assertSame(Object.class, s.getInferredType());
```



### IntSeries

#### add

```java
default IntSeries add(IntSeries s);
```

用于两个等长 `IntSeries` 的加法运算。

`DoubleSeries.add` 和 `LongSeries.add` 功能相同。

例如：

```java
IntSeries s0 = Series.ofInt(1, 2, 3, 4, 5, 6);
IntSeries s = Series.ofInt(3, 28, 15, -4, 3, 11).add(s0);
```

```
 4
30
18
 0
 8
17
```

#### indexInt

```java
IntSeries indexInt(IntPredicate predicate);
```

返回 `IntSeries` 中满足条件元素的索引。

- 偶数的索引

```java
IntSeries s = new IntArraySeries(3, 4, 2).indexInt(i -> i % 2 == 0);
new IntSeriesAsserts(s).expectData(1, 2);
```

- 全部

```java
IntSeries s = new IntArraySeries(3, 4, 2).indexInt(i -> true);
new IntSeriesAsserts(s).expectData(0, 1, 2);
```

- 空

```java
IntSeries s = new IntArraySeries(3, 4, 2).indexInt(i -> false);
new IntSeriesAsserts(s).expectData();
```

#### mul

```java
IntSeries mul(IntSeries s);
```

逐元素乘。长度必须相同。

```java
IntSeries s0 = Series.ofInt(1, 2, 3, 4, 5, 6);
IntSeries s = Series.ofInt(3, 28, 15, -4, 3, 11).mul(s0);
new IntSeriesAsserts(s).expectData(3, 56, 45, -16, 15, 66);
```

### BooleanSeries

#### 逻辑操作

```java
static BooleanSeries andAll(BooleanSeries... series);
tatic BooleanSeries orAll(BooleanSeries... series);
```

逐元素逻辑**与**操作。

要求所有 `series` 的长度相同。

> [!TIP]
>
> `andAll` 和 `orAll` 用于多个 `BooleanSeries` 的逻辑操作。
>
> `and` 和 `or` 用于当前 `Series` 和另一个 `Series` 的逻辑操作。

- 逻辑与操作-`andAll`

```java
BooleanSeries and = BooleanSeries.andAll(
        Series.ofBool(true, false, true, false),
        Series.ofBool(false, true, true, false)
);
new BooleanSeriesAsserts(and).expectData(false, false, true, false);
```

- 逻辑或操作-`orAll`

```java
BooleanSeries or = BooleanSeries.orAll(
        Series.ofBool(true, false, true, false),
        Series.ofBool(false, true, true, false));
new BooleanSeriesAsserts(or).expectData(true, true, true, false);
```

- 逻辑与操作-`and`

```java
BooleanSeries s = Series.ofBool(true, false, true, false);
BooleanSeries and = s.and(Series.ofBool(false, true, true, false));
new BooleanSeriesAsserts(and).expectData(false, false, true, false);
```

- 逻辑或操作-`or`

```java
BooleanSeries s = Series.ofBool(true, false, true, false);
BooleanSeries or = s.or(Series.ofBool(false, true, true, false));
new BooleanSeriesAsserts(or).expectData(true, true, true, false);
```

- 逻辑非操作-`not`

```java
BooleanSeries s = Series.ofBool(true, false, true, false);
BooleanSeries and = s.not();
new BooleanSeriesAsserts(and).expectData(false, true, false, true);
```

#### firstTrue

```java
int firstTrue();
```

返回第一个 `true` 的 index，若所有值均为 `false`，返回 -1.

```java
assertEquals(-1, Series.ofBool().firstTrue());
assertEquals(0, Series.ofBool(true, true, true).firstTrue());
assertEquals(2, Series.ofBool(false, false, true).firstTrue());
assertEquals(-1, Series.ofBool(false, false, false).firstTrue());
```

#### isTrue 和 isFalse

```java
boolean isTrue();
boolean isFalse();
```

是否所有元素为 true 或 false。

```java
assertTrue(Series.ofBool().isTrue());
assertTrue(Series.ofBool(true, true, true).isTrue());
assertFalse(Series.ofBool(true, false, true).isTrue());

assertFalse(Series.ofBool().isFalse());
assertTrue(Series.ofBool(false, false, false).isFalse());
assertFalse(Series.ofBool(true, false, true).isFalse());
```

#### concatBool

```java
BooleanSeries concatBool(BooleanSeries... other)
```

将多个 `BooleanSeries` 串联为一个。

- 如果参数为空，返回自身

```java
BooleanSeries s = new BooleanArraySeries(true, false);
assertSame(s, s.concatBool());
```

- 和自身串联

```java
BooleanSeries s = new BooleanArraySeries(true, false);
BooleanSeries c = s.concatBool(s);
new BoolSeriesAsserts(c).expectData(true, false, true, false);
```

- 多个 `BooleanSeries` 串联

```java
    BooleanSeries s1 = new BooleanArraySeries(true, false);
    BooleanSeries s2 = new BooleanArraySeries(false, false);
    BooleanSeries s3 = new BooleanArraySeries(true, true);

    BooleanSeries c = s1.concatBool(s2, s3);
    new BoolSeriesAsserts(c).expectData(true, false, false, false, true, true);
```

## DataFrame

`DataFrame` 接口只有一个实现，即 `ColumnDataFrame` 类。

### DataFrame 属性

- 名称

```java
String getName();
```

`DataFrame` 的名称默认为 `null`，可以使用 `as` 设置。

- row 数

```java
int height()
```

```java
DataFrame df = DataFrame.foldByRow("a").of(
        1,
        2
);

assertEquals(2, df.height());
```

- column 数

```java
int width()
```

```java
DataFrame df = DataFrame.foldByRow("a").of(
        1,
        2
);

assertEquals(1, df.width());
```

### 创建 DataFrame

`DataFrame` 包含三部分信息：

- 名称
- 标题行：用 `Index` 对象表示
- 数据，每个 column 用 `Series` 表示

可以直接使用 `ColumnDataFrame` 构造函数创建 `DataFrame`。为了便于创建 `DataFrame` ，DFLib 提供了多个 Builder 类，且都可以从 `DataFrame` 接口的 `static` 方法访问，

#### ColumnDataFrame 构造函数

- 指定表格名称、标题行和数据

```java
ColumnDataFrame df = new ColumnDataFrame(
        "n1",
        Index.of("a", "b"),
        Series.ofInt(1, 2),
        Series.ofInt(3, 4));

new DataFrameAsserts(df, "a", "b").expectHeight(2);
assertEquals("n1", df.getName());
```

- 仅指定标题行

```java
ColumnDataFrame df = new ColumnDataFrame(null, Index.of("a", "b"));
new DataFrameAsserts(df, "a", "b").expectHeight(0);
```

#### 创建空 DataFrame

```java
static DataFrame empty(String... columnNames);
static DataFrame empty(Index columnsIndex);
```

#### byArrayRow

逐行添加数据，每个 row 对应一个数组。

- 示例

```java
DataFrame df = DataFrame
        .byArrayRow("name", "age") // 创建 builder，每个 row 以可变数组指定
        .appender() // 使用默认设置的 builder 创建一个 appender
        .append("Joe", 18)   // 逐行添加数据
        .append("Andrus", 49)
        .append("Joan", 32)
        .toDataFrame();
```

```
name   age
------ ---
Joe     18
Andrus  49
Joan    32
3 rows x 2 columns
```

- 示例 2，指定 extractor

```java
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
```

```
o   i
-- --
a   1
b   2
L1 -1
L2 -2
4 rows x 2 columns
```

- 隐式 extractor

```java
List<Object[]> data = List.of(new Object[]{"L1", -1}, new Object[]{"L2", -2});

DataFrame df = DataFrame
        .byArrayRow("o", "i")
        .appender()
        .append("a", 1)
        .append("b", 2)
        .append(data)
        .toDataFrame();
```

```
o   i
-- --
a   1
b   2
L1 -1
L2 -2
4 rows x 2 columns
```

- 随机抽样

```java
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
```

```
o   i
-- --
b   2
L2 -2
2 rows x 2 columns
```

- 选择 rows 后再随机抽样

```java
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
```

```
o  i
-- -
ab 3
ad 6
2 rows x 2 columns
```

- ofIterable

```java
List<Object[]> data = List.of(new Object[]{"L1", -1}, new Object[]{"L2", -2});

DataFrame df = DataFrame
        .byArrayRow(
                Extractor.$col(a -> a[0]),
                Extractor.$int(a -> (Integer) a[1])
        )
        .columnNames("o", "i")
        .ofIterable(data);
```

```
o   i
-- --
L1 -1
L2 -2
2 rows x 2 columns
```



#### byRow

```java
static <T> DataFrameByRowBuilder<T, ?> byRow(Extractor<T, ?>... extractors) {
    return new DataFrameByRowBuilder<>(extractors);
}
```

从集合提取数据，按行填充。

- 从对象 list 中提取对象属性来创建

```java
record Person(String name, int age) {
}

List<Person> people = List.of(
        new Person("Joe", 18),
        new Person("Andrus", 49),
        new Person("Joan", 32));

DataFrame df = DataFrame
        .byRow( // builder 以 Extractor 数组开始
                Extractor.$col(Person::name),
                Extractor.$int(Person::age))
        .columnNames("name", "age") // 指定 column 名称，如果忽略，则自动命名
        .appender() // 创建 row-by-row appender
        .append(people) // 添加 list 数据
        .toDataFrame();
```

```
name   age
------ ---
Joe     18
Andrus  49
Joan    32
3 rows x 2 columns
```

- 定义对象

```java
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
```

- `ofIterable` 一次完成创建

```java
List<From> data = List.of(new From("L1", -1), new From("L2", -2));

DataFrame df = DataFrame
        .byRow(
                Extractor.$col(From::s),
                Extractor.$int(From::i)
        )
        .columnNames("o", "i")
        .ofIterable(data);
```

```
o   i
-- --
L1 -1
L2 -2
2 rows x 2 columns
```

- `appender()` 逐个添加

```java
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
```

```
0   1           2     3     4
-- -- ----------- ----- -----
a   1 10000000001  1.01 false
b   2 10000000002  2.01  true
c   3 10000000003  3.01 false
L1 -1  9999999999 -0.99 false
L2 -2  9999999998 -1.99  true
5 rows x 5 columns
```

- extractor 用常量

```java
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
```

```
0     1 
----- --
const a 
const b 
const c 
const L1
const L2
5 rows x 2 columns
```

- 不设置 extractor 会报错

```java
assertThrows(IllegalArgumentException.class, () -> DataFrame
        .byRow()
        .columnNames("a", "b")
        .appender());
```

- `selectRows` 过滤数据

```java
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
```

```
0  1
-- -
a  1
ab 2
2 rows x 2 columns
```



#### foldByRow

```java
static DataFrameFoldByRowBuilder foldByRow(String... columnLabels);
static DataFrameFoldByRowBuilder foldByRow(Index columnIndex);
```

`foldByRow` 与 `foldByColumn` 类似，均支持数组、Stream 和 `Iterable` 为数据源，并对基础类型提供了专门的方法。

`foldByRow` 以指定 column-labels 创建 `DataFrameFoldByRowBuilder`，该 builder 以 row-by-row 的方式折叠数据为 `DataFrame`，其 `of` 系列方法支持多种数据源。

```java
DataFrame of(Object... data);
<T> DataFrame ofIterable(Iterable<T> iterable);
<T> DataFrame ofStream(Stream<T> stream);

DataFrame ofDoubles(double padWith, double... data);
DataFrame ofStream(DoubleStream stream);
DataFrame ofStream(double padWith, DoubleStream stream);

DataFrame ofInts(int padWith, int... data);
DataFrame ofStream(IntStream stream);
DataFrame ofStream(int padWith, IntStream stream);

DataFrame ofLongs(long padWith, long... data);
DataFrame ofStream(LongStream stream);
DataFrame ofStream(long padWith, LongStream stream);
```

基础类型不允许 `null` 值，`padWith` 用于指定补齐末尾 row 的默认值，默认为 0。

- 一个 row，以数组为数据源

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, 2);
```

```
a b
- -
1 2
1 row x 2 columns
```

- 如果末尾 row 不完整，填充 null

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, 2, 3);
```

```
a    b
- ----
1    2
3 null
2 rows x 2 columns
```

- 以 `Stream` 为数据源

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .ofStream(Stream.of("a", 1, "b", 2, "c", 3));
```

```
a b
- -
a 1
b 2
c 3
3 rows x 2 columns
```

- 末尾 row 不完整的 `Stream` 以 null 补齐

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .ofStream(Stream.of("a", 1, "b", 2, "c"));
```

```
a    b
- ----
a    1
b    2
c null
3 rows x 2 columns
```

- 以 iterable 为数据源

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .ofIterable(List.of("a", 1, "b", 2, "c", 3));
```

```
a b
- -
a 1
b 2
c 3
3 rows x 2 columns
```

- 末尾 row 不完整的 `Iterable` 以 null 补齐

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .ofIterable(List.of("a", 1, "b", 2, "c"));
```

```
a    b
- ----
a    1
b    2
c null
3 rows x 2 columns
```

- 以 `IntStream` 为数据源

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .ofStream(-9999, IntStream.of(-1, 1, 0, 2, 5, 3));
```

```
 a b
-- -
-1 1
 0 2
 5 3
3 rows x 2 columns
```

- 以 `IntStream` 为数据源，末尾 row 用 -9999 补齐

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .ofStream(-9999, IntStream.of(-1, 1, 0, 2, 5));
```

```
 a     b
-- -----
-1     1
 0     2
 5 -9999
3 rows x 2 columns
```

`LongStream` 和 `DoubleStream` 使用方法完全一样。

#### foldByColumn

```java
static DataFrameFoldByColumnBuilder foldByColumn(String... columnLabels);
static DataFrameFoldByColumnBuilder foldByColumn(Index columnIndex);
```

`foldByColumn` 以指定 column-labels 创建 `DataFrameFoldByColumnBuilder`，该 builder 以 column-by-column的方式折叠数据为 `DataFrame`，其 `of` 系列方法支持多种数据源。

```java
DataFrame of(Object... data);
<T> DataFrame ofStream(Stream<T> stream);
<T> DataFrame ofIterable(Iterable<T> iterable);

DataFrame ofDoubles(double padWith, double... data);
DataFrame ofStream(DoubleStream stream);
DataFrame ofStream(double padWith, DoubleStream stream);

DataFrame ofInts(int padWith, int... data);
DataFrame ofStream(IntStream stream);
DataFrame ofStream(int padWith, IntStream stream);

DataFrame ofLongs(long padWith, long... data);
DataFrame ofStream(LongStream stream);
DataFrame ofStream(long padWith, LongStream stream);
```

- 以数组为数据源

```java
DataFrame df = DataFrame.foldByColumn("a", "b")
        .of("a", 1, "b", 2, "c", 3);
```

```
a b
- -
a 2
1 c
b 3
3 rows x 2 columns
```

- 以数组为数据源，末尾 row 不完整的用 null 补齐

```java
DataFrame df = DataFrame.foldByColumn("a", "b")
        .of("a", 1, "b", 2, "c");
```

```
a b   
- ----
a 2   
1 c   
b null
3 rows x 2 columns
```

- 对相同类型数据，取模确定 row 数

```java
DataFrame df = DataFrame.foldByColumn("a", "b", "c")
        .of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
```

```
a b c   
- - ----
a e i   
b f j   
c g null
d h null
4 rows x 3 columns
```

- 以 Stream 为数据源

```java
DataFrame df = DataFrame.foldByColumn("a", "b")
        .ofStream(Stream.of("a", 1, "b", 2, "c", 3));
```

```
a b
- -
a 2
1 c
b 3
3 rows x 2 columns
```

- 以 Stream 为数据源，末尾 row 不完整

```java
DataFrame df = DataFrame.foldByColumn("a", "b")
        .ofStream(Stream.of("a", 1, "b", 2, "c"));
```

```
a b   
- ----
a 2   
1 c   
b null
3 rows x 2 columns
```

- 以 `Iterable` 为数据源

```java
DataFrame df = DataFrame.foldByColumn("a", "b")
        .ofIterable(asList("a", 1, "b", 2, "c", 3));
```

```
a b
- -
a 2
1 c
b 3
3 rows x 2 columns
```

- 以 `Iterable` 为数据源，末尾 row 不完整

```java
DataFrame df = DataFrame.foldByColumn("a", "b")
        .ofIterable(asList("a", 1, "b", 2, "c"));
```

```
a b   
- ----
a 2   
1 c   
b null
3 rows x 2 columns
```

- 基础类型不能用 null，需要指定补齐的默认值

```java
DataFrame df = DataFrame.foldByColumn("a", "b")
        .ofInts(-9999, 0, 1, 2, 3, 4, 5);
```

```
a b
- -
0 3
1 4
2 5
3 rows x 2 columns
```

- 基础类型不能用 null，需要指定补齐的默认值，若不指定，默认为 0

```java
DataFrame df = DataFrame.foldByColumn("a", "b")
        .ofInts(-9999, 0, 1, 2, 3, 4);
```

```
a     b
- -----
0     3
1     4
2 -9999
3 rows x 2 columns
```

#### byColumn

使用 `Series` 数组创建 `DataFrame`，每个 `Series` 代表一个 column：

```java
DataFrame df = DataFrame
        .byColumn("name", "age")
        .of(
                Series.of("Joe", "Andrus", "Joan"),
                Series.ofInt(18, 49, 32)
        );
```

这是最有效的方法，因为 `DataFrame` 内部就是采用的 `Series` 数组结构。

- `of` 使用数组为参数

```java
DataFrame df = DataFrame
        .byColumn("a", "b")
        .of(Series.of("a", "b", "c"),
                Series.ofInt(1, 2, 3));
```

```
a b
- -
a 1
b 2
c 3
3 rows x 2 columns
```

- `ofIterable` 使用 `Iterable` 为参数

```java
DataFrame df = DataFrame
        .byColumn("a", "b")
        .ofIterable(List.of(Series.of("a", "b", "c"),
                Series.ofInt(1, 2, 3)));
```

```
a b
- -
a 1
b 2
c 3
3 rows x 2 columns
```

#### convertColumn

```java
<V, VR> DataFrame convertColumn(int pos, ValueMapper<V, VR> converter);
<V, VR> DataFrame convertColumn(String columnLabel, ValueMapper<V, VR> converter);
```

创建 `DataFrame` 副本，其中一个 column 的值使用 `converter` 进行转换。

- 使用 col-name 识别 col

将 col-a 的值乘以 10。

```java
DataFrame df = DataFrame
        .foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .convertColumn("a", v -> ((int) v) * 10);
```

```
 a b
-- -
10 x
20 y
2 rows x 2 columns
```

- 使用 col-index 识别 col

```java
DataFrame df = DataFrame
        .foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .convertColumn(0, v -> ((int) v) * 10);
```

结果同上。

- 包含 null 值

```java
DataFrame df = DataFrame
        .foldByRow("a", "b")
        .of(1, "x", 2, null)
        .convertColumn(1, v -> v != null ? "not null" : "null");
```

```
a b       
- --------
1 not null
2 null    
2 rows x 2 columns
```

- 转换为 `Date` 类型

`ValueMapper.stringToDate()` 使用 `LocalDate.parse(s)` 将字符串转换为 `LocalDate` 类型。

```java
DataFrame df = DataFrame
        .foldByRow("a")
        .of(
                "2018-01-05",
                "2019-02-28",
                null)
        .convertColumn("a", ValueMapper.stringToDate());
```

```
a         
----------
2018-01-05
2019-02-28
null      
3 rows x 1 column
```

- 指定 date 格式

对非标准格式化字符串，需要使用 `DateTimeFormatter` 才能正确解析。

```java
DataFrame df = DataFrame
        .foldByRow("a")
        .of("2018 01 05",
                "2019 02 28",
                null)
        .convertColumn("a",
                ValueMapper.stringToDate(DateTimeFormatter.ofPattern("yyyy MM dd")));
```

```
a         
----------
2018-01-05
2019-02-28
null      
3 rows x 1 column
```

- 转换为 `LocalDateTime`

`ValueMapper.stringToDateTime()` 使用 `LocalDateTime.parse(s)` 将字符串解析为 `LocalDateTime`。

```java
DataFrame df = DataFrame
        .foldByRow("a")
        .of(
                "2018-01-05T00:01:15",
                "2019-02-28T13:11:12",
                null)
        .convertColumn("a", ValueMapper.stringToDateTime());
```

```
a                  
-------------------
2018-01-05T00:01:15
2019-02-28T13:11:12
null               
3 rows x 1 column
```



### name

```java
DataFrame as(String name);
```

设置 `DataFrame` 的名称。

```java
DataFrame df = new ColumnDataFrame(
        "n1",
        Index.of("a", "b"),
        Series.ofInt(1, 2),
        Series.ofInt(3, 4)).as("n2");

new DataFrameAsserts(df, "a", "b").expectHeight(2);
assertEquals("n2", df.getName());
```

### addRow

```java
DataFrame addRow(Map<String, Object> row);
```

在 `DataFrame` 底部添加一行数据。`Map` 定义 col-name 到值的映射。

缺失值用 null 填充，`Map` 中多余的 key 忽略。

> [!WARNING]
>
> 该操作非常慢，如果需要添加许多 rows，可以使用 `vConcat` 方法。

- 添加一次

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df1 = df.addRow(Map.of("a", 3, "b", "z"));
```

```
a b
- -
1 x
2 y
3 z
```

- 再添加一行

```java
DataFrame df2 = df1.addRow(Map.of("a", 55, "b", "A"));
```

```
 a b
-- -
 1 x
 2 y
 3 z
55 A
```

- 缺失值用 null 填充，多余的忽略

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df1 = df.addRow(Map.of("c", 3, "b", "z"));
```

```
   a b
---- -
   1 x
   2 y
null z
```

- 类型改变

```java
DataFrame df = DataFrame.byColumn("a", "b").of(
                Series.ofLong(5L, 6L),
                Series.ofInt(1, 2))
        .addRow(Map.of("a", 3L, "b", "str"));
assertInstanceOf(LongSeries.class, df.getColumn("a").unsafeCastAs(Long.class));
assertInstanceOf(ObjectSeries.class, df.getColumn("b"));
```

```
a b  
- ---
5 1  
6 2  
3 str
```

### getColumn

```java
<T> Series<T> getColumn(int pos);
<T> Series<T> getColumn(String name);
```

返回指定名称或位置的 column。

- 使用 col-label

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

Series<String> cb = df.getColumn("b");
new SeriesAsserts(cb).expectData("x", "y");
```

- 若 col-label 没有，抛出异常

```java
DataFrame df = DataFrame.foldByRow("a").of(1, 2);
assertThrows(IllegalArgumentException.class, () -> df.getColumn("x"));
```

- 使用 col-index

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

Series<String> cb = df.getColumn(0);
new SeriesAsserts(cb).expectData(1, 2);
```

- col-index 超出范围抛出异常

```java
DataFrame df = DataFrame.foldByRow("a").of(1, 2);
assertThrows(IllegalArgumentException.class, () -> df.getColumn(1));
assertThrows(IllegalArgumentException.class, () -> df.getColumn(-1));
```

### get

```java
default Object get(int column, int row);
default Object get(String column, int row);
```

返回指定 cell 的值。

- 使用 col-label

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        3, "z");

assertEquals(2, df.get("a", 1));
assertEquals("z", df.get("b", 2));
```

- 使用 col-index

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        3, "z");

assertEquals(2, df.get(0, 1));
assertEquals("z", df.get(1, 2));
```

- col-index 超出范围，或 col-label 不存在，抛出异常

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        3, "z");
assertThrows(IllegalArgumentException.class, () -> df.get(2, 1));

DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        3, "z");
assertThrows(IllegalArgumentException.class, () -> df.get("c", 1));
```

- row-index 超出范围也抛出异常

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        3, "z");
assertThrows(ArrayIndexOutOfBoundsException.class, () -> df.get(0, 3));
```

### hConcat

拼接 `DataFrame` 有两种方式：

- 垂直：将 `DataFrame` 堆叠在一起，用 [vConcat](#vconcat)
- 水平：将 `DataFrame` 并排放一起，用 [hConcat](#hconcat)

```java
DataFrame hConcat(DataFrame df);
DataFrame hConcat(Index zippedColumns, JoinType how, DataFrame df, RowCombiner c);
DataFrame hConcat(JoinType how, DataFrame df);
```

用于水平拼接两个 `DataFrame`，得到一个**更宽**的 `DataFrame`。

- 如果 col-labels 存在命名冲突，添加 `_` 后缀
- 如果两个 `DataFrame` 的 height 不同，则处理方式取决于 `JoinType`

水平拼接，由于 row 没有名称，因此用 index 来对齐。

`JoinType` 定义如下：

```java
public enum JoinType {
    inner, left, right, full
}
```

- 默认 `JoinType` 值为 `inner`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        0, 1,
        2, 3);

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        10, 20);

DataFrame df_l = df1.hConcat(df2);
```

```
a b  c  d
- - -- --
0 1 10 20
1 row x 4 columns
```

两个 `DataFrame` 的 height 不一样，`inner` 取交集部分。

- 换个方向拼接

```java
DataFrame df_r = df2.hConcat(df1);
```

```
 c  d a b
-- -- - -
10 20 0 1
1 row x 4 columns
```

- 如果两个 `DataFrame` 的 height 相同，则拼接没有任何疑虑

```java
DataFrame df1 = DataFrame.foldByRow("b").of(1, 3);
DataFrame df2 = DataFrame.foldByRow("c").of(10, 30);

DataFrame df = df1.hConcat(df2);
```

```
b  c
- --
1 10
3 30
2 rows x 2 columns
```

- 用 `RowCombiner` 自定义合并方式

`RowCombiner.zip(df1.width()))` 表示直接将两个 `DataFrame` 的数据合并起来，`zip` 参数为右侧 `DataFrame` 数据的起始位置。`Index` 用于指定合并后 `DataFrame` 的标题。

```java
DataFrame df1 = DataFrame.foldByRow("b").of(1, 3);
DataFrame df2 = DataFrame.foldByRow("c").of(10, 30);

DataFrame df = df1.hConcat(Index.of("x", "y"),
        JoinType.inner, df2, RowCombiner.zip(df1.width()));
```

```
x  y
- --
1 10
3 30
2 rows x 2 columns
```

- `JoinType.right` 以右侧 df 的 height 为目标 height

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        0, 1,
        2, 3);

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        10, 20,
        30, 40,
        50, 60);

RowCombiner c = (lr, rr, tr) -> {
    if (rr != null) {
        tr.set(0, rr.get(1));// 第一个位置是 right-DF 的第二个值
    }

    if (lr != null) {
        tr.set(1, lr.get(0));// 第二个位置是 left-DF 的第一个值
    }
};

// JoinType.right 表示以 df2 的 height 为准，在 row-2 df1 没有值，为 null
DataFrame df = df1.hConcat(Index.of("x", "y"), JoinType.right, df2, c);
```

```
 x    y
-- ----
20    0
40    2
60 null
3 rows x 2 columns
```

- `JoinType.left` 表示以左侧 `DataFrame` 的 height 为目标 height

df1 高度为 2，所以生成的 `DataFrame` 高度为 2，df2 高度不足，用 null 补齐。

```java
DataFrame df1 = DataFrame.foldByRow("a").of(
        0,
        1);

DataFrame df2 = DataFrame.foldByRow("b").of(10);

DataFrame df_l = df1.hConcat(JoinType.left, df2);
```

```
a    b
- ----
0   10
1 null
2 rows x 2 columns
```

- `JoinType.left` 

此时以 df2 的 height 为目标 height。

```java
DataFrame df_r = df2.hConcat(JoinType.left, df1);
```

```
 b a
-- -
10 0
1 row x 2 columns
```

- `JoinType.right` 以右侧 `DataFrame` 的 height 为目标 height

```java
DataFrame df1 = DataFrame.foldByRow("a").of(
        0,
        1);
DataFrame df2 = DataFrame.foldByRow("b").of(10);

DataFrame df_l = df1.hConcat(JoinType.right, df2); // df2 高度为 2
```

```
a  b
- --
0 10
1 row x 2 columns
```

```java
DataFrame df_r = df2.hConcat(JoinType.right, df1); // df1 高度为 2
```

```
   b a
---- -
  10 0
null 1
2 rows x 2 columns
```

- `JoinType.full` 以最高的 height 为目标 height

```java
DataFrame df1 = DataFrame.foldByRow("a").of(
        0,
        1);
DataFrame df2 = DataFrame.foldByRow("b").of(10);

DataFrame df_l = df1.hConcat(JoinType.full, df2);
```

```
a    b
- ----
0   10
1 null
2 rows x 2 columns
```

```java
DataFrame df_r = df2.hConcat(JoinType.full, df1);
```

```
   b a
---- -
  10 0
null 1
2 rows x 2 columns
```

### vConcat

```java
DataFrame vConcat(DataFrame... dfs);
DataFrame vConcat(JoinType how, DataFrame... dfs);
```

垂直拼接 `DataFrame`，得到更高的 `DataFrame`。

- 单个 col 拼接最简单

```java
DataFrame df1 = DataFrame.foldByRow("a").of(1, 2);
DataFrame df2 = DataFrame.foldByRow("a").of(10, 20);

DataFrame df = df1.vConcat(df2);
```

```
 a
--
 1
 2
10
20
4 rows x 1 column
```

- 相同 col-labels 的多 col 拼接也容易

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, 2,
        3, 4);

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        5, 6,
        7, 8);

DataFrame dfv = df1.vConcat(df2); 
```

```
a b
- -
1 2
3 4
5 6
7 8
```

- col-label 相同，拼接多个也容易

```java
DataFrame df1 = DataFrame.foldByRow("a").of(1, 2);
DataFrame df2 = DataFrame.foldByRow("a").of(10);
DataFrame df3 = DataFrame.foldByRow("a").of(20);

DataFrame df = df1.vConcat(df2, df3);
```

```
 a
--
 1
 2
10
20
4 rows x 1 column
```

- col-label 不完全相同，`JoinType.left` 只提取匹配 col 的值进行拼接

这里 df2 只有 col-b 匹配，所以只是添加 col-b 的数据。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, 2,
        3, 4);

DataFrame df2 = DataFrame.foldByRow("c", "b").of(
        10, 20,
        30, 40);

DataFrame df = df1.vConcat(df2);
```

```
   a  b
---- --
   1  2
   3  4
null 20
null 40
4 rows x 2 columns
```

- 该拼接默认为 `JoinType.left`，即以左边 `DataFrame` 的 col 为目标

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, 2,
        3, 4);

DataFrame df2 = DataFrame.foldByRow("c", "b").of(
        10, 20,
        30, 40);

DataFrame df = df1.vConcat(JoinType.left, df2);
```

```
   a  b
---- --
   1  2
   3  4
null 20
null 40
4 rows x 2 columns
```

- `JoinType.right` 则以右边 `DataFrame` 的 cols 为目标，但是 rows 的顺序还是左边在上

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, 2,
        3, 4);

DataFrame df2 = DataFrame.foldByRow("c", "b").of(
        10, 20,
        30, 40);

DataFrame df = df1.vConcat(JoinType.right, df2);
```

```
   c  b
---- --
null  2
null  4
  10 20
  30 40
```

- `JoinType.inner` 则以 col 交集为目标

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, 2,
        3, 4);

DataFrame df2 = DataFrame.foldByRow("c", "b").of(
        10, 20,
        30, 40);

DataFrame df3 = DataFrame.foldByRow("b", "d").of(
        100, 200,
        300, 400);

DataFrame df = df1.vConcat(JoinType.inner, df2, df3);
```

```
  b
---
  2
  4
 20
 40
100
300
6 rows x 1 column
```

- `JoinType.full` 则以 col 并集为目标

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, 2,
        3, 4);

DataFrame df2 = DataFrame.foldByRow("c", "b").of(
        10, 20,
        30, 40);

DataFrame df = df1.vConcat(JoinType.full, df2);
```

```
   a  b    c
---- -- ----
   1  2 null
   3  4 null
null 20   10
null 40   30
4 rows x 3 columns
```

### eq 和 ne

```java
DataFrame eq(DataFrame another);
DataFrame ne(DataFrame another);
```

逐元素比较操作，返回 boolean `DataFrame`。

- 全部相等

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame eq = df1.eq(df2);
```

```
   a    b
---- ----
true true
true true
2 rows x 2 columns
```

- 不完全相等

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        1, "X",
        2, "y");

DataFrame eq = df1.eq(df2);
```

```
   a     b
---- -----
true false
true  true
2 rows x 2 columns
```

- 不等

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame eq = df1.ne(df2);
```

```
    a     b
----- -----
false false
false false
2 rows x 2 columns
```

- 不等 2

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "Y");

DataFrame eq = df1.ne(df2);
```

```
    a     b
----- -----
false false
false  true
2 rows x 2 columns
```

- col-labels 不完全匹配，抛出错误

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "B").of(
        1, "x",
        2, "Y");

assertThrows(IllegalArgumentException.class, () -> df1.ne(df2));
```

- row 数目不匹配，抛出错误

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        2, "Y");

assertThrows(IllegalArgumentException.class, () -> df1.ne(df2));
```

### rows

参考 [创建 RowSet](#创建-rowset)。

```java
RowSet rows();

RowSet rows(BooleanSeries condition);
RowSet rows(Condition rowCondition);
RowSet rows(RowPredicate condition)

RowSet rows(int... positions);
RowSet rows(IntSeries positions);

RowSet rowsExcept(Condition condition);
RowSet rowsExcept(RowPredicate condition);

RowSet rowsExcept(int... positions);
RowSet rowsExcept(IntSeries positions);

RowSet rowsRange(int fromInclusive, int toExclusive);

RowSet rowsSample(int size);
RowSet rowsSample(int size, Random random);
```

### map

```java
DataFrame map(UnaryOperator<DataFrame> op);
```

应用于整个 `DataFrame` 的操作。适合将多个操作链式组合起来。

- 删除 col

```java
DataFrame df = DataFrame
        .foldByRow("a", "b").of(1, "x", 2, "y")
        .map(f -> f.cols("b").drop());
```

```
a
-
1
2
2 rows x 1 column
```

### head

大多数 `DataFrame` 的操作是在 `RowSet` 后 `ColumnSet` 上执行。少数操作直接对 `DataFrame` 操作，如 `head` 和 `tail`。

`head` 和 `tail` 的参数都可以为负数，表示跳过开头/结尾的 rows，返回余下 rows。

> [!NOTE]
>
> 和 Java 中数组和 list 不同，`head(..)` 和 `tail(..)` 不边界检查不严格。
>
> 当长度超过 `DataFrame` 的 rows 数，不会抛出异常，而是返回原 `DataFrame` 或 `Series`。
>
> `Series` 的 `head(..)` 和 `tail(..)` 的功能与 `DataFrame` 类似。

```java
DataFrame head(int len);
```

返回 `DataFrame` 的前 `len` 行：

- 如果 `DataFrame` 的高度小于 `len`，返回整个 `DataFrame`
- 如果 `len` 为负数，则跳过前 `|len|` row，返回余下 rows

示例：

- `len` 在范围内，返回包含原 `DataFrame` 前 2 行的 `DataFrame`

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y",
                3, "z")
        .head(2);
```

```
a b
- -
1 x
2 y
2 rows x 2 columns
```

- `len=0` 

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y",
                3, "z")
        .head(0);
```

```
a b
- -
0 rows x 2 columns
```

- `len` 超出 height，直接返回原 `DataFrame`

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y",
                3, "z")
        .head(4);
```

```
a b
- -
1 x
2 y
3 z
3 rows x 2 columns
```

- `len=-2` 表示跳过前 2 行，返回余下 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y",
                3, "z")
        .head(-2);
```

```
a b
- -
3 z
1 row x 2 columns
```

### tail

```java
DataFrame tail(int len);
```

返回包含最后 `len` 行的 `DataFrame`：

- 若原 `DataFrame` 高度小于 `len`，则直接返回原 `DataFrame`；
- 若 `len` 为负数，则跳过最后 `|len|` rows，返回余下 rows。

例如：

- 返回最后 2 个 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y",
                3, "z")
        .tail(2);
```

```
a b
- -
2 y
3 z
2 rows x 2 columns
```

- 返回最后 0 个 row

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y",
                3, "z")
        .tail(0);
```

```
a b
- -
0 rows x 2 columns
```

- `len` 太大，返回原 `DataFrame`

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y",
                3, "z")
        .tail(4);
```

```
a b
- -
1 x
2 y
3 z
3 rows x 2 columns
```

- `len` 为负数，跳过末尾 `|len|` 行

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y",
                3, "z")
        .tail(-2);
```

```
a b
- -
1 x
1 row x 2 columns
```



### nullify

```java
DataFrame nullify(DataFrame condition);
```

参数是一个与当前 `DataFrame` 相同 size 的 boolean `DataFrame`，将对应条件为 true 位置的元素替换为 null。

```java
DataFrame nullifyNoMatch(DataFrame condition);
```

与 `nullify` 相反的操作。

- `nullify` 示例

```java
        DataFrame cond = DataFrame.byColumn("a", "b").of(
                Series.ofBool(true, false),
                Series.ofBool(true, false));

        DataFrame df = DataFrame.foldByRow("a", "b").of(
                        1, "x",
                        2, "y")
                .nullify(cond);
```

```
   a b   
---- ----
null null
   2 y   
2 rows x 2 columns
```

- `nullifyNoMatch` 示例

```java
DataFrame cond = DataFrame.byColumn("a", "b").of(
        Series.ofBool(true, false),
        Series.ofBool(true, false));

DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y").nullifyNoMatch(cond);
```

```
   a b   
---- ----
   1 x   
null null
2 rows x 2 columns
```

- 只对 col-label 相同的 col 进行操作

这里仅 col-b 匹配

```java
DataFrame cond = DataFrame.byColumn("c", "b").of(
        Series.ofBool(true, false),
        Series.ofBool(true, false));

DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y").nullify(cond);
```

```
a b   
- ----
1 null
2 y   
2 rows x 2 columns
```

### pivot

```java
PivotBuilder pivot();
```

- pivot 聚合 2x2

使用 col-b 的值创建 cols (因此 col-b 应该包含分类数据)；使用 col-a 的值创建 rows；`vals()` 执行转换，这里采用 col-c 的加和作为 cell 值。

a-1 与 b-x 对应 col-c 为 15.0；

a-1 与 b-y 对应 col-c 为 20.0；

a-2 与 b-x 没有对应值，因此为 null；

a-2 与 b-y 有两个值 18.0 与 19.0，加和为 37.0.

```jade
DataFrame df1 = DataFrame.foldByRow("a", "b", "c").of(
        1, "x", 15.0,
        2, "y", 18.0,
        2, "y", 19.0,
        1, "y", 20.0);

DataFrame df = df1.pivot()
        .cols("b").rows("a")
        .vals("c", $double(0).sum());
```

```
a    x    y
- ---- ----
1 15.0 20.0
2 null 37.0
2 rows x 3 columns
```

- pivot 聚合 4x4

col-b 作为 cols 值，col-a 作为 rows。col-b 包含 x, y, z, t 4 个值，col-a 包含 1, 2, 3, 4 共 4 个值，`vals()` 选择 col-c 的 sum 生成 4x4=16 个值。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b", "c").of(
        1, "x", 1,
        3, "y", 2,
        2, "y", 3,
        4, "y", 4,
        2, "z", 5,
        2, "t", 6,
        2, "y", 7,
        1, "t", 8,
        4, "x", 9,
        1, "y", 10);

DataFrame df = df1.pivot()
        .cols("b").rows("a")
        .vals("c", $int(0).sum())
        .sort("a", true);
```

```
a    x  y    z    t
- ---- -- ---- ----
1    1 10 null    8
2 null 10    5    6
3 null  2 null null
4    9  4 null null
4 rows x 5 columns
```

- 作为 row 的 col 包含 null 值

col-b 作为 col；col-a 作为 row，但是 col-a 包含 null 值，目前 null 值被直接忽略。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b", "c").of(
        1, "x", 15.0,
        null, "y", 18.0,
        null, "y", 19.0,
        1, "y", 20.0);

DataFrame df = df1.pivot()
        .cols("b").rows("a")
        .vals("c", $double(0).sum());
```

```
a    x    y
- ---- ----
1 15.0 20.0
1 row x 3 columns
```

- 作为 col 的 col 包含 null 值

因为不允许 null 作为 col-label，因此 col-b 中的 null 值被忽略。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b", "c").of(
        1, null, 15.0,
        2, "y", 18.0,
        2, "y", 19.0,
        1, "y", 20.0);

DataFrame df = df1.pivot()
        .cols("b").rows("a")
        .vals("c", $double(0).sum());
```

```
a    y
- ----
2 37.0
1 20.0
2 rows x 2 columns
```

- 非聚合操作

这里直接选择 col-c 的值作为 cell 值。col-b 和 col-a 的组合正好没有重复。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b", "c").of(
        1, "x", 15.0,
        2, "y", 19.0,
        1, "y", 20.0);

DataFrame df = df1.pivot()
        .cols("b").rows("a")
        .vals("c");
```

```
a    x    y
- ---- ----
1 15.0 20.0
2 null 19.0
2 rows x 3 columns
```

- 非聚合操作，如果有重复组合，抛出错误

```java
DataFrame df1 = DataFrame.foldByRow("a", "b", "c").of(
        1, "x", 15.0,
        2, "y", 19.0,
        2, "y", 21.0,
        1, "y", 20.0);

PivotBuilder pb = df1.pivot().cols("b").rows("a");
assertThrows(IllegalArgumentException.class, () -> pb.vals("c"));
```

- 聚合并转换

```java
DataFrame df1 = DataFrame.foldByRow("a", "b", "c").of(
        1, "x", 15.0,
        2, "y", 18.0,
        2, "y", 19.0,
        1, "y", 20.0);

DataFrame df = df1.pivot()
        .cols("b").rows("a")
        .vals("c", $double(0).sum().castAsDecimal().scale(2));
```

```
a     x     y
- ----- -----
1 15.00 20.00
2  null 37.00
2 rows x 3 columns
```

这里在聚合后添加了一个类型转换，将 `double` 转换为 `BigDecimal` 类型。

### sort

```java
// 自定义排序
DataFrame sort(Sorter... sorters);

// 根据单个 col 排序
DataFrame sort(String column, boolean ascending)
DataFrame sort(int column, boolean ascending);

// 根据多个 cols 排序
DataFrame sort(int[] columns, boolean[] ascending);
DataFrame sort(String[] columns, boolean[] ascending)
```

排序不改变原对象，而是创建一个新对象。该原则适用于 `Series` 和 `DataFrame`。

`DataFrame` 排序有上述 5 个方法。可以采用一个或多个 `Sorter`，或直接使用 col-labels 或 col-index。参数 `ascending` 为 true表示升序，false 表示降序。

- `Sorter` 单个 col 排序

基于 col-a 升序排序。

```java
DataFrame dfi = DataFrame.foldByRow("a", "b").of(
        0, 1,
        2, 3,
        -1, 2);

DataFrame df = dfi.sort($int("a").asc());
assertNotSame(dfi, df);
```

dfi:

```
 a b
-- -
 0 1
 2 3
-1 2
3 rows x 2 columns
```

df:

```
 a b
-- -
-1 2
 0 1
 2 3
3 rows x 2 columns
```

可以发现，排序属于 immutable，排序后生成新的 `DataFrame`，不影响原 `DataFrame`。

- null 值默认放在末尾

升序排序，null 在末尾。

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                0, 1,
                null, 3,
                -1, 2)
        .sort($int("a").asc());
```

```
   a b
---- -
  -1 2
   0 1
null 3
3 rows x 2 columns
```

- 升序排序

这里以 col-a 升序排序，null 值在末尾，即默认 null 值最大。

```java
DataFrame dfi = DataFrame.foldByRow("a", "b").of(
        0, 1,
        null, 3,
        -1, 2);

DataFrame dfa = dfi.sort("a", true);
```

```
   a b
---- -
  -1 2
   0 1
null 3
3 rows x 2 columns
```

以 col-a 降序排序，null 值在开头。

```java
DataFrame dfd = dfi.sort("a", false);
```

```
   a b
---- -
null 3
   0 1
  -1 2
3 rows x 2 columns
```

- 以多个 cols 排序

分别以 col-a 和 col-b 排序。

即先以 col-a 的值排序，对无法区分的 rows，在以 col-b 排序。

```java
DataFrame dfi = DataFrame.foldByRow("a", "b").of(
        0, 4,
        2, 2,
        0, 2);

DataFrame dfab = dfi.sort(new String[]{"a", "b"}, new boolean[]{true, true});
```

```
a b
- -
0 2
0 4
2 2
3 rows x 2 columns
```

换个 col 顺序，结果自然不同：

```java
DataFrame dfba = dfi.sort(new String[]{"b", "a"}, new boolean[]{true, true});
```

```
a b
- -
0 2
2 2
0 4
3 rows x 2 columns
```

- 多个 col 排序，null 值规则与单个 col 相同

```java
DataFrame dfi = DataFrame.foldByRow("a", "b").of(
        0, 4,
        2, null,
        0, 2);

DataFrame dfab = dfi.sort(new String[]{"a", "b"}, new boolean[]{true, true});
```

```
a    b
- ----
0    2
0    4
2 null
3 rows x 2 columns
```

```java
DataFrame dfba = dfi.sort(new String[]{"b", "a"}, new boolean[]{true, true});
```

```
a    b
- ----
0    2
0    4
2 null
3 rows x 2 columns
```

- 使用 col-index 与 col-label 没有什么区别

```java
DataFrame dfi = DataFrame.foldByRow("a", "b").of(
        0, 4,
        2, 2,
        0, 2);

DataFrame dfab = dfi.sort(new int[]{0, 1}, new boolean[]{true, true});
```

```
a b
- -
0 2
0 4
2 2
3 rows x 2 columns
```

```java
DataFrame dfba = dfi.sort(new int[]{1, 0}, new boolean[]{true, true});
```

```
a b
- -
0 2
2 2
0 4
3 rows x 2 columns
```

- 使用 col-index 降序排序

```java
DataFrame dfi = DataFrame.foldByRow("a", "b").of(
        0, 3,
        2, 4,
        0, 2);

DataFrame dfab = dfi.sort(1, false);
```

```
a b
- -
2 4
0 3
0 2
3 rows x 2 columns
```

- 若不指定 `Sorter`，返回原 `DataFrame`

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                0, 1,
                2, 3,
                -1, 2)
        // a case of missing sorter
        .sort();
```

````
 a b
-- -
 0 1
 2 3
-1 2
3 rows x 2 columns
````

- 基于 `Sorter` 的升序

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                0, 1,
                2, 3,
                -1, 2)
        .sort($int("a").asc());
```

```
 a b
-- -
-1 2
 0 1
 2 3
3 rows x 2 columns
```

- 基于 `Sorter` 的降序

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                0, 1,
                2, 3,
                -1, 2)
        .sort($int("a").desc());
```

```
 a b
-- -
 2 3
 0 1
-1 2
3 rows x 2 columns
```

- 多个 `Sorter`

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                0, 1,
                2, 3,
                2, 2,
                -1, 2)
        .sort($int("a").asc(), $int("b").asc());
```

```
 a b
-- -
-1 2
 0 1
 2 2
 2 3
4 rows x 2 columns
```

- BigDecimal 排序

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
                0, new BigDecimal("2"),
                2, new BigDecimal("1.0"),
                -1, new BigDecimal("-12.05"))
        .sort($decimal("b").asc());
```

```
 a      b
-- ------
-1 -12.05
 2    1.0
 0      2
3 rows x 2 columns
```

### stack

```java
DataFrame stack();
DataFrame stackIncludeNulls();
```

返回一个新的 `DataFrame`，该 `DataFrame` 包含 3 个 cols: "row", "column", "value"，包含该 `DataFrame` 所有 cols 的所有值。其中 col-row 列出值所在 row-index，col-column 列出值所在的 col-label。

上面两个方法的唯一差别，只是是否包含 null 值。

- stack

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        null, null,
        2, "y").stack();
```

```
row column value
--- ------ -----
  0 a      1    
  2 a      2    
  0 b      x    
  2 b      y    
4 rows x 3 columns
```

- 没有 null 值

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        5, "z",
        2, "y").stack();
```

```
row column value
--- ------ -----
  0 a      1    
  1 a      5    
  2 a      2    
  0 b      x    
  1 b      z    
  2 b      y    
6 rows x 3 columns
```

- `stackIncludeNulls` 会列出 null 值

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        null, null,
        2, "y").stackIncludeNulls();
```

```
row column value
--- ------ -----
  0 a      1    
  1 a      null 
  2 a      2    
  0 b      x    
  1 b      null 
  2 b      y    
6 rows x 3 columns
```

### group

```java
GroupBy group(Hasher by);
GroupBy group(int... columns);
GroupBy group(String... columns);
```

`group` 使用指定 cols 的值生成 hashCode，将 hashCode 值相同的 rows 分到一组。

以下都是针对返回对象 `GroupBy` 的操作。

#### GroupBy

`GroupBy` 对象用于辅助执行分组操作。

- `size()` 返回 group 个数
- `getGroupKeys()` 返回用于分组的 keys

```java
DataFrame df = DataFrame.empty("a", "b");

GroupBy gb = df.group(Hasher.of("a"));
assertNotNull(gb);

assertEquals(0, gb.size());
assertEquals(Collections.emptySet(), new HashSet<>(gb.getGroupKeys()));
```

- `getGroup()` 返回分组

```java
DataFrame getGroup(Object key);
Set<Object> getGroupKeys();
boolean hasGroup(Object key);
```

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        0, "a",
        1, "x");

GroupBy gb = df.group(Hasher.of("a")); // col-a 有 0,1,2 三个值
assertNotNull(gb);

assertEquals(3, gb.size());
assertEquals(new HashSet<>(asList(0, 1, 2)), new HashSet<>(gb.getGroupKeys()));

new DataFrameAsserts(gb.getGroup(0), "a", "b")
        .expectHeight(1)
        .expectRow(0, 0, "a");

new DataFrameAsserts(gb.getGroup(1), "a", "b")
        .expectHeight(3)
        .expectRow(0, 1, "x")
        .expectRow(1, 1, "z")
        .expectRow(2, 1, "x");

new DataFrameAsserts(gb.getGroup(2), "a", "b")
        .expectHeight(1)
        .expectRow(0, 2, "y");
```

- `GroupBy.head(..)` 和 `GroupBy.tail(..)` 返回每个 group 内开头或末尾的 rows

返回排序后每个 group 的最高值

```java
DataFrame topSalary = df.group("date")
        .sort($double("amount").desc()) // 排序
        .head(1) // 选择每个 group 的 top-row
        .select();
```

```
name        amount date
----------- ------ ----------
Joan O'Hara   9300 2024-01-15
Joan O'Hara   9300 2024-02-15
Joan O'Hara   9300 2024-03-15
```

#### null-group

作为 hash 的 col，其 null 值被忽略。例如

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        null, "a",
        1, "x");

GroupBy gb = df.group(Hasher.of("a")); 
assertNotNull(gb);

// col-a  有 null,1,2 三个值
assertEquals(2, gb.size());
assertEquals(new HashSet<>(asList(1, 2)), new HashSet<>(gb.getGroupKeys()));

// getGroup 的参数为 key，而不是 index
new DataFrameAsserts(gb.getGroup(1), "a", "b")
        .expectHeight(3)
        .expectRow(0, 1, "x")
        .expectRow(1, 1, "z")
        .expectRow(2, 1, "x");

new DataFrameAsserts(gb.getGroup(2), "a", "b")
        .expectHeight(1)
        .expectRow(0, 2, "y");
```

#### agg

对每个分组执行聚合操作。

```java
DataFrame agg(Exp<?>... aggregators);
```

参数为 `Exp` 数组，每个 `Exp` 生成一个新的 col。分组和聚合操作常一起使用。

`first()`, `sum()`, `count()` 等都是聚合操作。聚合操作输出的 `Series` 只有一个值。因此，聚合生成的 `DataFrame` 的 row 数与 `GroupBy` 中的 group 数一样。

- 空集合：分组后聚合

聚合 col 的 label 为函数名称与原 col-label 的组合。

```java
DataFrame df1 = DataFrame.empty("a", "b");

DataFrame df = df1.group("a").agg(
        $long("a").sum(),
        $str(1).vConcat(";"));
```

```
sum(a) b
------ -
0 rows x 2 columns
```

- 一个 col 对应一个聚合 `Exp`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        0, "a",
        1, "x");

// 采用 col-a 进行分组
DataFrame df = df1.group("a").agg(
        $long("a").sum(), // 对每个 group，计算 col-a 的加和
        $str(1).vConcat(";"));
```

```
sum(a) b    
------ -----
     3 x;z;x
     2 y    
     0 a    
3 rows x 2 columns
```

- 一个 col 对应多个聚合 `Exp`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "y",
        0, "a",
        1, "x");

DataFrame df = df1
        .group("b") // 使用 col-b 进行分组
        .agg(
                $col("b").first(), // 对每个 group，col-b 的值相同，取第一个
                $long("a").sum(), // 取 group 中 col-a 的加和
                $double("a").median()); // 取 group 中 col-a 的中位数
```

```
b sum(a) median(a)
- ------ ---------
x      2       1.0
y      3       1.5
a      0       0.0
3 rows x 3 columns
```

- 设置 col-label

使用 `as` 设置 col-label。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "y",
        0, "a",
        1, "x");

DataFrame df = df1.group("b").agg(
        $col("b").first().as("first"),
        $col("b").last().as("last"),
        $long("a").sum().as("a_sum"),
        $double("a").median().as("a_median")
);
```

```
first last a_sum a_median
----- ---- ----- --------
x     x        2      1.0
y     y        3      1.5
a     a        0      0.0
3 rows x 4 columns
```

- 也可以预先创建好 `ColumnSet`

```java
GroupBy cols(int... cols);
GroupBy cols(Predicate<String> colsPredicate);
GroupBy cols(String... cols);

GroupBy colsExcept(int... cols);
GroupBy colsExcept(Predicate<String> colsPredicate);
GroupBy colsExcept(String... cols)
```

`cols(String... cols)` 直接定义 col-labels；

`cols(int... cols)` 则使用原 `DataFrame` 对应位置的 col-labels；

`cols(Predicate<String> colsPredicate)` 则选择 `DataFrame` 满足条件的 col-labels.

`colsExcept` 则是取反操作。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        0, "a",
        1, "x");
// `cols("A", "B")` 指定聚合结果的 col-labels
DataFrame df = df1.group("a").cols("A", "B").agg(
        $long("a").sum(),
        $str(1).vConcat(";"));
```

```
A B    
- -----
3 x;z;x
2 y    
0 a    
3 rows x 2 columns
```

- 如果用 col-index 定义 `ColumnSet`，则使用原 `DataFrame` 对应位置的 col-labels

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        0, "a",
        1, "x");

DataFrame df = df1.group("a").cols(1, 0).agg(
        $long("a").sum(),
        $str(1).vConcat(";"));
```

```
b a    
- -----
3 x;z;x
2 y    
0 a    
3 rows x 2 columns
```

- 使用 `Predicate` 选择 col-labels

```java
DataFrame df1 = DataFrame.foldByRow("aA", "b", "aB").of(
        1, "x", 50,
        2, "y", 51,
        1, "z", 52,
        0, "a", 53,
        1, "x", 54);

DataFrame df = df1.group("aA").cols(s -> s.startsWith("a")).agg(
        $long("aA").sum(),
        $str(1).vConcat(";"));
```

```
aA aB   
-- -----
 3 x;z;x
 2 y    
 0 a    
3 rows x 2 columns
```

- `colsExcept(String... cols)`  使用原 `DataFrame` 指定 cols 以外的 col-labels 定义 `ColumnSet`

```java
DataFrame df1 = DataFrame.foldByRow("a", "C", "b").of(
        1, 60, "x",
        2, 61, "y",
        1, 62, "z",
        0, 63, "a",
        1, 64, "x");

DataFrame df = df1.group("a").colsExcept("C").agg(
        $long("a").sum(),
        $str(2).vConcat(";"));
```

```
a b    
- -----
3 x;z;x
2 y    
0 a    
3 rows x 2 columns
```

- `colsExcept(int... cols)`

```java
DataFrame df1 = DataFrame.foldByRow("a", "skip", "b").of(
        1, 40, "x",
        2, 41, "y",
        1, 42, "z",
        0, 43, "a",
        1, 44, "x");

DataFrame df = df1.group("a").colsExcept(1).agg(
        $long("a").sum(),
        $str(2).vConcat(";"));
```

```
a b    
- -----
3 x;z;x
2 y    
0 a    
3 rows x 2 columns
```

- `colsExcept(Predicate<String> colsPredicate)`

```java
DataFrame df1 = DataFrame.foldByRow("aA", "b", "aB").of(
        1, "x", 50,
        2, "y", 51,
        1, "z", 52,
        0, "a", 53,
        1, "x", 54);

DataFrame df = df1.group("aA").colsExcept(s -> !s.startsWith("a")).agg(
        $long("aA").sum(),
        $str(1).vConcat(";"));
```

```
aA aB   
-- -----
 3 x;z;x
 2 y    
 0 a    
3 rows x 2 columns
```

#### merge

```java
DataFrame merge(Exp<?>... exps);
```

将 `Exp` 生成的 col 与原 `DataFrame` 合并。

- 隐式合并

按 col-a 分组后，`rowNum()` 生成每个 group 内部的 row-number。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "y",
        0, "a",
        1, "z");

DataFrame df = df1.group("a").merge(rowNum());
```

```
a b rowNum()
- - --------
1 x        1
1 y        2
1 z        3
2 y        1
0 a        1
5 rows x 3 columns
```

- 用 `head(len)` 取每个分组前 `len` 个数据

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "y",
        0, "a",
        1, "x");

DataFrame df2 = df1.group("a")
        .head(2)
        .merge(rowNum());
```

```
a b rowNum()
- - --------
1 x        1
1 y        2
2 y        1
0 a        1
4 rows x 3 columns
```

- 用 `merge` 操作前，可以用 `cols()` 定义新添加 col 的 label

```java
GroupBy cols(int... cols);
GroupBy cols(Predicate<String> colsPredicate);
GroupBy cols(String... cols);

GroupBy colsExcept(int... cols);
GroupBy colsExcept(Predicate<String> colsPredicate);
GroupBy colsExcept(String... cols);
```

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "a",
        2, "b",
        1, "c",
        0, "d",
        1, "e");

DataFrame df = df1.group("a").cols("X").merge(rowNum());
```

```
a b X
- - -
1 a 1
1 c 2
1 e 3
2 b 1
0 d 1
5 rows x 3 columns
```

- `merge` 多个 `Exp`

这里用 `cols(1,2)` 定义新 cols 名称，col-1 对应 col-b，即用后面 `rowNum()` 替换原 `DataFrame` 的 col-b；col-2 原 `DataFrame` 没有，因此直接添加 col-2，采用 `$val("c")` 填充该 col。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "a",
        2, "b",
        1, "c",
        0, "d",
        1, "e");

DataFrame df = df1.group("a").cols(1, 2)
        .merge(rowNum(), $val("c"));
```

```
a b 2
- - -
1 1 c
1 2 c
1 3 c
2 1 c
0 1 c
5 rows x 3 columns
```

- 使用 `Predicate` 选择 col-labels

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "a",
        2, "b",
        1, "c",
        0, "d",
        1, "e");

DataFrame df = df1.group("a")
        .cols(c -> c.startsWith("b")).merge(rowNum());
```

```
a b
- -
1 1
1 2
1 3
2 1
0 1
5 rows x 2 columns
```

- `colsExcept` 

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "a",
        2, "b",
        1, "c",
        0, "d",
        1, "e");

DataFrame df = df1.group("a").colsExcept("a").merge(rowNum());
```

```
a b
- -
1 1
1 2
1 3
2 1
0 1
5 rows x 2 columns
```

- `colsExcept`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "a",
        2, "b",
        1, "c",
        0, "d",
        1, "e");

DataFrame df = df1.group("a").colsExcept(0).merge(rowNum());
```

```
a b
- -
1 1
1 2
1 3
2 1
0 1
5 rows x 2 columns
```

- `colsExcept`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "a",
        2, "b",
        1, "c",
        0, "d",
        1, "e");

DataFrame df = df1.group("a")
        .colsExcept(c -> c.startsWith("a")).merge(rowNum());
```

```
a b
- -
1 1
1 2
1 3
2 1
0 1
5 rows x 2 columns
```

#### rank

```java
IntSeries rank();
```

为每个 group 的 rows 生成 rank，返回的 rank 值的顺序与原 `DataFrame` 的 rows 顺序一致。

rank 值通过指定 `Comparator` 确认，如果不指定，所有 row 的 rank 默认为 1。指定排序的方法有多个：

```java
GroupBy sort(int column, boolean ascending);
GroupBy sort(int[] columns, boolean[] ascending);

GroupBy sort(IntComparator sorter);
GroupBy sort(Sorter... sorters);

GroupBy sort(String column, boolean ascending);
GroupBy sort(String[] columns, boolean[] ascending);
```

-  不排序

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        0, "a",
        1, "x");

IntSeries rn1 = df.group("a").rank();
new IntSeriesAsserts(rn1).expectData(1, 1, 1, 1, 1);

IntSeries rn2 = df.group("b").rank();
new IntSeriesAsserts(rn2).expectData(1, 1, 1, 1, 1);
```

- 排序

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        0, "a",
        1, "x");

// 按 col-a 分组，按 col-b 排序
// col-a 的 1 对应 x, z, x，对应 rank 1,3,1
IntSeries rn1 = df.group("a").sort("b", true).rank();
new IntSeriesAsserts(rn1).expectData(1, 1, 3, 1, 1);

IntSeries rn2 = df.group("b").sort("a", true).rank();
new IntSeriesAsserts(rn2).expectData(1, 1, 1, 1, 1);
```

- `Sorter` 排序

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        0, "a",
        1, "x");

IntSeries rn1 = df.group("a").sort($col("b").asc()).rank();
new IntSeriesAsserts(rn1).expectData(1, 1, 3, 1, 1);

IntSeries rn2 = df.group("b").sort($col("a").asc()).rank();
new IntSeriesAsserts(rn2).expectData(1, 1, 1, 1, 1);
```

#### select

```java
DataFrame select();
```

将 groups 重组为 `DataFrame`，保留分组、组内排序等。

- 隐式 cols

这里以 col-a 分组，分组后 rows 的顺序与 col-a 值首次出现的顺序一致。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "y",
        0, "a",
        1, "x");

DataFrame df = df1.group("a").select();
```

```
a b
- -
1 x
1 y
1 x
2 y
0 a
5 rows x 2 columns
```

- `head` 后 `select`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "y",
        0, "a",
        1, "x");

DataFrame df2 = df1.group("a")
        .head(2)
        .select();
```

```
a b
- -
1 x
1 y
2 y
0 a
4 rows x 2 columns
```

```java
DataFrame df3 = df1.group("a")
        .head(1)
        .select();
```

```
a b
- -
1 x
2 y
0 a
3 rows x 2 columns
```

```java
DataFrame df4 = df1.group("a")
        .head(-1)
        .select();
```

```
a b
- -
1 y
1 x
2 y
0 a
4 rows x 2 columns
```

- `sort`, `head`, `select`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "y",
        0, "a",
        1, "x");

DataFrame df2 = df1.group("a")
        .sort("b", false) // 降序排序
        .head(2)
        .select();
```

```
a b
- -
1 y
1 x
2 y
0 a
4 rows x 2 columns
```

`tail` 和 `head` 类似，不再赘述。

- 多个 cols 排序

```java
DataFrame df1 = DataFrame.foldByRow("a", "b", "c").of(
        1, "x", 2,
        2, "b", 1,
        2, "a", 2,
        1, "z", -1,
        0, "n", 5,
        1, "x", 1);

DataFrame df2 = df1.group("a")
        .sort(new String[]{"b", "c"}, new boolean[]{true, true})
        .select();
```

```
a b  c
- - --
1 x  1
1 x  2
1 z -1
2 a  2
2 b  1
0 n  5
6 rows x 3 columns
```

- 定义 `cols`

参考 `agg`, `merge` 或 `select(Exp..)`，用法一样。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "a",
        2, "b",
        1, "c",
        0, "d",
        1, "e");

DataFrame df = df1.group("a").cols("b").select();
```

```
b
-
a
c
e
b
d
5 rows x 1 column
```

#### select-Exp

```java
DataFrame select(Exp<?>... exps);
```

`select` 根据 `Exp` 生成 `DataFrame`，每个 `Exp `单独应用于每个 group。

> 即执行 group-specific 转换，然后返回原 `DataFrame`。
>
> 得到的 `DataFrame` 与原 `DataFrame` rows 数相同，但熟悉可能不同。

- 例如，分组后执行组内排序，然后返回原始 rows

```java
DataFrame ranked = df.group("date")
        .sort($double("amount").desc()) // 对每个 group，按 amount 降序
        .cols("date", "name", "rank")
        .select( // 使用 select 而非聚合操作
                $col("date"),
                $col("name"),
                rowNum() // 对每个 group 分别排序
        );
```

```
date       name             rank
---------- ---------------- ----
2024-01-15 Joan O'Hara         1
2024-01-15 Juliana Walewski    2
2024-01-15 Jerry Cosin         3
2024-02-15 Joan O'Hara         1
2024-02-15 Juliana Walewski    2
2024-02-15 Jerry Cosin         3
2024-03-15 Joan O'Hara         1
2024-03-15 Jerry Cosin         2
```

- `rowNum()` 生成每个 group 的 row-number，`$col("b")` 则直接选择 col-b

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "y",
        0, "a",
        1, "z");

DataFrame df = df1.group("a")
        .select(rowNum(), $col("b"));
```

```
rowNum() b
-------- -
       1 x
       2 y
       3 z
       1 y
       1 a
5 rows x 2 columns
```

- 取每个 group 的前 2 行，然后生成 `rowNum()`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "y",
        0, "a",
        1, "x");

DataFrame df2 = df1.group("a")
        .head(2)
        .select(rowNum());
```

```
rowNum()
--------
       1
       2
       1
       1
4 rows x 1 column
```

- 在 `select` 前，用 `cols` 指定 col-label

指定方式和 `agg` 以及 `merge` 完全相同，示例：

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "a",
        2, "b",
        1, "c",
        0, "d",
        1, "e");

DataFrame df = df1.group("a").cols("X")
        .select(rowNum());
```

```
X
-
1
2
3
1
1
5 rows x 1 column
```

- 生成两个 cols

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "a",
        2, "b",
        1, "c",
        0, "d",
        1, "e");

DataFrame df = df1.group("a")
        .cols(1, 2).select(rowNum(), $val("c"));
```

```
b 2
- -
1 c
2 c
3 c
1 c
1 c
5 rows x 2 columns
```

### join

```jade
Join join(DataFrame rightFrame); // 同 `innerJoin`
Join innerJoin(DataFrame rightFrame);
Join leftJoin(DataFrame rightFrame);
Join rightJoin(DataFrame rightFrame);
Join fullJoin(DataFrame rightFrame);
```

`join` 用于按 row 合并两个 `DataFrame` 的数据。

用 `on` 指定匹配的 col，内部使用 `Hasher` 来查找匹配值。

```java
Join on(Hasher hasher);
Join on(Hasher left, Hasher right);
Join on(int columnsIndex);
Join on(int leftColumn, int rightColumn);
Join on(String column);
Join on(String leftColumn, String rightColumn);
```

- `innerJoin` 取交集

df1 和 df2 的 col-0 只有 2 相同，因此取 col-0 为 2 的所有值。合并进来的 df2 添加名称 df2. 前缀。

> [!TIP]
>
> 会自动添加 `as` 设置的前缀，若有命名冲突，再添加 `_` 后缀。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y"); //

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        2, "a", //
        2, "b", //
        3, "c");

DataFrame df = df1.innerJoin(df2.as("df2"))
        .on(0) // 设置匹配 col
        .select();
```

```
a b df2.a df2.b
- - ----- -----
2 y     2 a    
2 y     2 b    
2 rows x 4 columns
```

- 设置 `DataFrame` 名称

这里 df1 设置名称后，join 后的 `DataFrame` 自动加上 `df1.` 前缀。`df2` 由于命名冲突，加上 `_` 后缀。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.as("df1").innerJoin(df2)
        .on(0)
        .select();
```

```
df1.a df1.b a_ b_
----- ----- -- --
    2 y      2 a 
    2 y      2 b 
2 rows x 4 columns
```

- 设置 `DataFrame` 名称

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.as("df1").innerJoin(df2.as("df2"))
        .on(0)
        .select();
```

```
df1.a df1.b df2.a df2.b
----- ----- ----- -----
    2 y         2 a    
    2 y         2 b    
2 rows x 4 columns
```

- df1 和 df2 指定不同 col 进行匹配

这里用 df1 的 col-0 与 df2 的 col-1 进行匹配。匹配上 2和4 两个值。

这里采用 df1 计算 hashCode，匹配 df2 所有 hashCode 相同的值。df1 中若有重复值，只保留第一个。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        4, "z");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        "a", 2,
        "b", 2,
        "x", 4,
        "c", 3);

DataFrame df = df1.innerJoin(df2)
        .on(Hasher.of(0), Hasher.of(1))
        .select();
```

```
a b c d
- - - -
2 y a 2
2 y b 2
4 z x 4
3 rows x 4 columns
```

- `fullJoin` 取并集

col-0 共有 1,2,3 三种值。

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
                1, "x",
                2, "y")
        .cols(0).compactInt(0);

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
                2, "a",
                2, "b",
                3, "c")
        .cols(0).compactInt(0);

DataFrame df = df1.fullJoin(df2)
        .on(0)
        .select();
```

```
   a b       c d   
---- ---- ---- ----
   1 x    null null
   2 y       2 a   
   2 y       2 b   
null null    3 c   
4 rows x 4 columns
```

- `leftJoin` 以左侧 `DataFrame` 的 col 值为目标

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.leftJoin(df2) // 以 df1 为参考
        .on(0) // df1 的 col-0 有 1,2 二个值
        .select();
```

```
a b    c d   
- - ---- ----
1 x null null
2 y    2 a   
2 y    2 b   
3 rows x 4 columns
```

- `rightJoin` 则以右侧 `DataFrame` 的  col 值为目标

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df2.rightJoin(df1) //df1 放到了右侧
        .on(0) // df1 的 col-0 有 1,2 二个值
        .select();
```

```
   c d    a b
---- ---- - -
null null 1 x
   2 a    2 y
   2 b    2 y
3 rows x 4 columns
```

- 如果两个 `DataFrame` 用于匹配的 col 都有重复值

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "a",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df2.innerJoin(df1)
        .on("c", "a")
        .on("d", "b")
        .select();
```

```
c d a b
- - - -
2 a 2 a
1 row x 4 columns
```

- 可以添加一个 `indicatorColumn` 只是每个 row 的匹配情况

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.fullJoin(df2)
        .on(0)
        .indicatorColumn("ind") // 创建指定名称的 indicator col
        .select();
```

```
   a b       c d    ind       
---- ---- ---- ---- ----------
   1 x    null null left_only 
   2 y       2 a    both      
   2 y       2 b    both      
null null    3 c    right_only
4 rows x 5 columns
```

> [!NOTE]
>
> 所有 join 类型都可以输出 indicator-col，但是它只对三种 outer-join 有意义，对 inner-join 结果总是 `both`。

#### predicatedBy

上面介绍的 join 操作都是通过对比 left-col 和 right-col 的 hash 值来实现，称之为 hash-join，速度较快。也可以使用下列方式指定匹配条件，称为 nested-loop，将 left-df 和 right-df 逐行比对：

```java
Join predicatedBy(JoinPredicate predicate)；
```

该方式采用**嵌套循环**匹配，**比较慢，尽量避免使用**。

- `innerJoin`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.innerJoin(df2)
        .predicatedBy((lr, rr) -> Objects.equals(lr.get(0), rr.get(0)))
        .select();
```

```
a b c d
- - - -
2 y 2 a
2 y 2 b
2 rows x 4 columns
```

- 无匹配

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.innerJoin(df2)
        .predicatedBy((lr, rr) -> false) // 强制无匹配
        .select();
```

```
a b c d
- - - -
0 rows x 4 columns
```

- col-labels 重复时自动重命名

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.innerJoin(df2)
        .predicatedBy((lr, rr) -> Objects.equals(lr.get(0), rr.get(0)))
        .select();
```

```
a b a_ b_
- - -- --
2 y  2 a 
2 y  2 b 
2 rows x 4 columns
```

- 对 df2 指定名称

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.innerJoin(df2.as("df2"))
        .predicatedBy((lr, rr) -> Objects.equals(lr.get(0), rr.get(0)))
        .select();
```

```
a b df2.a df2.b
- - ----- -----
2 y     2 a    
2 y     2 b    
2 rows x 4 columns
```

- 对 df1 指定名称

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.as("df1").join(df2)
        .predicatedBy((lr, rr) -> Objects.equals(lr.get(0), rr.get(0)))
        .select();
```

```
df1.a df1.b a_ b_
----- ----- -- --
    2 y      2 a 
    2 y      2 b 
2 rows x 4 columns
```

- 指定 df1 和 df2 的名称

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        2, "a",
        2, "b",
        3, "c");

DataFrame df = df1.as("df1").innerJoin(df2.as("df2"))
        .predicatedBy((lr, rr) -> Objects.equals(lr.get(0), rr.get(0)))
        .select();
```

```
df1.a df1.b df2.a df2.b
----- ----- ----- -----
    2 y         2 a    
    2 y         2 b    
2 rows x 4 columns
```

#### selectAs

```java
DataFrame selectAs(String... newColumnNames);
DataFrame selectAs(Map<String, String> oldToNewNames);
DataFrame selectAs(UnaryOperator<String> renamer);
```

用于 `join` 结果的 col 重命名。

- `innerJoin`

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        4, "z");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        "a", 2,
        "x", 4,
        "c", 3);

DataFrame df = df1.innerJoin(df2)
        .on(0, 1)
        .selectAs("A", "B", "C", "D");
```

```
A B C D
- - - -
2 y a 2
4 z x 4
2 rows x 4 columns
```

- 选择 cols 再重名

```java
Join cols(int... columns);
Join cols(String... columns);
Join cols(Predicate<String> labelCondition);

Join colsExcept(int... columns);
Join colsExcept(String... columns);
Join colsExcept(Predicate<String> labelCondition);
```

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        4, "z");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        "a", 2,
        "x", 4,
        "c", 3);

DataFrame df = df1.innerJoin(df2)
        .on(0, 1)
        .cols("a", "b", "c", "d")
        .selectAs("A", "B", "C", "D");
```

```
A B C D
- - - -
2 y a 2
4 z x 4
2 rows x 4 columns
```

- 选择部分 cols 再重名

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y", //
        4, "z"); //

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        "a", 2, //
        "b", 2, //
        "x", 4, //
        "c", 3);

DataFrame df = df1.innerJoin(df2)
        .on(0, 1)
        .cols("a", "c")
        .selectAs("A", "C");
```

```
A C
- -
2 a
2 b
4 x
3 rows x 2 columns
```

- `cols(..)` 包含没有的 col，值为 null

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        4, "z");

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        "a", 2,
        "x", 4,
        "c", 3);

DataFrame df = df1.innerJoin(df2)
        .on(0, 1)
        .cols("a", "b", "X")
        .selectAs("A", "B", "Y");
```

```
A B Y   
- - ----
2 y null
4 z null
2 rows x 3 columns
```

- 排除重命名后的 cols

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        4, "z");

DataFrame df2 = DataFrame.foldByRow("a", "b").of(
        "a", 2,
        "b", 2,
        "x", 4,
        "c", 3);

DataFrame df = df1.innerJoin(df2)
        .on(0, 1)
        .cols(c -> !c.endsWith("_"))
        .selectAs("X", "Y");
```

```
X Y
- -
2 y
2 y
4 z
3 rows x 2 columns
```

#### select

```java
DataFrame select(Exp<?>... exps);
DataFrame select();
```

返回 `join` 结果。

- 选择所有结果

```java
DataFrame df1 = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y", // 
        4, "z"); // 

DataFrame df2 = DataFrame.foldByRow("c", "d").of(
        "a", 2, //
        "x", 4, //
        "c", 3);

DataFrame df = df1.innerJoin(df2)
        .on(0, 1)
        .select();

new DataFrameAsserts(df, "a", "b", "c", "d")
        .expectHeight(2)
        .expectRow(0, 2, "y", "a", 2)
        .expectRow(1, 4, "z", "x", 4);
```

#### select-Exp

在构建 join 结果时，可以使用 exp 修改已有 col，也可以添加新的 col：

```java
DataFrame joined = left.as("L")
        .join(right.as("R"))
        .on("id")
        .cols("name", "retires_soon")
        .select(
                $col("name"),
                $int("R.age").gt(57)
        );
```

```
name    retires_soon
------- ------------
Juliana        false
Joan            true
```

> [!NOTE]
>
> 在 join 时，col-exp 可以引用 left 和 right col 的 short-col-names，也可以引用带前缀的完整 col-names。使用 short-col-names 时，需要考虑 `_` 后缀。

## ColumnSet

`ColumnSet` 表示 col 集合。有两种类型：

- FixedColumnSet：提前定义好宽度
- DeferredColumnSet：根据后面的表达式动态定义

`ColumnSet` 以 `DataFrame` 为数据源，定义基于 column 的操作。

### 创建 ColumnSet

操作 `DataFrame` 数据，一般从选择 cols 或 rows 开始，所得子集用 `ColumnSet` 或 `RowSet` 对象表示。

基于 column 的操作从定义 `ColumnSet` 开始。可以按条件、名称、位置以及隐式定义 `ColumnSet`。

```java
ColumnSet cols();
ColumnSet cols(Index columnsIndex);
ColumnSet cols(int... columns);
ColumnSet cols(Predicate<String> condition);
ColumnSet cols(String... columns);

ColumnSet colsExcept(int... columns);
ColumnSet colsExcept(Predicate<String> condition);
ColumnSet colsExcept(String... columns);
```

- `cols()` 根据后续操作隐式创建 `ColumnSet`

**agg 操作**

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c", "d").of(
        1, "x", "n", 1.0,
        2, "y", "a", 2.5,
        0, "a", "z", 0.001);

DataFrame agg = df
        .cols() // 选择所有 cols
        .agg(
                $long("a").sum(), // 计算 col-a 加和
                count(),          // rows 数
                $double("d").sum()); // 计算 col-d 加和
```

```
sum(a) count sum(d)
------ ----- ------
     3     3  3.501
```



- `cols(Predicate<String> condition)` 根据条件选择 cols

```java
DataFrame df1 = df.cols(c -> !"middle".equals(c)).select();
```

这种形式的 `cols(...)` 不允许对 col 重新排序。生成的 col 的顺序与原 `DataFrame` 的相对顺序保持一致：

```
first   last
------- --------
Jerry   Cosin
Joan    O'Hara
```

- `cols(String... columns)` 设置返回 `ColumnSet` 的 col-names

这种选择方方式得到的 col 顺序与参数一致，即支持设置 col 顺序。

```java
DataFrame df = DataFrame.foldByRow("first", "last", "middle").of(
        "Jerry", "Cosin", "M",
        "Joan", "O'Hara", null);

DataFrame df1 = df
        .cols("last", "first") // 匹配名称的 cols 组成的 `ColumnSet`，columns 顺序与参数一致
        .select(); // 以 ColumnSet 创建新的 DataFrame
```

```
last     first
-------- -------
Cosin    Jerry
O'Hara   Joan
```

**agg 操作**

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c", "d").of(
        1, "x", "n", 1.0,
        2, "y", "a", 2.5,
        0, "a", "z", 0.001);

DataFrame agg = df
        .cols("sum_a", "count", "sum_d") // 根据 agg 操作生成 columns
        .agg(
                $long("a").sum(),
                count(),
                $double("d").sum());
```

```
sum_a count sum_d
----- ----- -----
    3     3 3.501
```



除了指定要选择的 cols，也可以选择排除哪些 cols。这种模式不支持重新排序：

```java
DataFrame df1 = df.colsExcept("middle").select();
```

- `cols(int... columns)` 根据 col 位置选择

```java
DataFrame df1 = df.cols(1, 0).select();
```



**agg 操作**

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c", "d").of(
        1, "x", "n", 1.0,
        2, "y", "a", 2.5,
        0, "a", "z", 0.001);

DataFrame agg = df
        .cols(0, 2, 3) // 选择基础 cols
        .agg(
                $long("a").sum(),
                count(),
                $double("d").sum());
```

```
a c     d
- - -----
3 3 3.501
```





- `cols()` 选择所有 cols

如果在构建 `COlumnSet` 时不提供参数，则返回的 `DataFrame` 与原来的 `DataFrame` 相同。

```java
DataFrame df1 = df.cols().select();
```

### as

```java
DataFrame as(UnaryOperator<String> renamer);
DataFrame as(String... newColumnNames);
DataFrame as(Map<String, String> oldToNewNames);
```

重命名 cols。

#### asArray

- 示例

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols()
        .as("c", "d");
```

```
c d
- -
1 x
2 y
```

- 选择 col-a 和 col-c，然后重命名

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c")
        .as("X", "Y");
```

```
X b Y
- - -
1 x a
2 y b
```

- 添加新的 col

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c", "new") // new 是新的 col
        .as("X", "Y", "NEW");
```

```
X b Y NEW 
- - - ----
1 x a null
2 y b null
```

#### asMap

使用 `Map` 指定重命名。

- 示例

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols()
        .as(Map.of("a", "c", "b", "d"));
```

```
c d
- -
1 x
2 y
```

- 选择要命名的 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c")
        .as(Map.of("a", "X", "c", "Y"));
```

```
X b Y
- - -
1 x a
2 y b
```

- 添加新的 col

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c", "new") // new 重命名为 NEW
        .as(Map.of("a", "X", "new", "NEW"));
```

```
X b c NEW 
- - - ----
1 x a null
2 y b null
```

#### asUnaryOperator

- 使用函数重命名

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols()
        .as(c -> "[" + c + "]");
```

```
[a] [b]
--- ---
  1 x  
  2 y 
```

- 选择部分 col 重命名

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c")
        .as(c -> "[" + c + "]");
```

```
[a] b [c]
--- - ---
  1 x a  
  2 y b
```

- 添加新的 col

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c", "new")
        .as(c -> "[" + c + "]");
```

```
[a] b [c] [new]
--- - --- -----
  1 x a   null 
  2 y b   null 
```

### compact

```java
DataFrame compactBool();
<V> DataFrame compactBool(BoolValueMapper<V> converter);

DataFrame compactInt(int forNull);
<V> DataFrame compactInt(IntValueMapper<V> converter);

DataFrame compactLong(long forNull);
<V> DataFrame compactLong(LongValueMapper<V> converter);

DataFrame compactDouble(double forNull);
<V> DataFrame compactDouble(DoubleValueMapper<V> converter);
```

将 `ColumnSet` 的 cols 类型转换为指定 primitives 类型。

```java
DataFrame df = DataFrame.foldByRow("year", "sales").of(
        "2022", "2005365.01",
        "2023", "4355098.75");

DataFrame df1 = df
        .cols("year").compactInt(0)
        .cols("sales").compactDouble(0.);
```

`compact` 的参数指定要转换的值为 `null` 时的默认值。

> [!TIP]
>
> `compactInt(..)`, `compactDouble(..)` 等方法不仅仅是语法糖，它们将 `DataFrame` 的 col 转换为 primitive `Series` 可以减少内存占用，且计算速度比基于对象的 `Series` 更快。

#### compactBool

```java
DataFrame df = DataFrame.byColumn("a", "b", "c").of(
                Series.ofBool(true, false),
                Series.of(null, "true"),
                Series.of(Boolean.TRUE, Boolean.FALSE)
        )
        .cols()
        .compactBool();
```

```
    a     b     c
----- ----- -----
 true false  true
false  true false
```

- 根据指定 `BoolValueMapper` 进行转换

```java
DataFrame df = DataFrame.byColumn("a", "b").of(
                Series.of(5, 6),
                Series.ofInt(8, 9)
        )
        .cols()
        .compactBool((Integer o) -> o % 2 == 0);
```

```
    a     b
----- -----
false  true
 true false
```

- `String` 类型转换为 bool

```java
DataFrame df = DataFrame.byColumn("a", "b", "c", "d").of(
                Series.ofBool(true, false),
                Series.of(null, "true"),
                Series.of(Boolean.TRUE, Boolean.FALSE),
                Series.of("one", "two")
        )
        .cols("a", "b", "c") // 选择 a, b, c 进行转换，col-b 是 String 类型
        .compactBool();
```

```
    a     b     c d  
----- ----- ----- ---
 true false  true one
false  true false two
```

`String` 类型采用 `Boolean.parseBoolean()` 转换为 bool。

#### compactDouble

默认使用 `Double.parseDouble` 解析其它类型。

- 默认使用 `Double.parseDouble` 转换

```java
DataFrame df = DataFrame.byColumn("a", "b", "c").of(
                Series.ofDouble(1, 2),
                Series.of(null, "5"),
                Series.of(Boolean.TRUE, Boolean.FALSE)
        )
        .cols()
        .compactDouble(-1);
```

```
  a    b   c
--- ---- ---
1.0 -1.0 1.0
2.0  5.0 0.0
```

- 使用 `DoubleValueMapper` 转换

```java
DataFrame df = DataFrame.byColumn("a", "b").of(
                Series.of("a", "ab"),
                Series.of("abc", "abcd")
        )
        .cols()
        .compactDouble((String o) -> o.length() + 0.1);
```

```
  a   b
--- ---
1.1 3.1
2.1 4.1
```

- 选择 cols 进行转换，未选择的 col 保持不变

```java
DataFrame df = DataFrame.byColumn("a", "b", "c", "d").of(
                Series.ofDouble(1, 2),
                Series.of(null, "5"),
                Series.of(Boolean.TRUE, Boolean.FALSE),
                Series.of("one", "two")
        )
        .cols("a", "b", "c") // 转换 col a,b,c
        .compactDouble(-1); // 默认 -1
```

```
  a    b   c d  
--- ---- --- ---
1.0 -1.0 1.0 one
2.0  5.0 0.0 two
```

`compactInt` 和 `compactLong` 与 `compactDouble` 的用法基本一样。

### merge

`ColumnSet` 提供了许多合并操作。所有不以 `select...` 开头的 `ColumnSet` 方法都与源 `DataFrame` 执行合并。包括重命名和数据转换也可以作为合并来执行。

合并规则：

- 合并按 col-name 完成
  - `DataFrame` 中与 `ColumnSet` 的名称匹配的 cols 被替换为 `ColumnSet` 版本；
  - `DataFrame` 包含而 `ColumnSet` 不包含的 cols 保持不变；
  - `DataFrame` 不包含而 `ColumnSet` 包含的 cols 添加到 `DataFrame` 右侧；
- `ColumnSet` 中 cols 的顺序不影响被替换 cols 的顺序，添加的 cols 顺序与 `ColumnSet` 中一致
- 所有转换操作都应用于原 `DataFrame` 的 cols

```java
DataFrame merge(Exp<?>... exps);
DataFrame merge(Series<?>... columns);
DataFrame merge(RowToValueMapper<?>... mappers);
DataFrame merge(RowMapper mapper);
```

这 4 个函数都是执行合并 col 操作，只是提供 col 值的方式不同。例如：

```java
DataFrame df = DataFrame.foldByRow("first", "last", "middle").of(
        "jerry", "cosin", "M",
        "Joan", "O'Hara", null);

Function<String, Exp<String>> cleanup = col -> $str(col).mapVal(
        s -> s != null && !s.isEmpty()
                ? Character.toUpperCase(s.charAt(0)) + s.substring(1)
                : null); // 实现首字母大写

DataFrame df1 = df
        .cols("last", "first", "full") // 要选择或生成的 cols
        .merge( // 不使用 select，而是用 merge 将 col 与原 DataFrame 合并
                cleanup.apply("last"),
                cleanup.apply("first"),
                concat($str("first"), $val(" "), $str("last"))
        );
```

```
first last   middle full
----- ------ ------ -----------
Jerry Cosin  M      jerry cosin
Joan  O'Hara null   Joan O'Hara
```

根据合并规则，`cols("last", "first", "full")` 定义的前两个 cols 在原 `DataFrame` 已经包含，因此替换为 cleanup 后的版本；"full" 是新添加的 col，添加到 `DataFrame` 右侧。

添加 `ColumnSet` 的所有 cols，而不替换 `DataFrame` 中原有 cols，有两种实现方式：

- 手动指定 `ColumnSet` 的 col-names，保证名称不冲突
- 使用 `DataFrame.colsAppend(..)`，DFLib 会在每个新 col-name 添加 `_` 以确保不与已有 col-names 相同

```java
DataFrame df1 = df
        .colsAppend("last", "first") // 添加 cols
        .merge(
                cleanup.apply("last"),
                cleanup.apply("first")
        );
```

```
first last   middle last_  first_
----- ------ ------ ------ ------
jerry cosin  M      Cosin  Jerry
Joan  O'Hara null   O'Hara Joan
```

#### merge-Exp

```java
DataFrame merge(Exp<?>... exps);
```

将 `exps` 生成的 cols 合并到当前 `DataFrame`。

- 使用 col-name 定义 cols

`$int(0).mul(100)` 选择 col-0 乘以 100 作为 col-b 的值；`$int(0).mul(10)` 选择 col-0 乘以 100 作为 col-c 的值。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("b", "c").merge($int(0).mul(100), $int(0).mul(10));
```

```
a   b  c
- --- --
1 100 10
2 200 20
```

- 重复 cols

`cols("b", "b")` 选择 col-b 两次，因此生成两个 col-b，第二个自动添加 `_` 后缀；col-a 不变。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("b", "b").merge($int(0).mul(100), $int(0).mul(10));
```

```
a   b b_
- --- --
1 100 10
2 200 20
```

- 使用 col-index 选择 cols

其中 2 超出 col-index 范围，因此新建 col-2。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols(1, 2).merge($int(0).mul(100), $int(0).mul(10));
```

```
a   b  2
- --- --
1 100 10
2 200 20
```

- 使用 `colsAppend` 添加新 cols，不会修改原 cols

当添加的 cols 与原 cols 有命名冲突，自动添加后缀 `_`。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .colsAppend("b", "c", "b").merge(
                $int(0).mul(100),
                $int(0).mul(10),
                $str(1).mapVal(v -> "[" + v + "]"));
```

```
a b  b_  c b__
- - --- -- ---
1 x 100 10 [x]
2 y 200 20 [y]
```

- 不选择 cols，则根据后面的表达式推断 cols

`as("b")` 指定生成的 `Series` 为 "b"，替换原来的 col-b；`$int(0).mul(10)` 没有指定名称，生成新的 col，其名称根据表达式自动生成。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols().merge($int(0).mul(100).as("b"), $int(0).mul(10));
```

```
a   b a * 10
- --- ------
1 100     10
2 200     20
```

- `colsExcept` 在合并 cols 排除指定 col

`colsExcept("a")` 排除 col-a，对 col-b 和 col-c 执行合并操作。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept("a").merge($int(0).mul(100), $int(0).mul(10));
```

```
a   b  c
- --- --
1 100 10
2 200 20
```

- `colsExcept` 支持以 col-index 为参数

`colsExcept(1)` 排除 col-1，对 col-0 为 col-2 执行合并操作。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept(1).merge($int(0).mul(100), $int(0).mul(10));
```

```
  a b  c
--- - --
100 x 10
200 y 20
```

#### merge-RowMapper

```java
DataFrame merge(RowMapper mapper);
```

使用 `RowMapper` 定义生成数据的方式。

`RowMapper` 定义一个 row 到另一个 row 的映射。

- 使用 `colsAppend` 添加新的 cols

"b" 与 col-b 重名，添加 `_` 后缀。`f.get(0, Integer.class)` 返回 col-0 的 Integer 值，`set(0, f.get(0, Integer.class) * 100)` 将该值设置为 col-b 的值。同理，`set(1,...)` 设置 col-c 的值。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .colsAppend("b", "c").merge((f, t) -> t
                .set(0, f.get(0, Integer.class) * 100)
                .set(1, f.get(0, Integer.class) * 10));
```

```
a b  b_  c
- - --- --
1 x 100 10
2 y 200 20
```

- 使用 `cols` 修改或添加 cols

这里修改 col-b，添加 col-c。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("b", "c").merge((f, t) -> t
                .set(0, f.get(0, Integer.class) * 100)
                .set(1, f.get(0, Integer.class) * 10));
```

```
a   b  c
- --- --
1 100 10
2 200 20
```

- 使用 col-index 选择 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols(1, 2).merge((f, t) -> t
                .set(0, f.get(0, Integer.class) * 100)
                .set(1, f.get(0, Integer.class) * 10)
        );
```

```
a   b  2
- --- --
1 100 10
2 200 20
```

- 自动推断 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols().merge(
                (f, t) -> {
                    t.set("b", f.get(0, Integer.class) * 100);
                    t.set("c", f.get(0, Integer.class) * 10);
                }
        );
```

```
a   b  c
- --- --
1 100 10
2 200 20
```

- `colsExcept` 排除指定 cols

排除 col-a，对 col-b 和 col-c 进行合并。因此 `RowMapper` 的 0 和 1 对应的是 col-b 和 col-c。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept("a").merge((f, t) -> t
                .set(0, f.get(0, Integer.class) * 100)
                .set(1, f.get(0, Integer.class) * 10));
```

```
a   b  c
- --- --
1 100 10
2 200 20
```

- `colsExcept` 排除指定 cols，col-index 为参数

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept(1).merge((f, t) -> t
                .set(0, f.get(0, Integer.class) * 100)
                .set(1, f.get(0, Integer.class) * 10));
```

```
  a b  c
--- - --
100 x 10
200 y 20
```

#### merge-RowToValueMapper

```java
DataFrame merge(RowToValueMapper<?>... mappers);
```

`RowToValueMapper` 定义 `RowProxy` 到值的映射。因此，一个值对应一个 `RowToValueMapper` 定义。

- 使用 `colsAppend` 添加 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .colsAppend("b", "c")
        .merge(r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
a b  b_  c
- - --- --
1 x 100 10
2 y 200 20
```

- 使用 `cols(...)` 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("b", "c").merge(r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
a   b  c
- --- --
1 100 10
2 200 20
```

- 使用 col-index 定义 cols

这里 col-1 对应 col-b，因此会修改 col-b 的值。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols(1, 2).merge(r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
a   b  2
- --- --
1 100 10
2 200 20
```

- 动态定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols().merge(r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
a b   2  3
- - --- --
1 x 100 10
2 y 200 20
```

- 使用 `colsExcept` 排斥指定 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept("a").merge(r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
a   b  c
- --- --
1 100 10
2 200 20
```

- 使用 `colsExcept` 排除指定 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept(1).merge(r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
  a b  c
--- - --
100 x 10
200 y 20
```

### selects

`select` 执行 col 转换操作。该操作分三步：

1. 使用 `cols` 定义 `ColumnSet`
2. 使用 `select` 对定义的 cols 执行操作
3. 以 `DataFrame` 返回转换后的 cols

其使用和 `merge` 类似，差别在于 `select` 返回的 `DataFrame` 只包含 `ColumnSet` 选择的 cols。 

```java
DataFrame select();
DataFrame select(Exp<?>... exps);
DataFrame select(RowToValueMapper<?>... mappers);
DataFrame select(RowMapper mapper);
```

后面三种方法类似。

例如：

```java
DataFrame df = DataFrame.foldByRow("first", "last", "middle").of(
        "Jerry", "Cosin", "M",
        "Joan", "O'Hara", null);

Exp fmExp = concat(
        $str("first"),
        ifNull($str("middle").mapVal(s -> " " + s), ""));

DataFrame df1 = df
        .cols("first_middle", "last") // (1)
        .select(fmExp, $str("last")); // (2)
```

```
first_middle last
------------ ------
Jerry M      Cosin
Joan         O'Hara
```

(1) 定义 `ColumnSet` 时，可以指定原 `DataFrame` 中不存在的 col，表示创建新的 col

(2) 对 `ColumnSet` 的每个 col，都应该有一个对应的 exp 来生成该 col。上面第一个 exp 转换数据，第二个 exp 直接选择一个 col。

使用 `RowMapper` 生成与上面一样的数据： 

```java
RowMapper mapper = (from, to) -> {
    String middle = from.get("middle") != null
            ? " " + from.get("middle")
            : "";
    to.set(0, from.get("first") + middle).set(1, from.get("last"));
};

DataFrame df1 = df
        .cols("first_middle", "last")
        .select(mapper);
```

另外还有一个 `RowToValueMapper`，其功能与上面差别不大。

#### select

```java
DataFrame select();
```

返回 `cols` 定义的 `ColumnSet`，不做额外修改。

- 使用 col-name 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "Y", "c")
        .select();
```

```
a Y    c
- ---- -
1 null a
2 null b
```

- 使用 col-index 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols(0, 2, 4)
        .select();
```

```
a c 4   
- - ----
1 a null
2 b null
```

- 自动推断 cols，默认选择所有 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols()
        .select();
```

```
a b c
- - -
1 x a
2 y b
```

#### select-Exp

- 使用 `colsAppend` 添加 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .colsAppend("b", "c")
        .select($int(0).mul(100), $int(0).mul(10));
```

```
 b_  c
--- --
100 10
200 20
```

- 使用 `cols(...)` 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("b", "c").select($int(0).mul(100),
                $int(0).mul(10));
```

```
  b  c
--- --
100 10
200 20
```

- 使用 `cols(..)` 定义重复 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("b", "b")
        .select($int(0).mul(100),
                $int(0).mul(10));
```

```
  b b_
--- --
100 10
200 20
```

- 使用 col-index 选择 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols(1, 7).select($int(0).mul(100),
                $int(0).mul(10));
```

```
  b  7
--- --
100 10
200 20
```

- 自动推断 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols().select($int(0).mul(100).as("b"),
                $int(0).mul(10));
```

```
  b a * 10
--- ------
100     10
200     20
```

- `colsExcept` 排除 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept("a").select($int(0).mul(100),
                $int(0).mul(10));
```

```
  b  c
--- --
100 10
200 20
```

- `colsExcept` 排除 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept(1)
        .select($int(0).mul(100),
                $int(0).mul(10));
```

```
  a  c
--- --
100 10
200 20
```

#### select-RowMapper

- 使用 col-names 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("b", "c")
        .select((f, t) -> t
                .set(0, f.get(0, Integer.class) * 100)
                .set(1, f.get(0, Integer.class) * 10));
```

```
  b  c
--- --
100 10
200 20
```

- 使用 col-index 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols(1, 2)
        .select((f, t) -> t
                .set(0, f.get(0, Integer.class) * 100)
                .set(1, f.get(0, Integer.class) * 10));
```

```
  b  2
--- --
100 10
200 20
```

- 自动推断 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols()
        .select(
                (f, t) -> {
                    t.set("b", f.get(0, Integer.class) * 100);
                    t.set("c", f.get(0, Integer.class) * 10);
                }
        );
```

```
  b  c
--- --
100 10
200 20
```

- `colsExcept` 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept("a")
        .select((f, t) -> t
                .set(0, f.get(0, Integer.class) * 100)
                .set(1, f.get(0, Integer.class) * 10));
```

```
  b  c
--- --
100 10
200 20
```

- `colsExcept` 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept(1)
        .select((f, t) -> t
                .set(0, f.get(0, Integer.class) * 100)
                .set(1, f.get(0, Integer.class) * 10));
```

```
  a  c
--- --
100 10
200 20
```

#### select-RowToValueMapper

- 使用 col-name 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("b", "c")
        .select(
                r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
  b  c
--- --
100 10
200 20
```

- 使用 col-index 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols(1, 2)
        .select(
                r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
  b  2
--- --
100 10
200 20
```

- 自动推断 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols()
        .select(
                r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10
        );
```

```
  0  1
--- --
100 10
200 20
```

- `colsExcept` 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept("a")
        .select(r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
  b  c
--- --
100 10
200 20
```

- `colsExcept` 以 col-index 定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "z", 2, "y", "a")
        .colsExcept(1)
        .select(r -> r.get(0, Integer.class) * 100,
                r -> r.get(0, Integer.class) * 10);
```

```
  a  c
--- --
100 10
200 20
```

### selectAs

`selectAs` 用于选择并重命名 col。

```java
DataFrame selectAs(UnaryOperator<String> renamer);
DataFrame selectAs(String... newColumnNames);
DataFrame selectAs(Map<String, String> oldToNewNames);
```

#### selectAs-array

数组中 col-names 顺序要求与 `ColumnSet` 中 cols 的顺序一致。

- 对所有 col 重命名

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols()
        .selectAs("c", "d");
```

```
c d
- -
1 x
2 y
```

- 用 col-name 选择一部分 cols 重命名

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c")
        .selectAs("X", "Y");
```

```
X Y
- -
1 a
2 b
```

- 添加新的 cols

`cols(..)` 定义的 cols 包含新的 col，在 `selectAs` 同时对其重命名。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c", "new")
        .selectAs("X", "Y", "NEW");
```

```
X Y NEW 
- - ----
1 a null
2 b null
```

#### selectAs-Map

- 选择所有 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols()
        .selectAs(Map.of("a", "c", "b", "d"));
```

```
c d
- -
1 x
2 y
```

- 使用 col-names 选择 col

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c")
        .selectAs(Map.of("a", "X", "c", "Y"));
```

```
X Y
- -
1 a
2 b
```

- 选择部分 cols，并添加新 col

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a", 2, "y", "b")
        .cols("a", "c", "new")
        .selectAs(Map.of("a", "X", "new", "NEW"));
```

```
X c NEW 
- - ----
1 a null
2 b null
```

#### selectAs-Operator

```java
DataFrame selectAs(UnaryOperator<String> renamer);
```

使用函数定义新的 col-names。

- 对所有 col 重命名

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols()
        .selectAs(c -> "[" + c + "]");
```

```
[a] [b]
--- ---
  1 x  
  2 y
```

- 使用 col-name 选择一部分 col 重命名

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a",
                2, "y", "b")
        .cols("a", "c")
        .selectAs(c -> "[" + c + "]");
```

```
[a] [c]
--- ---
  1 a  
  2 b 
```

- 添加新的 col

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(1, "x", "a",
                2, "y", "b")
        .cols("a", "c", "new")
        .selectAs(c -> "[" + c + "]");
```

```
[a] [c] [new]
--- --- -----
  1 a   null 
  2 b   null 
```

- 使用函数引用，将所有 col-names 转换为大写

```java
DataFrame df1 = df
        .cols("last", "first")
        .selectAs(String::toUpperCase);
```

```
LAST   FIRST
------ -----
Cosin  Jerry
O'Hara Joan
```

### expand

```java
DataFrame expand(Exp<? extends Iterable<?>> splitExp);
```

`expand` 和 `expandArray` 的功能相似，都是将 `splitExp` 生成的 cols 合并到现有 `DataFrame`，差别在于 `expand` 使用 `Iterable` 对象生成 row，而 `expandArray` 使用数组生成 row。

- 使用 list 定义 row 数据

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("c", "b").expand($val(List.of("one", "two")));
```

```
a b   c  
- --- ---
1 two one
2 two one
```

- 不同长度的 list

当不同 row 对应的 list 长度不一致，根据 `ColumnSet` 定义，多余的拆掉，不足的填充 null。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols("b", "c").expand($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return List.of("one");
                case 2:
                    return List.of("one", "two");
                case 3:
                    return List.of("one", "two", "three");
                default:
                    return null;
            }
        }));
```

```
a b   c   
- --- ----
1 one null
2 one two 
3 one two
```

- 某个 row 对应的 list 为 null，则该 row 所有值均为 null

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols("b", "c").expand($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return List.of("one");
                case 3:
                    return List.of("one", "two", "three");
                default:
                    return null;
            }
        }));
```

```
a b    c   
- ---- ----
1 one  null
2 null null
3 one  two 
```

- 采用 `cols()` 动态定义 `ColumnSet`，即根据后面的表达式确定要添加和修改的 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols().expand($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return List.of("one");
                case 2:
                    return List.of("one", "two");
                case 3:
                    return List.of("one", "two", "three");
                default:
                    return null;
            }
        }));
```

```
a b 2   3    4    
- - --- ---- -----
1 x one null null 
2 y one two  null 
3 z one two  three
```

### selectExpand

```java
DataFrame selectExpand(Exp<? extends Iterable<?>> splitExp);
```

`selectExpand` 拆分 col 生成新的 cols，其功能与 `selectExpandArray` 类似。

即先 `select`，然后进行 `expand` 操作。

拆分出的 cols 数取决于 `cols` 定义。

- `List` 提供数据

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("c", "b")
        .selectExpand($val(List.of("one", "two")));
```

```
c   b  
--- ---
one two
one two
```

- `List` 提供数据，长度不一致

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols("b", "c")
        .selectExpand($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return List.of("one");
                case 2:
                    return List.of("one", "two");
                case 3:
                    return List.of("one", "two", "three");
                default:
                    return null;
            }
        }));
```

```
b   c   
--- ----
one null
one two 
one two 
```

- null 值

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols("b", "c")
        .selectExpand($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return List.of("one");
                case 3:
                    return List.of("one", "two", "three");
                default:
                    return null;
            }
        }));
```

```
b    c   
---- ----
one  null
null null
one  two 
```

- 动态大小

使用 `cols()`，生成的 cols 数为最长 `List`的 size。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols()
        .selectExpand($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return List.of("one");
                case 2:
                    return List.of("one", "two");
                case 3:
                    return List.of("one", "two", "three");
                default:
                    return null;
            }
        }));
```

```
0   1    2    
--- ---- -----
one null null 
one two  null 
one two  three
```

### expandArray

```java
DataFrame expandArray(Exp<? extends Object[]> splitExp);
```

将 `splitExp` 定义的 cols 与 `DataFrame` 合并。

对定宽 `ColumnSet`，`splitExp` 数组的每个值对应一个 col：

- 如果 `splitExp` 生成的值不足以填充 row，则末尾用 null 填充；
- 如果 `splitExp` 生成的值超过 row 宽度，则忽略多出来的值。

对动态 `ColumnSet`，生成 col 数等于 `splitExp` 生成的最长 row。

- 合并 col-c，修改 col-b

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("c", "b").expandArray($val(new String[]{"one", "two"}));
```

```
a b   c  
- --- ---
1 two one
2 two one
```

`$val` 生成常量 `Series`。

- 不同长度的数组

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols("b", "c").expandArray($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return new String[]{"one"};
                case 2:
                    return new String[]{"one", "two"};
                case 3:
                    return new String[]{"one", "two", "three"};
                default:
                    return null;
            }
        }));
```

```
a b   c   
- --- ----
1 one null
2 one two 
3 one two
```

col-a 保持不变，修改 col-b，添加 col-c。这里将 col-a 的值映射为不同长度的数组。其中 3 超出部分忽略，1 长度不够用 null 补齐。

- null 数组

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols("b", "c").expandArray($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return new String[]{"one"};
                case 3:
                    return new String[]{"one", "two", "three"};
                default:
                    return null;
            }
        }));
```

`cols("b", "c")` 选择 col-b 和 col-c，表示创建 col-c，修改 col-b。

col-a 的 2 对应 null，因此该 row 的 col-b 和 col-c 都是 null。

```
a b    c   
- ---- ----
1 one  null
2 null null
3 one  two 
```

- **动态大小**

上面的示例都通过 `cols("b", "c")` 定义好了 `ColumnSet` 宽度，如果用 `cols()` 则动态生成 cols，`ColumnSet` 宽度取决于后面的表达式。

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols().expandArray($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return new String[]{"one"};
                case 2:
                    return new String[]{"one", "two"};
                case 3:
                    return new String[]{"one", "two", "three"};
                default:
                    return null;
            }
        }));
```

```
a b 2   3    4    
- - --- ---- -----
1 x one null null 
2 y one two  null 
3 z one two  three
```

这里，col-a 和 col-b 保持不变，数组最长为 3，因此添加 3 个新的 cols。新 cols 的 labels 默认为 col-index。

- 如果要拆分的 col 是 `String` 类型，而非数组类型，可以使用 `split(..)` 将字符串拆分为数组

```java
DataFrame df = DataFrame.foldByRow("name", "phones").of(
        "Cosin", "111-555-5555,111-666-6666,111-777-7777",
        "O'Hara", "222-555-5555");

DataFrame df1 = df
        .cols("primary_phone", "secondary_phone")
        .selectExpandArray($str("phones").split(','));
```

### selectExpandArray

```java
DataFrame selectExpandArray(Exp<? extends Object[]> splitExp);
```

`select` 与 `expandArray` 的合并操作。

- 常量

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y")
        .cols("c", "b")
        .selectExpandArray($val(new String[]{"one", "two"}));
```

```
c   b  
--- ---
one two
one two
```

`selectExpandArray` 选择 `cols("c", "b")` 定义的 `ColumnSet`，并对齐进行 `expandArray` 操作。

- 变长

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols("b", "c")
        .selectExpandArray($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return new String[]{"one"};
                case 2:
                    return new String[]{"one", "two"};
                case 3:
                    return new String[]{"one", "two", "three"};
                default:
                    return null;
            }
        }));
```

```
b   c   
--- ----
one null
one two 
one two 
```

- null 对应 row 的值全部为 null

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols("b", "c")
        .selectExpandArray($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return new String[]{"one"};
                case 3:
                    return new String[]{"one", "two", "three"};
                default:
                    return null;
            }
        }));
```

```
b    c   
---- ----
one  null
null null
one  two 
```

- 变长，自动定义 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b")
        .of(1, "x", 2, "y", 3, "z")
        .cols()
        .selectExpandArray($int("a").mapVal(i -> {
            switch (i) {
                case 1:
                    return new String[]{"one"};
                case 2:
                    return new String[]{"one", "two"};
                case 3:
                    return new String[]{"one", "two", "three"};
                default:
                    return null;
            }
        }));
```

```
0   1    2    
--- ---- -----
one null null 
one two  null 
one two  three
```

### fills

```java
DataFrame fill(Object... values);
DataFrame fillNulls(Object value);
DataFrame fillNullsBackwards();
DataFrame fillNullsForward();
DataFrame fillNullsFromSeries(Series<?> series);
DataFrame fillNullsWithExp(Exp<?> replacementValuesExp);
```

- `fill` 参数 `values` 长度与 `ColumnSet` 宽度相同，对应 row 的每个值
- `fillNulls` 使用常量替换 null 值
- `fillNullsBackwards()` 使用相邻靠前的 non-null 值替换 `null` 值
- `fillNullsForward()` 使用相邻靠后的 non-null 值替换 `null` 值
- `fillNullsFromSeries` 使用另一个 `Series` 等 index 的元素替换 `null` 值
- `fillNullsWithExp` 则使用表达式生成的值替换 `null` 值

#### fill

 `fill` 用指定数组的值替换 `ColumnSet` 原始值。

- 替换所有 cols 的值

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", null,
                null, null, "a",
                3, null, null
        )
        .cols().fill("B", "C", "*");
```

```
a b c
- - -
B C *
B C *
B C *
```

- 替换部分 cols 的值

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", null,
                null, null, "a",
                3, null, null
        )
        .cols("b", "c", "new").fill("B", "C", "*");
```

```
   a b c new
---- - - ---
   1 B C *  
null B C *  
   3 B C * 
```

#### fillNulls

`fillNulls` 用常量替换 null 值。

- 使用 `cols()` 无目标 col，表示替换所有 null

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", null,
                null, null, "a",
                3, null, null
        )
        .cols().fillNulls("*");
```

```
a b c
- - -
1 x *
* * a
3 * *
```

- 使用 `cols("b", "c", "new")` 指定要替换 null 的 cols

替换 col-b 和 col-c 中的 null，并创建 col-new，新创建的 col-new 全都是 null 值，已经替换为 `*`。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", null,
                null, null, "a",
                3, null, null
        )
        .cols("b", "c", "new").fillNulls("*");
```

```
   a b c new
---- - - ---
   1 x * *  
null * a *  
   3 * * *  
```

#### fillNullsBackwards

`fillNullsBackwards()` 用相邻高 index 的 non-null 值替换 `null`

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", null,
                null, null, "a",
                3, null, null
        )
        .cols().fillNullsBackwards();
```

```
a b    c   
- ---- ----
1 x    a   
3 null a   
3 null null
```

#### fillNullsForward

`fillNullsForward()` 用相邻低 index 的  non-null 值替换 `null`。

- 使用 `cols()` 替换所有 cols 的 null 值

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", null,
                null, null, "a",
                3, null, null
        )
        .cols().fillNullsForward();
```

```
a b c   
- - ----
1 x null
1 x a   
3 x a   
```

- 使用 `cols("b", "c", "new")` 替换指定 cols 的 null 值

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", null,
                null, null, "a",
                3, null, null
        )
        .cols("b", "c", "new").fillNullsForward();
```

```
   a b c    new 
---- - ---- ----
   1 x null null
null x a    null
   3 x a    null
```

#### fillNullsFromSeries

`fillNullsFromSeries` 使用另一个 `Series` 等 index 元素替换 `null`。

- 替换所有 cols 的 null 值

```java
Series<String> filler = Series.of("row1", "row2", "row3");

DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", null,
                null, null, "a",
                3, null, null
        )
        .cols().fillNullsFromSeries(filler);
```

```
a    b    c   
---- ---- ----
1    x    row1
row2 row2 a   
3    row3 row3
```

- 替换指定 cols 的 null 值

```java
Series<String> filler = Series.of("row1", "row2", "row3");

DataFrame df = DataFrame.foldByRow("a", "b", "c").of(
                1, "x", null,
                null, null, "a",
                3, null, null
        )
        .cols("b", "c", "new").fillNullsFromSeries(filler);
```

```
   a b    c    new 
---- ---- ---- ----
   1 x    row1 row1
null row2 a    row2
   3 row3 row3 row3
```

> [!NOTE]
>
> filler `Series` 不需要与 `DataFrame` 一样高，值从 0 开始对齐。

#### fillNullsWithExp

使用 exp 生成的值替换 null 值。

```java
DataFrame df = DataFrame.foldByRow("c1", "c2").of(
                "a1", "a2",
                null, null,
                "b1", "b2");

DataFrame clean = df.cols("c1", "c2")
        .fillNullsWithExp(rowNum()); 
```

`Exp.rowNum()` 生成一个包含行号的 `Series`，因此 `null` 值被替换为所在 row 的行号。

```
c1 c2
-- --
a1 a2
2  2
b1 b2
```

### agg

```java
DataFrame agg(Exp<?>... aggregators);
```

使用指定聚合表达式对 `ColumnSet` 的 cols 进行计算，得到**单行** `DataFrame`。

#### avg

- 计算平均值

```java
NumExp<?> avg();
NumExp<?> avg(Condition filter);
```

计算平均值通过两个 exp 实现，其中支持数据过滤。

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 4L,
        0, 55.5);

DataFrame agg = df.cols().agg(
        $int("a").avg(), // 取 col-a，计算均值
        $double(1).avg()); // 取 col-q 计算均值

new DataFrameAsserts(agg, "avg(a)", "avg(b)")
        .expectHeight(1)
        .expectRow(0, 0.5, 29.75);
```

- 过滤后计算平均值

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 4L,
        5, 8L,
        0, 55.5);

DataFrame agg = df.cols().agg(
        $double("a").avg($int(0).ne(5)), // 取 col-0 不等于 5 的 rows
        $double(1).avg($int(0).ne(5)));

new DataFrameAsserts(agg, "avg(a)", "avg(b)")
        .expectHeight(1)
        .expectRow(0, 0.5, 29.75);
```

#### median

```java
default NumExp<?> median();
default NumExp<?> median(Condition filter);
```

计算中位数。

- 奇数个数，median 为中间的数

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 100.,
        0, 55.5,
        4, 0.);

DataFrame agg = df.cols().agg(
        $int("a").median(),
        $double(1).median());
```

```
median(a) median(b)
--------- ---------
      1.0      55.5
```

- 偶数个数，median 为中间两个数的平均值

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 100.,
        0, 55.5,
        4, 0.,
        3, 5.);

DataFrame agg = df.cols().agg(
        $int("a").median(),
        $double(1).median());
```

```
median(a) median(b)
--------- ---------
      2.0     30.25
```

- 空集，median 为 0

```java
DataFrame df = DataFrame.empty("a", "b");

DataFrame agg = df.cols().agg(
        $int("a").median(),
        $double(1).median());
```

```
median(a) median(b)
--------- ---------
      0.0       0.0
```

- 一个数，直接返回该数

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(1, 100);

DataFrame agg = df.cols().agg(
        $int("a").median(),
        $int(1).median());
```

```
median(a) median(b)
--------- ---------
      1.0     100.0
```

- null 值被跳过

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, null,
        0, 55.5,
        4, 0.,
        null, 5.);

DataFrame agg = df.cols().agg(
        $int("a").median(),
        $double(1).median());
```

```
median(a) median(b)
--------- ---------
      1.0       5.0
```

#### min 和 max

```java
default NumExp<?> min();
default NumExp<?> min(Condition filter);
default NumExp<?> max();
default NumExp<?> max(Condition filter);
```

计算每个 col 的最小值或最大值。

- 计算 col-a 的最小值和最大值

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 1,
        -1, 1,
        8, 1);

DataFrame agg = df.cols().agg(
        $int("a").min(),
        $int("a").max());
```

```
min(a) max(a)
------ ------
    -1      8
```

- 过滤后计算

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1L, 1L,
        2L, 4L,
        -1L, 5L,
        8L, 2L);

DataFrame agg = df.cols().agg(
        $long(1).max($long(0).mod(2).eq(0L)), // col-0 为偶数过滤
        $long(1).min($long(0).mod(2).eq(0L)), // 取 col-1 的 min 和 max
        $long("a").max($long("b").mod(2).eq(1L)), // col-b 为奇数过滤
        $long("a").min($long("b").mod(2).eq(1L)) // 取 col-a 的 min 和 max
);
```

```
max(b) min(b) max(a) min(a)
------ ------ ------ ------
     4      2      1     -1
```

该操作对 `$int` 和 `$double` 都适用。

#### sum

```java
default NumExp<?> sum();
default NumExp<?> sum(Condition filter);
```

计算加和。

- 示例

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 1,
        -1, 5L);

DataFrame agg = df.cols().agg(
        $int("a").sum(),
        $long(1).sum());
```

```
sum(a) sum(b)
------ ------
     0      6
```

- 过滤后计算 sum

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 1,
        -1, 5,
        2, 6,
        -4, 5);

DataFrame agg = df.cols().agg(
        // 过滤：col-0 为偶数时，计算 col-1 加和
        $int(1).sum($int(0).mod(2).eq(0)),
        // 过滤：col-b 为奇数时，计算 col-a 加和
        $int("a").sum($int("b").mod(2).eq(1)));
```

```
sum(b) sum(a)
------ ------
    11     -4
```



#### set

```java
Exp<Set<T>> set();
```

将所有值收集到一个 `Set` 中，包含包含该 `Set` 的 `Series`。

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "x",
        1, "a");

// col-a 和 col-1 都转换为 set
DataFrame agg = df.cols().agg($col("a").set(), $col(1).set());

new DataFrameAsserts(agg, "a", "b")
        .expectHeight(1)
        .expectRow(0, Set.of(1, 2), Set.of("x", "a"));
```

#### list

```java
Exp<List<T>> list()
```

将 `Series` 的所有值包含到一个 `List`。

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "x",
        1, "a");

DataFrame agg = df.cols().agg($col("a").list(), $col(1).list());

new DataFrameAsserts(agg, "a", "b")
        .expectHeight(1)
        .expectRow(0, List.of(1, 2, 1), List.of("x", "x", "a"));
```

#### count

```java
static Exp<Integer> count();
static Exp<Integer> count(Condition filter);
```

返回 row 数目。

- count 得到 row 数，返回一个 1x1 的 `DataFrame`

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        0, "a");

DataFrame agg = df.cols().agg(count());

new DataFrameAsserts(agg, "count")
        .expectHeight(1)
        .expectRow(0, 2);
```

- 过滤后计数，不同 col 得到的 count 数不同

```jade
DataFrame df = DataFrame.foldByRow("a", "b").of(
        7, 1,
        -1, 5,
        -4, 5);

DataFrame agg = df.cols().agg(
        count($int(0).mod(2).eq(0)), // 偶数个数
        count($int("b").mod(2).eq(1)) // 奇数个数
);

new DataFrameAsserts(agg, "count", "count_")
        .expectHeight(1)
        .expectRow(0, 1, 3);
```

#### first 和 last

```java
default Exp<T> first();
default Exp<T> first(Condition filter);
default Exp<T> last();
```

取第一个或最后一个元素。

- 取第一个值

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 100,
        2, 5);

DataFrame agg = df.cols().agg(
        $col("a").first(),
        $col(1).first());

new DataFrameAsserts(agg, "a", "b")
        .expectHeight(1)
        .expectRow(0, 1, 100);
```

- 空集 `first()` 返回 null

```java
DataFrame df = DataFrame.empty("a", "b");

DataFrame agg = df.cols().agg(
        $col("a").first(),
        $col(1).first());

new DataFrameAsserts(agg, "a", "b")
        .expectHeight(1)
        .expectRow(0, null, null);
```

- 第一个 row 包含 null

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, null,
        null, 5);

DataFrame agg = df.cols().agg(
        $col("a").first(),
        $col(1).first());

new DataFrameAsserts(agg, "a", "b")
        .expectHeight(1)
        .expectRow(0, 1, null);
```

- 过滤再取 first

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        7, 1,
        -1, 5,
        -4, 5);

DataFrame agg = df.cols().agg(
        $col(1).first($int(0).mod(2).eq(0)), // col-0 为偶数时取 col-1 的值
        $col("a").first($int("b").mod(2).eq(1))); // col-b 为奇数时取 col-a 的值

new DataFrameAsserts(agg, "b", "a")
        .expectHeight(1)
        .expectRow(0, 5, 7);
```

- 过滤后没有任何值时，first 返回 null

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        7, 1,
        -1, 5,
        -4, 5);

DataFrame agg = df.cols().agg(
        $col(1).first($val(false).castAsBool()),
        $col("a").first($int("b").mod(2).eq(1)));
```

```
b    a
---- -
null 7
```

- 取最后一个值

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 100,
        2, 5);

DataFrame agg = df.cols().agg(
        $col("a").last(),
        $col(1).last());
```

```
a b
- -
2 5
```

#### vConcat

```java
default Exp<String> vConcat(String delimiter);
default Exp<String> vConcat(Condition filter, String delimiter);
default Exp<String> vConcat(String delimiter, String prefix, String suffix);
default Exp<String> vConcat(Condition filter, String delimiter, String prefix, String suffix);
```

应用于字符串类型 `Series`，串联起来生成单个值的 `Series`。

- `delimiter` 指定连接字符串
- `prefix` 指定连接后添加的前缀、
- `suffix` 指定连接后添加的后缀

- 示例

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        0, "a");

DataFrame agg = df.cols().agg(
        $col("a").vConcat("_"),
        $col(1).vConcat(" ", "[", "]"));
```

```
a   b    
--- -----
1_0 [x a]
```

- 过滤后再串联

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        7, 1,
        -1, 5,
        -4, 5,
        8, 8);

DataFrame agg = df.cols().agg(
        $col(1).vConcat($int(0).mod(2).eq(0), "_"),
        $col("a").vConcat($int("b").mod(2).eq(1), ", ", "[", "]"));
```

```
b   a          
--- -----------
5_8 [7, -1, -4]
```



#### Function

agg 操作本质是应用于 `Series`，生成单个值的函数。为此 DFLib 提供了以下两个通用方法：

```java
default <A> Exp<A> agg(Function<Series<T>, A> aggregator);
default <A> Exp<A> agg(Condition filter, Function<Series<T>, A> aggregator);
```

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, 100,
        2, 5);

// 返回 Series 的 size
DataFrame agg = df.cols().agg($col(1).agg(Series::size));

new DataFrameAsserts(agg, "b")
        .expectHeight(1)
        .expectRow(0, 2);
```

```
b
-
2
```

### colsSample

```java
default ColumnSet colsSample(int size);
default ColumnSet colsSample(int size, Random random);
```

随机抽取 col。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c", "d").of(
                1, "x", "m", "z",
                2, "y", "x", "E")
        // using fixed seed to get reproducible result
        .colsSample(2, new Random(11)).select();
```

```
b a
- -
x 1
y 2
```

### drop

删除 col 的操作有两种实现方式：

1. 定义 `ColumnSet` 选择要删除的 cols，调用 `drop()` 删除
2. 定义 `ColumnSet` 选择要保留的 col，调用 `select()` 保留

例如：

```java
DataFrame df1 = df.cols("middle").drop();

DataFrame df1 = df.colsExcept("middle").select();
```

两种方法得到的结果相同：

```
first last
----- ------
Jerry Cosin
Joan  O'Hara
```

## RowSet

### 创建 RowSet

基于 row 的操作，可以分三步完成：

1. 定义 `RowSet`
2. 执行操作
3. 将结果合并到原 `DataFrame` (`merge`)，或作为独立的 `DataFrame` (`select`)

所有 row 操作基本都是在 `RowSet` 对象上完成。创建 `RowSet` 的方式可以分为四类：

1. by-condition：通过 `DataFrame.rows()` 实现
2. by-index：通过 `DataFrame.rows()` 实现
3. by-range：通过 `DataFrame.rowsRange()` 实现
4. sample：通过 `DataFrame.rowsSample()` 实现

- 选择所有 rows:

```java
RowSet rows();
```

- 选择指定位置的 rows：

```java
RowSet rows(int... positions);
RowSet rows(IntSeries positions);
```

- 选择满足条件的 rows

```java
RowSet rows(Condition rowCondition);
RowSet rows(BooleanSeries condition);
RowSet rows(RowPredicate condition);
```

- 选择指定范围的 rows

```java
RowSet rowsRange(int fromInclusive, int toExclusive);
```

- `rowsExcept` 选择不满足指定条件的 rows

```java
RowSet rowsExcept(int... positions);
RowSet rowsExcept(IntSeries positions);
RowSet rowsExcept(RowPredicate condition);
RowSet rowsExcept(Condition condition);
```

- 随机抽样

```java
RowSet rowsSample(int size);
RowSet rowsSample(int size, Random random);
```

#### rows

`rows()` 选择 `DataFrame` 的所有 rows。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows()
        .select();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 1, "x", "a")
        .expectRow(1, 2, "y", "b")
        .expectRow(2, -1, "m", "n");
```

#### rows-condition

```java
RowSet rows(Condition rowCondition);
RowSet rows(BooleanSeries condition);
RowSet rows(RowPredicate condition);
```

选择满足指定条件的 rows，即过滤 rows。

- `Condition` (boolean `Exp`) 选择匹配的 rows

```java
DataFrame df = DataFrame.foldByRow("first", "last", "middle").of(
        "Jerry", "Cosin", "M",
        "Juliana", "Walewski", null,
        "Joan", "O'Hara", "P");

DataFrame df1 = df
        .rows($str("last").startsWith("W").eval(df)) // 选择满足条件的 rows，保存为 RowSet 
        .select();  // 将 RowSet 转换为 DataFrame
```

```
first   last     middle
------- -------- ------
Juliana Walewski null
```

- 另一种形式的 condition 为 `RowPredicate`

```java
DataFrame df1 = df
        .rows(r -> r.get("last", String.class).startsWith("W"))
        .select();
```

- `cond` 可以是预先算好的 `BooleanSeries`

一种常见的场景是在一个 `Series` 或 `DataFrame`/`RowSet` 调用 `locate()` 构建 `BooleanSeries` selector，然后使用它从另一个 `DataFrame`  选择 rows：

```java
// 创建 salaries Series，其大小与 DataFrame 的 rows 数目一样
IntSeries salaries = Series.ofInt(100000, 50000, 45600); 
// 创建可复用 selector
BooleanSeries selector = salaries.locateInt(s -> s > 49999); 

DataFrame df1 = df.rows(selector).select();
```

```
first   last     middle
------- -------- ------
Jerry   Cosin    M
Juliana Walewski null
```

#### rows-index

```java
RowSet rows(int... positions)
RowSet rows(IntSeries positions)
```

选择指定位置的 rows。

> [!TIP]
>
> 该方式可以不按顺序选择，还可以重复选择 rows。

- 选择 index 0,2 两行

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt(0, 2)).select();
new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(2)
        .expectRow(0, 1, "x", "a")
        .expectRow(1, -1, "m", "n");
```

- 什么都不选

```jade
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt()).select();

new DataFrameAsserts(df, "a", "b", "c").expectHeight(0);
```

- 重复选择 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(0, 2, 2, 0).select();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(4)
        .expectRow(0, 1, "x", "a")
        .expectRow(1, -1, "m", "n")
        .expectRow(2, -1, "m", "n")
        .expectRow(3, 1, "x", "a");
```

- 使用 `IntSeries` 定义位置

和 cond 一样，通常使用另一个 `Series` 或 `DataFrame`/`RowSet` 计算得到 `IntSeries`：

```java
IntSeries selector = salaries.indexInt(s -> s > 49999); // 创建包含位置的 selector

DataFrame df1 = df.rows(selector).select();
```

```
first   last     middle
------- -------- ------
Jerry   Cosin    M
Juliana Walewski null
```

#### rowsRange

```java
RowSet rowsRange(int fromInclusive, int toExclusive);
```


- `rowsRange` 选择指定范围的 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rowsRange(1, 2).select(); // [startIdx, endIdx)

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(1)
        .expectRow(0, 2, "y", "b");
```

- startIdx 和 endIdx 相同时，什么也不选

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rowsRange(1, 1).select();

new DataFrameAsserts(df, "a", "b", "c").expectHeight(0);
```

- 选择所有 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rowsRange(0, 3).select();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 1, "x", "a")
        .expectRow(1, 2, "y", "b")
        .expectRow(2, -1, "m", "n");
```

### drop

```java
DataFrame drop();
```

删除 `RowSet`，返回余下的 `DataFrame`。

删除 row 的操作有两种方式：

- 选择要删除的 rows，调用 `drop()`；
- 选择需要的 rows，调用 `select()`

如下所示：

```java
DataFrame df = DataFrame.foldByRow("first", "last", "middle").of(
        "Jerry", "Cosin", "M",
        "Juliana", "Walewski", null,
        "Joan", "O'Hara", "P");

DataFrame df1 = df.rows($col("middle").isNull()).drop();
```

```java
DataFrame df = DataFrame.foldByRow("first", "last", "middle").of(
        "Jerry", "Cosin", "M",
        "Juliana", "Walewski", null,
        "Joan", "O'Hara", "P");

DataFrame df1 = df.rowsExcept($col("middle").isNull()).select();
```

两者得到相同结果：

```
first last   middle
----- ------ ------
Jerry Cosin  M
Joan  O'Hara P
```

下面详细介绍第一种方式。

- 删除所有 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows()
        .drop();

new DataFrameAsserts(df, "a", "b", "c").expectHeight(0);
```

- 删除指定位置的 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(0, 2)
        .drop();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(1)
        .expectRow(0, 2, "y", "b");
```

- 如果定义的 `RowSet` 为空，自然不会删除任何数据

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt())
        .drop();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 1, "x", "a")
        .expectRow(1, 2, "y", "b")
        .expectRow(2, -1, "m", "n");
```

- 定义的 `RowSet` 包含重复值不影响删除

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(0, 2, 2, 0)
        .drop();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(1)
        .expectRow(0, 2, "y", "b");
```

- 删除指定范围的 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rowsRange(1, 2)
        .drop();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(2)
        .expectRow(0, 1, "x", "a")
        .expectRow(1, -1, "m", "n");
```

- 如果定义的范围为空，不删除数据

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rowsRange(1, 1)
        .drop();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 1, "x", "a")
        .expectRow(1, 2, "y", "b")
        .expectRow(2, -1, "m", "n");
```

- 使用 `BooleanSeries` 定义删除的 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofBool(true, false, true))
        .drop();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(1)
        .expectRow(0, 2, "y", "b");
```

- 随机抽样删除

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rowsSample(2, new Random(9))
        .drop();

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(1)
        .expectRow(0, 2, "y", "b");
```


### index

```java
IntSeries index();
```

返回 `RowSet` 所含值在原 `DataFrame` 中的位置。

- 所有 rows 的 index

```java
IntSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows()
        .index();

new IntSeriesAsserts(index).expectData(0, 1, 2);
```

- 选择部分 rows

```java
IntSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt(0, 2))
        .index();

new IntSeriesAsserts(index).expectData(0, 2);
```

- 重复选择

```java
IntSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .cols(0).compactInt(0)
        .rows(Series.ofInt(0, 2, 2, 0))
        .index();

// duplicates and ordering of RowSet is preserved
new IntSeriesAsserts(index).expectData(0, 2, 2, 0);
```

- 范围

```java
IntSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rowsRange(1, 3)
        .index();

new IntSeriesAsserts(index).expectData(1, 2);
```

- 按条件选择

```java
IntSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofBool(true, false, true))
        .index();

new IntSeriesAsserts(index).expectData(0, 2);
```

- 按条件选择

```java
IntSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .cols(0).compactInt(0)
        .rows(r -> Math.abs(r.getInt(0)) == 1).index();

new IntSeriesAsserts(index).expectData(0, 2);
```

### locate

```java
BooleanSeries locate();
```

返回与 `DataFrame` height 相同的 `BooleanSeries`，`DataFrame` 对应位置的 row 是否在 `RowSet` 中。

- 空

```java
BooleanSeries index = DataFrame.empty("a", "b", "c")
        .rows().locate();

new BoolSeriesAsserts(index).expectData();
```

- 所有

```java
BooleanSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows()
        .locate();

new BoolSeriesAsserts(index).expectData(true, true, true);
```

- 用 index 选择 rows

```java
BooleanSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt(0, 2))
        .locate();

new BoolSeriesAsserts(index).expectData(true, false, true);
```

- 选择重复 rows

```java
BooleanSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .cols(0).compactInt(0)
        .rows(Series.ofInt(0, 2, 2, 0))
        .locate();

new BoolSeriesAsserts(index).expectData(true, false, true);
```

- 选择一定范围 rows

```java
BooleanSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rowsRange(1, 3)
        .locate();

new BoolSeriesAsserts(index).expectData(false, true, true);
```

- 按条件选择

```java
BooleanSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofBool(true, false, true))
        .locate();
new BoolSeriesAsserts(index).expectData(true, false, true);

BooleanSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofBool(false, false, false))
        .locate();
new BoolSeriesAsserts(index).expectData(false, false, false);

BooleanSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .cols(0).compactInt(0)
        .rows(r -> Math.abs(r.getInt(0)) == 1).locate();
new BoolSeriesAsserts(index).expectData(true, false, true);

BooleanSeries index = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .cols(0).compactInt(0)
        .rows(r -> false).locate();
new BoolSeriesAsserts(index).expectData(false, false, false);
```

### merge

```java
DataFrame merge(Exp<?>... exps);
DataFrame merge(RowMapper mapper);
DataFrame merge(RowToValueMapper<?>... mappers);
```

使用 `Exp` 对 `RowSet` 进行转换，然后将 `RowSet` 与原 `DataFrame` 合并。

row 合并规则：

- 通过 row pos 合并
  - `DataFrame` 中位置匹配的 rows 被替换为 `RowSet` 中转换后的 rows
  - `DataFrame` 中与 `RowSet` 不匹配的 rows 不变
  - `RowSet` 包含 `DataFrame` 不包含的 rows (如重复 row 和分割 row) 添加到底部
- `RowSet` 中 row 的顺序不影响 `DataFrame` 中被替换的 row 的顺序
  - 附加到底部的 rows 与 `RowSet` 中的顺序一致

> [!NOTE]
>
> 和 col 一样，大多数 `RowSet.select(..)` 方法都有对应的 merge 方法，如 `map()`, `expand()`, `unique(..)` 等

- 对所有 rows 进行操作

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows()
        .merge(
                Exp.$int(0).mul(3), // col-0 x 3
                Exp.concat(Exp.$str(1), Exp.$str(2)), // col-1 串联
                Exp.$str(2) // col-2 原 col-2
        );

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 3, "xa", "a")
        .expectRow(1, 6, "yb", "b")
        .expectRow(2, -3, "mn", "n");
```

- 选部分 row 操作

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt(0, 2))
        .merge(
                Exp.$int(0).mul(3),
                Exp.concat(Exp.$str(1), Exp.$str(2)),
                Exp.$str(2)
        );

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 3, "xa", "a")
        .expectRow(1, 2, "y", "b")
        .expectRow(2, -3, "mn", "n");
```

- 重复选择的 row，会得到重复结果

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a", //<-
                2, "y", "b",
                -1, "m", "n") //<-
        .cols(0).compactInt(0)
        .rows(Series.ofInt(0, 2, 2, 0))
        .merge(
                Exp.$int(0).mul(3),
                Exp.concat(Exp.$str(1), Exp.$str(2)),
                Exp.$str(2)
        );

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(5)
        .expectRow(0, 3, "xa", "a") // 0
        .expectRow(1, 2, "y", "b") // 1
        .expectRow(2, -3, "mn", "n") // 2
        .expectRow(3, -3, "mn", "n") // 2
        .expectRow(4, 3, "xa", "a"); // 0
```

#### merge-Mapper

类似：

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .cols(0).compactInt(0)
        .rows()
        .merge(
                r -> r.getInt(0) * 3,
                r -> r.get(1) + "" + r.get(2),
                r -> r.get(2)
        );

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 3, "xa", "a")
        .expectRow(1, 6, "yb", "b")
        .expectRow(2, -3, "mn", "n");
```

#### merge-RowMapper

`RowMapper` 比 `RowToValueMapper` 更灵活一点。但是用法都差不多：

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .cols(0).compactInt(0)
        .rows()
        .merge((f, t) -> {
            t.set(0, f.getInt(0) * 3);
            t.set(1, f.get(1) + "" + f.get(2));
            t.set(2, f.get(2));
        });

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 3, "xa", "a")
        .expectRow(1, 6, "yb", "b")
        .expectRow(2, -3, "mn", "n");
```

### select

```java
DataFrame select();
DataFrame select(Exp<?>... exps);
DataFrame select(RowMapper mapper);
DataFrame select(RowToValueMapper<?>... mappers);
```

`select()` 将 `RowSet` 转换为 `DataFrame`，`select(..)` 则根据条件转换后再返回 `DataFrame`。

> [!TIP]
>
> `select` 和 `merge` 对选择的 `RowSet` 行转换操作，然后返回 `RowSet` 的数据（`select`），或者与原 `DataFrame` 合并后返回（`merge`）。

**select(Exp<?>... exps)**

对 `RowSet` 应用 `Exp`，并将其返回为 `DataFrame`。添加几个 `Exp`，返回的 `DataFrame` 就有几个 cols。

- 所有 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows()
        .select(
                Exp.$int(0).mul(3),
                Exp.concat(Exp.$str(1), Exp.$str(2)),
                Exp.$str(2)
        );

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 3, "xa", "a")
        .expectRow(1, 6, "yb", "b")
        .expectRow(2, -3, "mn", "n");
```

- 所选 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt(0, 2))
        .select(
                Exp.$int(0).mul(3),
                Exp.concat(Exp.$str(1), Exp.$str(2)),
                Exp.$str(2)
        );

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(2)
        .expectRow(0, 3, "xa", "a")
        .expectRow(1, -3, "mn", "n");
```

- 重复 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(0, 2, 0, 2)
        .select(
                Exp.$int(0).mul(3),
                Exp.concat(Exp.$str(1), Exp.$str(2)),
                Exp.$str(2)
        );

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(4)
        .expectRow(0, 3, "xa", "a")
        .expectRow(1, -3, "mn", "n")
        .expectRow(2, 3, "xa", "a")
        .expectRow(3, -3, "mn", "n");
```

- 空 rows，不会 col-labels 依然保留

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt())
        .select(
                Exp.$int(0).mul(3),
                Exp.concat(Exp.$str(1), Exp.$str(2)),
                Exp.$str(2)
        );

new DataFrameAsserts(df, "a", "b", "c").expectHeight(0);
```

### selectAs

```java
DataFrame selectAs(UnaryOperator<String> renamer);
DataFrame selectAs(String... newColumnNames);
DataFrame selectAs(Map<String, String> oldToNewNames);
```

在 `select` 的基础上添加了重命名操作。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows()
        .selectAs("x", "y", "z");

new DataFrameAsserts(df, "x", "y", "z")
        .expectHeight(3)
        .expectRow(0, 1, "x", "a")
        .expectRow(1, 2, "y", "b")
        .expectRow(2, -1, "m", "n");
```


### expand

```java
DataFrame expand(String columnName);
DataFrame expand(int columnPos);
```

`expand` 用于拆分 row。即将数组类型或 `Iterable` 类型的 col 展开并**添加为新的 rows**。其它 col 填充与原 row 相同的值。

- 全部展开

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, List.of("x1", "x2"), "a",
                2, List.of("y1", "y2"), "b",
                4, List.of("e1", "e2"), "k",
                0, List.of("f1", "f2"), "g",
                1, List.of("m1", "m2"), "n",
                5, null, "x") // null 无法展开
        .rows()
        .expand("b"); // 将 col-b 展开

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(11)
        .expectRow(0, 1, "x1", "a")
        .expectRow(1, 1, "x2", "a")
        .expectRow(2, 2, "y1", "b")
        .expectRow(3, 2, "y2", "b")
        .expectRow(4, 4, "e1", "k")
        .expectRow(5, 4, "e2", "k")
        .expectRow(6, 0, "f1", "g")
        .expectRow(7, 0, "f2", "g")
        .expectRow(8, 1, "m1", "n")
        .expectRow(9, 1, "m2", "n")
        .expectRow(10, 5, null, "x");
```

- 指定要展开的 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, List.of("x1", "x2"), "a", // <--
                2, List.of("y1", "y2"), "b",
                4, List.of("e1", "e2"), "k",
                0, List.of("f1", "f2"), "g", // <--
                1, List.of("m1", "m2"), "n", // <--
                5, null, "x") // <--
        .rows(Series.ofInt(0, 3, 4, 5)).expand("b");

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(9)
        .expectRow(0, 1, "x1", "a")
        .expectRow(1, 1, "x2", "a")
        .expectRow(2, 2, List.of("y1", "y2"), "b")
        .expectRow(3, 4, List.of("e1", "e2"), "k")
        .expectRow(4, 0, "f1", "g")
        .expectRow(5, 0, "f2", "g")
        .expectRow(6, 1, "m1", "n")
        .expectRow(7, 1, "m2", "n")
        .expectRow(8, 5, null, "x");
```

- 选择特定范围的 rows 展开 

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, List.of("x1", "x2"), "a",
                2, List.of("y1", "y2"), "b",
                4, List.of("e1", "e2"), "k", // <--
                0, List.of("f1", "f2"), "g", // <--
                1, List.of("m1", "m2"), "n", // <--
                5, null, "x")
        .rowsRange(2, 5).expand("b");

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(9)
        .expectRow(0, 1, List.of("x1", "x2"), "a")
        .expectRow(1, 2, List.of("y1", "y2"), "b")
        .expectRow(2, 4, "e1", "k")
        .expectRow(3, 4, "e2", "k")
        .expectRow(4, 0, "f1", "g")
        .expectRow(5, 0, "f2", "g")
        .expectRow(6, 1, "m1", "n")
        .expectRow(7, 1, "m2", "n")
        .expectRow(8, 5, null, "x");
```

- 展开满足条件的 rows

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, List.of("x1", "x2"), "a", // <--
                2, List.of("y1", "y2"), "b",
                4, List.of("e1", "e2"), "k",
                0, List.of("f1", "f2"), "g", // <--
                1, List.of("m1", "m2"), "n", // <--
                5, null, "x") // <--
        .rows(Series.ofBool(true, false, false, true, true, true)).expand("b");

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(9)
        .expectRow(0, 1, "x1", "a")
        .expectRow(1, 1, "x2", "a")
        .expectRow(2, 2, List.of("y1", "y2"), "b")
        .expectRow(3, 4, List.of("e1", "e2"), "k")
        .expectRow(4, 0, "f1", "g")
        .expectRow(5, 0, "f2", "g")
        .expectRow(6, 1, "m1", "n")
        .expectRow(7, 1, "m2", "n")
        .expectRow(8, 5, null, "x");
```

- 随机抽取一些 rows 展开

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, List.of("x1", "x2"), "a",
                2, List.of("y1", "y2"), "b",
                4, List.of("e1", "e2"), "k",
                0, List.of("f1", "f2"), "g",
                1, List.of("m1", "m2"), "n",
                5, null, "x")
        // using fixed seed to get reproducible result
        .rowsSample(2, new Random(9)).expand("b");

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(8)
        .expectRow(0, 1, "x1", "a")
        .expectRow(1, 1, "x2", "a")
        .expectRow(2, 2, List.of("y1", "y2"), "b")
        .expectRow(3, 4, "e1", "k")
        .expectRow(4, 4, "e2", "k")
        .expectRow(5, 0, List.of("f1", "f2"), "g")
        .expectRow(6, 1, List.of("m1", "m2"), "n")
        .expectRow(7, 5, null, "x");
}
```

### selectExpand

```java
DataFrame selectExpand(String columnName);
DataFrame selectExpand(int columnPos);
```

将指定 `Iterable` 或数组类型的 col 展开生成新的 rows。

与 `expand` 功能一致，只是 `expand` 展开指定 `RowSet` 后合并到原 `DataFrame`，而 `selectExpand` 展开 `RowSet` 后只包含 `RowSet` 的内容。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, List.of("x1", "x2"), "a", // <--
                2, List.of("y1", "y2"), "b",
                4, List.of("e1", "e2"), "k",
                0, List.of("f1", "f2"), "g", // <--
                1, List.of("m1", "m2"), "n", // <--
                5, null, "x") // <--
        .rows(Series.ofInt(0, 3, 4, 5)).selectExpand("b");

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(7)
        .expectRow(0, 1, "x1", "a")
        .expectRow(1, 1, "x2", "a")
        .expectRow(2, 0, "f1", "g")
        .expectRow(3, 0, "f2", "g")
        .expectRow(4, 1, "m1", "n")
        .expectRow(5, 1, "m2", "n")
        .expectRow(6, 5, null, "x");
```


### sort

```java
DataFrame sort(Sorter... sorters);
DataFrame sort(int sortCol, boolean ascending);
DataFrame sort(String sortCol, boolean ascending);
DataFrame sort(int[] sortCols, boolean[] ascending);
DataFrame sort(String[] sortCols, boolean[] ascending);
```

对 `RowSet` 定义的部分 rows 进行排序，返回这部分 rows 排序后的 `DataFrame`。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a", // <--
                2, "y", "b",
                4, "e", "k", // <--
                0, "f", "g") // <--
        .rows(Series.ofInt(0, 2, 3))
        .sort(0, true);

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(4)
        .expectRow(0, 0, "f", "g")
        .expectRow(1, 2, "y", "b")
        .expectRow(2, 1, "x", "a")
        .expectRow(3, 4, "e", "k");
```

### unique

```java
DataFrame unique();
DataFrame unique(String... uniqueKeyColumns);
DataFrame unique(int... uniqueKeyColumns);
```

和 `selectUnique` 类似，不过该方法对 `RowSet` 去重后，将结果合并回原 `DataFrame`。

根据所选 cols 的值计算 hashCode，以 hashCode 判断是否重复。

示例：

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a", // <--
                1, "x", "b",
                1, "e", "k", // <--
                1, "x", "g", // <-- 去重
                1, "m", "n") // <--
        .rows(Series.ofInt(0, 2, 3, 4))
        .unique("a", "b"); // 使用 col-a 和 col-b 计算 hash 去重

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(4)
        .expectRow(0, 1, "x", "a")
        .expectRow(1, 1, "x", "b") // 不在 RowSet 中
        .expectRow(2, 1, "e", "k")
        .expectRow(3, 1, "m", "n");
```

### selectUnique

```java
DataFrame selectUnique();
DataFrame selectUnique(String... uniqueKeyColumns);
DataFrame selectUnique(int... uniqueKeyColumns);
```

对 `RowSet` 中指定 col 执行去重操作，保留 `RowSet` 去重后的结果。

- 使用所有 cols 的值来去重

```java
DataFrame df = DataFrame.foldByRow("first", "last").of(
        "Jerry", "Cosin",
        "Jerry", "Jones",
        "Jerry", "Cosin",
        "Joan", "O'Hara");

DataFrame df1 = df.rows().selectUnique(); // 选择完全 unique 的 rows
```

```
first last
----- ------
Jerry Cosin
Jerry Jones
Joan  O'Hara
```

- 使用一个 col 的值去重

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a", // <--
                2, "y", "b",
                1, "e", "k",
                1, "f", "g", // <--
                1, "m", "n") // <--
        .rows(0, 3, 4).selectUnique("a"); // RowSet 的 col-a 去重，只保留第一个

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(1)
        .expectRow(0, 1, "x", "a");
```

## RowColumnSet

`RowColumnSet` 是在 `RowSet` 的基础上进一步的子集对象。使用 `RowSet` 定义 `RowColumnSet`，即选择 `RowSet` 的特定 cols 子集：

```java
RowColumnSet cols();
RowColumnSet cols(Index columnsIndex);
RowColumnSet cols(String... columns);
RowColumnSet cols(int... columns);
RowColumnSet cols(Predicate<String> condition);
RowColumnSet colsExcept(String... columns);
RowColumnSet colsExcept(int... columns);
```

### drop

从 `DataFrame` 删除 `RowColumnSet` 定义的区域。

- 删除全部 rows，部分 cols (其中也删除完了)

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows().cols("b", "a")
        .drop();

new DataFrameAsserts(df, "c").expectHeight(0);
```

- 删除部分 rows，部分 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(0, 2).cols("b", "a")
        .drop();

new DataFrameAsserts(df, "c")
        .expectHeight(1)
        .expectRow(0, "b");
```

- 重复 rows 以及多余 cols 直接忽略

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b", // 
                -1, "m", "n")
        .rows(0, 2, 2).cols("b", "a", "x")
        .drop();

new DataFrameAsserts(df, "c")
        .expectHeight(1)
        .expectRow(0, "b");
```

### map

```java
DataFrame map(Exp<?>... exps);
DataFrame map(RowMapper mapper);
DataFrame map(RowToValueMapper<?>... mappers);
```

对所选部分执行转换。例如：

选择 row-0 和 row-2，col-b 和 col-a，然后将 col-b 修改为 col-2 和 col-3 的串联，col-a 修改为 col-0乘以3.

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a", //
                2, "y", "b",
                -1, "m", "n") //
        .rows(0, 2).cols("b", "a")
        .map(
                Exp.concat(Exp.$str(1), Exp.$str(2)),
                Exp.$int(0).mul(3)
        );

new DataFrameAsserts(df, "a", "b", "c")
        .expectHeight(3)
        .expectRow(0, 3, "xa", "a")
        .expectRow(1, 2, "y", "b")
        .expectRow(2, -3, "mn", "n");
```

### select

```java
DataFrame select();
DataFrame select(Exp<?>... exps);
DataFrame select(RowMapper mapper);
DataFrame select(RowToValueMapper<?>... mappers);
```

`select()` 返回 `RowColumnSet` 所含内容.

- 部分 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows()
        .cols("b", "a")
        .select();

new DataFrameAsserts(df, "b", "a")
        .expectHeight(3)
        .expectRow(0, "x", 1)
        .expectRow(1, "y", 2)
        .expectRow(2, "m", -1);
```

- 部分 rows 和部分 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt(0, 2))
        .cols("b", "a")
        .select();

new DataFrameAsserts(df, "b", "a")
        .expectHeight(2)
        .expectRow(0, "x", 1)
        .expectRow(1, "m", -1);
```

- 重复 rows，额外 cols

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(0, 2, 2)
        .cols("b", "a", "x")
        .select();

new DataFrameAsserts(df, "b", "a", "x")
        .expectHeight(3)
        .expectRow(0, "x", 1, null)
        .expectRow(1, "m", -1, null)
        .expectRow(2, "m", -1, null);
```

### selectAs

```java
DataFrame selectAs(UnaryOperator<String> renamer);
DataFrame selectAs(String... newColumnNames);
DataFrame selectAs(Map<String, String> oldToNewNames);
```

在 `select` 的基础上添加了重命名。

```java
DataFrame df = DataFrame.foldByRow("a", "b", "c")
        .of(
                1, "x", "a",
                2, "y", "b",
                -1, "m", "n")
        .rows(Series.ofInt(0, 2))
        .cols("b", "a")
        .selectAs("X", "Y");

new DataFrameAsserts(df, "X", "Y")
        .expectHeight(2)
        .expectRow(0, "x", 1)
        .expectRow(1, "m", -1);
```



## Printers

在分析数据时，数据可视化非常重要，将数据打印到控制台是最简单的可视化方法。`DataFrame` 和 `Series` 都实现了 `toString()` 方法，将它们的内容打印成一行，截断大型数据集，适合于调试。

以表格形式打印可读性更好。下面使用默认表格 printer 打印：

```java
DataFrame df = DataFrame
        .foldByColumn("col1", "col2", "col3")
        .ofStream(IntStream.range(0, 10000));

String table = Printers.tabular.toString(df);
System.out.println(table);
```

该方式最多显示 6 行，截断余下 rows。对单元格也是如此，长度超过 30 个字符的值也会被截断。

```
col1 col2 col3
---- ---- ----
   0 3334 6668
   1 3335 6669
   2 3336 6670
...
3331 6665 9999
3332 6666    0
3333 6667    0
3334 rows x 3 columns
```

可以调整截断参数：

```java
Printer printer = new TabularPrinter(3, 3); // 最多 3 行，每个 cell 最多 3 字符
String table = printer.toString(df);
System.out.println(table);
```

```
c.. c.. c..
--- --- ---
  0 3.. 6..
  1 3.. 6..
...
3.. 6..   0
3334 rows x 3 columns
```

> [!NOTE]
>
> 在 Jupyter Notebook 中，所有的 printer 都配置好。因此，如果 Jupyter cell 最后一行是 `DataFrame` 或 `Series`，它将打印一个 table。

## Expression

DFLib 内置了一个表达式语言（实现为 Java DSL），可用来在 `DataFrame` 和 `Series` 上执行 column-centric 操作，如数据转换、聚合和过滤。

> [!NOTE]
>
> 所有 exp 都返回 `Series` 类型。

`Exp` 是 exp 接口，exp 以 `DataFrame` 或 `Series` 为参数，生成指定类型的 `Series`。

- 非聚合 exp 生成与原数据结构大小相同的 `Series`；
- 聚合 exp 生成更少元素的 `Series` (通常只有一个元素)。

`Exp` 接口包含创建各种类型表达式的 factory 方法。

按照惯例，**应用于 col 的表达式**以 `$` 开头。以下是 `Exp` 接口的核心内容：

```java
public interface Exp<T> {
    /**
     * 返回结果类型。DBLib 表达式引擎使用该类型优化计算。
     *
     * Java 泛型的限制使得 DFLib 有时无法明确类型，导致表达式返回 Object。
     * 已知类型能够优化计算过程。
     */
    Class<T> getType();

    /**
     * 以 DataFrame 为参数计算表达式的值，返回一个 Series
     */
    Series<T> eval(DataFrame df);

    /**
     * 以 Series 为参数计算表达式的值，返回一个 Series
     */
    Series<T> eval(Series<?> s);
}
```

### static 方法

下表对 `Exp` 提供的方法做一个简单总结。

| Exp                                                          | 功能                                            |
| ------------------------------------------------------------ | ----------------------------------------------- |
| `Exp<T> $val(V value, Class<T> type)`                        | 返回包含重复值的 `Series`                       |
| `<T> Exp<T> $val(T value)`                                   | 同上，只是使用泛型推断类型                      |
| `DateExp $dateVal(LocalDate value)`                          | 同 `$val`，`LocalDate` 类型                     |
| `TimeExp $timeVal(LocalTime value)`                          | 同 `$val`，`LocalTime` 类型                     |
| `DateTimeExp $dateTimeVal(LocalDateTime value)`              | 同 `$val`，`LocalDateTime` 类型                 |
| `Exp<T> $col(String name)`                                   | 返回名为 `name` 的 col                          |
| `Exp<T> $col(int position)`                                  | 同上，用 position 选择 col                      |
| `Exp<T> $col(int position, Class<T> type)`                   | 以 index 选择 col，同时指定 col 类型            |
| `StrExp $str(String name)`                                   | 以 name 选择 col，col 类型指定为 str            |
| `StrExp $str(int position)`                                  | 以 index 选择 col，col 类型指定为 str           |
| `NumExp<Integer> $int(String name)`                          | 以 name 选择 col，col 类型指定为 int            |
| `NumExp<Integer> $int(int position)`                         | 以 index 选择 col，col 类型指定为 int           |
| `NumExp<Long> $long(String name)`                            | 以 name 选择 col，col 类型指定为 long           |
| `NumExp<Long> $long(int position)`                           | 以 index 选择 col，col 类型指定为 long          |
| `NumExp<Double> $double(String name)`                        | 以 name 选择 col，col 类型指定为 double         |
| `NumExp<Double> $double(int position)`                       | 以 index 选择 col，col 类型指定为 double        |
| `DecimalExp $decimal(String name)`                           | 以 name 选择 col，col 类型指定为 BigDecimal     |
| `DecimalExp $decimal(int position)`                          | 以 index 选择 col，col 类型指定为 BigDecimal    |
| `Condition $bool(String name)`                               | 以 name 选择 col，col 类型指定为 bool           |
| `Condition $bool(int position)`                              | 以 index 选择 col，col 类型指定为 bool          |
| `DateExp $date(String name)`                                 | 以 name 选择 col，col 类型指定为 LocalDate      |
| `DateExp $date(int position)`                                | 以 index 选择 col，col 类型指定为 LocalDate     |
| `TimeExp $time(String name)`                                 | 以 name 选择 col，col 类型指定为 LocalTime      |
| `TimeExp $time(int position)`                                | 以 index 选择 col，col 类型指定为 LocalTime     |
| `DateTimeExp $dateTime(String name)`                         | 以 name 选择 col，col 类型指定为 LocalDateTime  |
| `DateTimeExp $dateTime(int position)`                        | 以 index 选择 col，col 类型指定为 LocalDateTime |
| `Condition or(Condition... conditions)`                      |                                                 |
| `Condition and(Condition... conditions)`                     |                                                 |
| `Condition not(Condition condition)`                         |                                                 |
| `Exp<T> ifExp(Condition condition, Exp<T> ifTrue, Exp<T> ifFalse)` |                                                 |
| `Exp<T> ifNull(Exp<T> exp, Exp<T> ifNullExp)`                | 计算 exp，`null` 值通过调用 `ifNullExp` 替换    |
| `Exp<T> ifNull(Exp<T> exp, T ifNull)`                        | 计算 exp，`null` 值用 `ifNull` 替换             |
|                                                              |                                                 |

### exp 基础

首先静态导入 `Exp` 接口以使用其工厂方法：

```java
import static org.dflib.Exp.*;
```

下面创建两个简单的 exps，分别以 label 和 name 选择所需类型的 col：

```java
StrExp lastExp = $str("last");
DecimalExp salaryExp = $decimal(2);
```

求这两个 exps 的值：

```java
DataFrame df = DataFrame.foldByRow("first", "last", "salary").of(
        "Jerry", "Cosin", new BigDecimal("120000"),
        "Juliana", "Walewski", new BigDecimal("80000"),
        "Joan", "O'Hara", new BigDecimal("95000"));

Series<String> last = lastExp.eval(df); // 取 col-last，并转换为 str 类型
Series<BigDecimal> salary = salaryExp.eval(df); // 取 col-2，转换为 BigDecimal 类型
```

同样的操作也可以使用其它 DFLib API 完成，但这个基本抽象可以描述各种操作。

exp 很少单独使用，它们通常作为参数传递给其它方法。

> [!NOTE]
>
> DFLib exp 处理 `Series` 而非单个值，因此其性能较好。Exp 是操作数据的**首选方式**，而不是直接 API 或 lambda。

### col-exp

col-exp 用于选择 col。上面示例中的 `$str(...)` 和 `$decimal(...)` exps 为查找 col 的 exp，返回 `DataFrame` 指定名称或指定位置(0-based)的 col。

> [!NOTE]
>
> 对 `Series` 使用 col-exp 直接返回该 `Series`，忽略隐含的 col-name 或 col-index。

`Exp` 接口中的 col-exp 工厂方法很容易找，它们都以 `$` 开始。

- 返回一个命名 column，对 `Series` 返回自身

```java
$col("col");
```

- 返回指定类型的命名 column

```java
// 数值类型
$decimal("col");
$double("col");
$int("col");
$long("col");

// date/time 类型
$date("col");
$dateTime("col");
$time("col");

$bool("col");
$str("col");
```

> [!WARNING]
>
> 为了避免开销，col-exp 不执行类型转换（`$bool` 除外）。因此，应该根据数据类型选择正确的方法，以避免 `ClassCastExceptions`，或者使用通用的 `$col(...)` exp。

如果确实需要转换类型，可以使用 `castAs` 显式方法：

```java
$str("salary").castAsDecimal();
```

`castAs` 会尽量转换为目标类型，当出现无法转换的情况会抛出异常。当默认转换无法实现时，可以通过 `Exp.mapValue(...)` 自定义转换。

### 常量-exp

`$val(..)` 生成具有相同重复值的 `Series`：

```java
Series<String> hi = $val("hi!").eval(df);
```

```
hi!
hi!
hi!
```

`$val(..)` 可用于为字符串连接创建分隔符：

```java
Series<String> fn = Exp.concat(
        $str("first"),
        $val(" "),  // 在 first 和 last 之间插入空格
        $str("last")).eval(df);
```

### String exp

### numeric exp

上面提到的`$int(..)`, `$long(..)`, `double(..)` 和 `$decimal(..)` 等 col-exp 都是数值型的（`NumExp` 子类），因此它们提供了算术、比较和数字聚合等操作。

算术示例：

```java
NumExp<?> exp = $int("c1")
        .add($long("c2")) // 两个 col 相加
        .div(2.);  // 除以一个常量

DataFrame df = DataFrame.foldByRow("c1", "c2").of(
                1, 2L,
                3, 4L)
        .cols("c3").select(exp);
```

> [!NOTE]
>
> 数学表达式按照方法调用顺序执行，不按照数学运算符的优先级。
>
> 将 `int` 和 `long` 相加，结果被隐式转换为 `long` 类型，除以 `double` 时，又转换为 `double`。

聚合操作示例：

```java
NumExp<?> exp = $int("c1")
        .add($long("c2"))
        .sum() // <1> aggregating here
        .div(2.);

DataFrame df = DataFrame.foldByRow("c1", "c2").of(
                1, 2L,
                3, 4L)
        .cols("c3").agg(exp);
```

### complex exp

通过 `Exp` 的方法可以将多个 exp 合并为复杂的 exp。例如：

```java
// Condition 时一个 `Exp<Boolean>` exp
Condition c = and(  // Exp.add() 的 static 导入
        $str("last").startsWith("A"), // `startsWith()` 生成一个 condition
        $decimal("salary").add($decimal("benefits")).gt(100000.)  
    					// add 执行加法操作，`gt(..)` 生成 condition
);
```

### condition

`Condition` 返回 `BooleanSeries` 的 `Exp`。



## window

window 操作与 group 类似，不过它通常保留原始 `DataFrame`，并根据指定 row 的 window 计算值添加额外 col。下面使用如下 `DataFrame` 来显示 window 操作：

```
name             amount date
---------------- ------ ----------
Jerry Cosin        8000 2024-01-15
Juliana Walewski   8500 2024-01-15
Joan O'Hara        9300 2024-01-15
Jerry Cosin        8800 2024-02-15
Juliana Walewski   8500 2024-02-15
Joan O'Hara        9500 2024-02-15
```

调用 `DataFrame.over()` 创建 window:

```java
Window window = df.over();
```

为每个员工添加 max_salary col：

```java
DataFrame df1 = df
        .over()
        .partitioned("name") 
        .cols("max_salary").merge($int("amount").max()); 
```

```
name             amount date       max_salary
---------------- ------ ---------- ----------
Jerry Cosin        8000 2024-01-15       8800
Juliana Walewski   8500 2024-01-15       8500
Joan O'Hara        9300 2024-01-15       9500
Jerry Cosin        8800 2024-02-15       8800
Juliana Walewski   8500 2024-02-15       8500
Joan O'Hara        9500 2024-02-15       9500
```

上例中 `partitioned` 类似 `GroupBy`。另外也可以使用 `range(..)` 定义 window，该方法可以定义相对每个 row 的窗口大小。

### shift

```java
public <T> Series<T> shift(String column, int offset)
public <T> Series<T> shift(String column, int offset, T filler)
public <T> Series<T> shift(int column, int offset)
public <T> Series<T> shift(int column, int offset, T filler)
```

### rowNumber



### select

```java
public DataFrame select(Exp<?>... aggregators)
```

生成与原 `DataFrame` 等高的 `DataFrame`，包含所提供聚合表达式参数生成的 cols。对每个 row 调用一次 aggregators，并传入与 partition, sorting 和 ranges 设置对应的 range。

- 应用于整个 range 的聚合表达式

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        0, "a",
        1, "x");

// 这里聚合表达式计算 col-a 的加和，对每个 row 都是如此
// 所以生成的 sum(a) 每个值都是 col-a 的加和
DataFrame r = df.over().select($int("a").sum());
new DataFrameAsserts(r, "sum(a)")
        .expectHeight(5)
        .expectRow(0, 5)
        .expectRow(1, 5)
        .expectRow(2, 5)
        .expectRow(3, 5)
        .expectRow(4, 5);
```

- 计算多个聚合值

```java
DataFrame df = DataFrame.foldByRow("a", "b").of(
        1, "x",
        2, "y",
        1, "z",
        0, "a",
        1, "x");

DataFrame r = df.over()
        .cols("a", "b")
        .select(
                $int("a").sum(),
                $col("b").first()
        );

new DataFrameAsserts(r, "a", "b")
        .expectHeight(5)
        .expectRow(0, 5, "x")
        .expectRow(1, 5, "x")
        .expectRow(2, 5, "x")
        .expectRow(3, 5, "x")
        .expectRow(4, 5, "x");
```



### partitioned

```java
public Window partitioned(Hasher partitioner)
public Window partitioned(int... columns)
public Window partitioned(String... columns)
```

`partitioned()` 用于指定计算 hash 值的 cols。

例如：

```jade
@Test
public void partitioned() {
    DataFrame df = DataFrame.foldByRow("a", "b").of(
            1, "x",
            2, "y",
            1, "z",
            0, "a",
            2, "y",
            1, "x");

    Series<String> sa = df.over().partitioned("a").shift("b", 1);
    new SeriesAsserts(sa).expectData(null, null, "x", null, "y", "z");

    Series<String> sb = df.over().partitioned("b").shift("b", -1);
    new SeriesAsserts(sb).expectData("x", "y", null, null, null, null);
}
```

## 工具类

### Sorter

用于排序的辅助类。

```java
public interface Sorter {

    IntComparator eval(DataFrame df);

    IntComparator eval(Series<?> s);
}
```


`Sorter` 用于 DFLib 数据结构排序。`Sorter` 内部使用 exp 检索包含排序条件的值，并按指定顺序进行索引。

使用 `asc()` 或 `desc()` 方法创建 `Sorter` 对象。

```java
// sort by last name in the ascending order
Sorter s = $str("last").asc();
```

> [!NOTE]
>
> 虽然 DFLib 支持以任何表达式类型创建 `Sorter`，在运行时，实际类型必须是 java primitive 后 `Comparable` 实例，否则抛出 `ClassCastException`。

## 自定义函数

可以使用用户自定义函数（UDF）创建可重用 exp。[exp](#expressions) 是 col 的抽象转换，在某种程度上可重用。但是它们硬编码了 cols 的名称或位置，因为相同的 exp 无法用于不同 cols。UDF 旨在解决该问题，使得 exp 更加动态。

UDF 是一种函数，通常定义为 lambda 表达式或方法引用。它接受一个或多个 exps 作为参数，并生成一个 exp：

```java
void formatNames() {
    DataFrame df = DataFrame.foldByRow("first", "last").of(
            "JERRY", "COSIN",
            "juliana", "waLEwsKi");

    Udf1<String, String> formatName = e ->
            e.castAsStr().mapVal(this::formatName); // 定义一个 UDF，接受单个参数，首字母大小

    DataFrame clean = df.cols("first", "last").merge(
            formatName.call("first"),
            formatName.call("last")); // 相同 UDF 分别应用于 "first" 和 "last" name cols
}

String formatName(String raw) {
    return Character.toUpperCase(raw.charAt(0))
            + raw.substring(1).toLowerCase();
}
```

```
first   last
------- --------
Jerry   Cosin
Juliana Walewski
```

上例多次调用 UDF，应用不同的 col-name。除了 col-name，也可以使用位置或完整 exp 来调用 UDF。

上面使用 `Udf1` 函数，它接受一个 exp 或 col 为输入。有 `Udf1`, `Udf2`, `Udf3` 和 `UdfN` 接口，用于不同参数个数，其中 `UdfN` 用于超过 3 个参数或可变参数：

```java
DataFrame df = DataFrame.foldByRow("first", "last").of(
        "Jerry", "Cosin",
        null, "Walewski",
        "Joan", null);

UdfN<Boolean> noNulls = exps -> and( // 创建包含动态参数个数的 UDF，参数以 Exp[] 形式输入
        Stream.of(exps)
                .map(Exp::isNotNull)
                .toArray(Condition[]::new)
);

DataFrame clean = df
        .rows(noNulls.call("first", "last").castAsBool()) // 调用 UDF 创建 row-filter
        .select();
```

上面 UDF 返回 `Exp<Boolean>`，需要显式转换为 `Condition` 以满足`RowSet` 的需要。

```
first last
----- -----
Jerry Cosin
```

> [!TIP]
>
> 如果 UDF 中需要执行 col 类型转换，而该 col 类型是已知的，则应该提前转换为所需类型，这样性能更好。例如，如果 UDF 中对其参数调用 `castAsInt()`，那么 `udf.call($int("a"))` 比 `udf.call("a")` 要快。

## 使用关系数据库

关系数据库是最重要的数据存储类型之一，而它恰好可以很好地映射到 dataframe。DFLib 支持加载和保存到 RDBMS。支持事务、自动生成部分 SQL 语句、合并数据等操作。

首先要加入 `dflib-jdbc` 模块：

```xml
<dependency>
    <groupId>org.dflib</groupId>
    <artifactId>dflib-jdbc</artifactId>
</dependency>

<!-- 导入对应的 JDBC driver -->
```

### JdbcConnector

首先使用 `Jdbc` 类获得 `JdbcConnector` 实例来执行所有 DB 操作。你可能已经配置好了 `javax.sql.DataSource`，因此，创建 connector 的最简单方法时将该 datasource 传递给 `Jdbc` 工厂方法：

```java
JdbcConnector connector = Jdbc.connector(dataSource);
```

也可以从头开始构建：

```java
JdbcConnector connector = Jdbc
        .connector("jdbc:derby:target/derby/mydb;create=true")
        // .driver("com.foo.MyDriver") // optional，驱动名称。有些 driver 没有正确注册到 
    								  // DriverManager，需要显式声明
        // .userName("root")
        // .password("secret") // DB 用户名和密码(对 in-memory DB Derby 不需要)
        .build();
```

### TableLoader / TableSaver

有了 connector 后，就可以开始读取数据。许多 dataframe 直接映射到 db 的单个表格或视图。DFLib 提供了相当直接的操作，不需要用户编写 SQL:

```java
DataFrame df = connector.tableLoader("person").load();
```

```
id name              salary
-- ----------------- --------
 1 Jerry Cosin       70000.0
 2 Juliana Walewski  85000.0
 3 Joan O'Hara       101000.0
```

`tableLoader` 提供了自定义加载的方法，允许选择特定 cols，设置读取的最大 row 数、抽样 row，甚至可以通过另一个 `DataFrame` 指定条件。例如：

```java
DataFrame condition = DataFrame
        .byColumn("salary")
        .of(Series.ofInt(70_000, 101_000));

DataFrame df = connector.tableLoader("person")
        .cols("name", "salary")
        .eq(condition)
        .load();
```

```
name        salary
----------- --------
Jerry Cosin 70000.0
Joan O'Hara 101000.0
```

这里不需要显式指定 col 类型（csv 文件需要），因为从数据库的 metadata 可以推断出来。

`tableSaver` 用于将 `DataFrame` 保存为 table。`DataFrame` 的 col-names 需要匹配 DB table 的 col-names:

```java
DataFrame df = DataFrame.byArrayRow("id", "name", "salary")
        .appender()
        .append(1, "Jerry Cosin", 70_000)
        .append(2, "Juliana Walewski", 85_000)
        .append(3, "Joan O'Hara", 101_000)
        .toDataFrame();

connector.tableSaver("person").save(df);
```

在这里，`tableSaver` 对 `DataFrame` 的每个 row 逐个执行 insert 操作。如果 table 中已经有数据，则可以选择覆盖或合并数据：

- append 数据
- 插入前删除已有数据
- 通过比较 `DataFrame` 和 DB table 数据来自行合并。插入缺失 rows，更新已有 rows。如果 table 的 cols 数比 `DataFrame` 多，则保留额外 cols 的数据

插入前删除：

```java
connector.tableSaver("person")
        .deleteTableData()
        .save(df);
```

通过 DB metadata 检测 PK cols，然后合并：

```java
connector.tableSaver("person")
        .mergeByPk()
        .save(df);
```

### SqlLoader / SqlSaver

## csv 文件

>  2024年9月27日 ⭐⭐
>
>  2024年9月29日 添加 row-filter

DFLib 支持读取 CSV，以及将 `DataFrame` 保存为 CSV。

需要添加额外的依赖项：

```xml
<dependency>
    <groupId>org.dflib</groupId>
    <artifactId>dflib-csv</artifactId>
</dependency>
```

`Csv` 类是所有 csv 相关操作的入口。通过 `Csv` 类创建 `CsvLoader` 或 `CsvSaver`，然后用来读取或输出 csv 文件。

### 读取 csv

读取 CSV 最简单的 API：

```java
DataFrame df = Csv.load("src/test/resources/f1.csv"); // 参数支持文件名，File, Reader
```

这里，DFLib 默认：

- `f1.csv` 文件第一行为 col-labels
- 所有 col 都是 `String` 类型
- 读取所有 rows 和 cols

这些假设不一定满足要求，因此 DFLib 提供了设置功能，包括配置 col 类型，跳过 rows 和 cols，甚至对整个 CSV 进行采样。`Csv.loader()` 返回 `CsvLoader` 实例

例如：

```java
DataFrame df = Csv.loader() 
        .offset(1) // 跳过 header row
        .header("x", "y") // 自定义 header
        .intColumn("x") // 将第一个 col 类型设置为 int
        .load("src/test/resources/f1.csv");
```

> [!NOTE]
>
> 虽然可以直接加载原始数据，然后使用标准的 `DataFrame` 转换来得到所需类型，但是通过 loader 加载可以优化速度和内存。

#### col 类型设置

建议提前设置 col 类型，这样性能更好。类型设置通用方法：

```java
CsvLoader col(int column, ValueMapper<String, ?> mapper);
CsvLoader col(String column, ValueMapper<String, ?> mapper);
```

`ValueMapper` 中已经预定义了许多转换方式。由于默认为 `String` 类型，所以里面定义的都是 `StringTo...` 方法。

示例 f1.csv 文件：

```
A,b,C
1,2,3
4,5,6
```

- 类型转换

```java
DataFrame df = new CsvLoader()
        .col(0, ValueMapper.stringToInt()) // col-0 转换为 Int
        .col(2, ValueMapper.stringToLong()) // col-2 转换 Long
        .load(inPath("f1.csv"));
new DataFrameAsserts(df, "A", "b", "C")
        .expectHeight(2)
        .expectRow(0, 1, "2", 3L)
        .expectRow(1, 4, "5", 6L);
```

- 若针对特定 col 定义了多次转换方式，仅最后一次生效

```java
DataFrame df = new CsvLoader()
        .col(0, ValueMapper.stringToInt())
        .col("A", ValueMapper.stringToLong()) // 这次生效
        .load(inPath("f1.csv"));
new DataFrameAsserts(df, "A", "b", "C")
        .expectHeight(2)
        .expectRow(0, 1L, "2", "3")
        .expectRow(1, 4L, "5", "6");
```

针对**数值类型**，有对应的便捷方式：

```java
CsvLoader numCol(int column, Class<? extends Number> type);
CsvLoader numCol(String column, Class<? extends Number> type);
```

- 数值类型转换

```java
DataFrame df = new CsvLoader()
        .numCol(0, Integer.class)
        .numCol("b", Long.class)
        .numCol("C", Double.class)
        .load(inPath("f1.csv"));

new DataFrameAsserts(df, "A", "b", "C")
        .expectHeight(2)
        .expectRow(0, 1, 2L, 3.)
        .expectRow(1, 4, 5L, 6.);
```

- 数值类型支持 BigDecimal

```java
DataFrame df = new CsvLoader()
        .numCol(0, Float.class)
        .numCol("b", BigDecimal.class)
        .numCol("C", BigInteger.class)
        .load(inPath("f1.csv"));

new DataFrameAsserts(df, "A", "b", "C")
        .expectHeight(2)
        .expectRow(0, 1.f, new BigDecimal(2), new BigInteger("3"))
        .expectRow(1, 4.f, new BigDecimal(5), new BigInteger("6"));
```

针对**基础类型**，有大量快捷方法，以 int 类型为例：

```java
CsvLoader intCol(int column);
CsvLoader intCol(int column, int forNull);
CsvLoader intCol(String column);
CsvLoader intCol(String column, int forNull);
```

对 `long`, `double`, `BigDecimal`, `boolean`, `LocalDate`, `LocalDateTime` 等有类似方法。

> [!TIP]
>
> 可以串联多个表达式，设置多个 col 的类型。

例如，下面是一个 csv 文件：

```csv
A,B
1,7
2,8
3,9
4,10
5,11
6,12
```

显然，两个 col 都是 int 类型，我们把 col-0 设置为 int 类型：

```java
DataFrame df = new CsvLoader()
        .intCol(0) // col-0 设置为 integer 类型
        .rows(RowPredicate.of(0, (Integer i) -> i % 2 == 0)) // 保留 col-0 为偶数的 rows
        .load(new StringReader(csv()));

new DataFrameAsserts(df, "A", "B")
        .expectHeight(3)
        .expectRow(0, 2, "8")
        .expectRow(1, 4, "10")
        .expectRow(2, 6, "12");
```

#### row filter

对非常大的 CSV 文件，一边读一边过滤很有必要。通过为 `CsvLoader` 设置 `RowPredicate` 实现：

```java
CsvLoader rows(RowPredicate rowCondition)
```

`RowPredicate` 是一个简单的接口，只有一个方法：

```java
boolean test(RowProxy r);
```

其中 `RowProxy` 表示单个 row。根据所选 row 的值是否满足条件来过滤 rows。`RowPredicate` 提供了便捷的工厂方法：

```java
static <V> RowPredicate of(int pos, Predicate<V> columnPredicate) {
    return r -> columnPredicate.test((V) r.get(pos));
}

static <V> RowPredicate of(String columnName, Predicate<V> columnPredicate) {
    return r -> columnPredicate.test((V) r.get(columnName));
}
```

通过判断指定 col 的值是否满足 `Predicate` 来生成过滤条件。

还可以通过 `and()`, `or()` 和 `negate()` 来组合多个 `RowPredicate` 实现更复杂的条件。

例如，定义如下 csv 数据：

```java
A,B
1,7
2,8
3,9
4,10
5,11
6,12
```

- 使用 col-index 指定用于过滤的 col

```java
DataFrame df = new CsvLoader()
        .intCol(0) // 将第一个 col 转换为 int 类型
        .rows(RowPredicate.of(0, (Integer i) -> i % 2 == 0)) // 要求 col-0 为偶数
        .load(new StringReader(csv()));

new DataFrameAsserts(df, "A", "B")
        .expectHeight(3)
        .expectRow(0, 2, "8")
        .expectRow(1, 4, "10")
        .expectRow(2, 6, "12");
```

- 使用 col-label 指定用于过滤的 col

```java
DataFrame df = new CsvLoader()
        .intCol(0)
        .rows(RowPredicate.of("A", (Integer i) -> i % 2 == 0))
        .load(new StringReader(csv()));

new DataFrameAsserts(df, "A", "B")
        .expectHeight(3)
        .expectRow(0, 2, "8")
        .expectRow(1, 4, "10")
        .expectRow(2, 6, "12");
```

- 通过 `rows` 指定多个 `RowPredicate`，仅最后一个生效

```java
DataFrame df = new CsvLoader()
        .intCol(0)
        .intCol(1)
        .rows(RowPredicate.of("B", (Integer i) -> i % 2 == 0))
        .rows(RowPredicate.of("B", (Integer i) -> i == 12))

        // 就这个有效
        .rows(RowPredicate.of("B", (Integer i) -> i > 10))
        .load(new StringReader(csv()));

new DataFrameAsserts(df, "A", "B")
        .expectHeight(2)
        .expectRow(0, 5, 11)
        .expectRow(1, 6, 12);
```

- 过滤后再随机抽样

```java
DataFrame df = new CsvLoader()
        .intCol(0) // 将 col-0 转换为 int
        .rows(RowPredicate.of("A", (Integer i) -> i > 1)) // 只要 col-0 大于 1 的 rows
        .rowsSample(2, new Random(9)) // 对保留下来的 rows 随机抽样
        .load(new StringReader(csv()));

new DataFrameAsserts(df, "A", "B")
        .expectHeight(2)
        .expectRow(0, 2, "8")
        .expectRow(1, 5, "11");
```

- 如果抽样数大于样本数，返回所有 rows

```java
DataFrame df = new CsvLoader()
        .intCol(0)
        .rows(RowPredicate.of("A", (Integer i) -> i % 2 == 0))
        .rowsSample(4, new Random(8))
        .load(new StringReader(csv()));

new DataFrameAsserts(df, "A", "B")
        .expectHeight(3)
        .expectRow(0, 2, "8")
        .expectRow(1, 4, "10")
        .expectRow(2, 6, "12");
```

- 不能使用删除的 col 作为过滤条件

```java
CsvLoader loader = new CsvLoader()
        .intCol("A")
        .intCol("B")
        // col-A 不在结果中，使用 col-A 过滤会报错
        .rows(RowPredicate.of("A", (Integer i) -> i % 2 == 0))
        .cols("B"); // 只保留 col-B

assertThrows(IllegalArgumentException.class, 
             	() -> loader.load(new StringReader(csv())));
```

之所以会报错，是因为 DBLib 会先提取所选的 cols，再进行过滤。在过滤时找不到 col-A，因此报错。

- 调整返回 cols 的顺序

```java
DataFrame df = new CsvLoader()
        .intCol("A")
        .intCol("B")
        .cols("B", "A")

        // 这里的 col-1 是 col-A，而不是原 csv 的 col-B
        .rows(RowPredicate.of(1, (Integer i) -> i == 4))
        .load(new StringReader(csv()));

new DataFrameAsserts(df, "B", "A")
        .expectHeight(1)
        .expectRow(0, 10, 4);
```

#### csv 格式

`CsvLoader` 内部使用 commons-csv 实现 csv 解析。csv 格式也通过 commons-csv 的 `CSVFormat` 类来设置：

```java
CsvLoader format(CSVFormat format)
```

`CSVFormat` 的配置方法可以参考 [commons-csv](./commons_csv.md)。

#### header

```java
CsvLoader header(String... columns);
```

显式设置 csv 文件的标题：

- 如果不设置，将 csv 文件第一行作为 header
- 设置后，将 csv 文件第一行作为数据

标题按照从左到右的位置依次分配。

标题 `columns` 的长度必须 <= csv 文件中 col 数。

下面是 f1.csv 文件内容：

```
A,b,C
1,2,3
4,5,6
```

设置标题，第一行 "A,b,C" 也作为数据：

```java
DataFrame df = new CsvLoader().header("X", "Y", "Z").load(inPath("f1.csv"));
new DataFrameAsserts(df, "X", "Y", "Z")
        .expectHeight(3)
        .expectRow(0, "A", "b", "C")
        .expectRow(1, "1", "2", "3")
        .expectRow(2, "4", "5", "6");
```

#### generateHeader

```java
CsvLoader generateHeader();
```

生成 "c0", "c1", "c2" 格式的标题，csv 文件第一行作为数据处理。依然以 f1.csv 文件为例：

```java
DataFrame df = new CsvLoader().generateHeader().load(inPath("f1.csv"));
new DataFrameAsserts(df, "c0", "c1", "c2")
        .expectHeight(3)
        .expectRow(0, "A", "b", "C")
        .expectRow(1, "1", "2", "3")
        .expectRow(2, "4", "5", "6");
```

#### cols

```java
public CsvLoader cols(String... columns);
public CsvLoader cols(int... columns);

CsvLoader colsExcept(int... columns);
CsvLoader colsExcept(String... columns);
```

只处理指定的 cols，且返回的 cols 顺序与指定顺序一致。

以 f1.csv 文件为例：

```
A,b,C
1,2,3
4,5,6
```

- 只保留 col-b 和 col-A

```java
DataFrame df = new CsvLoader().cols("b", "A").load(inPath("f1.csv"));
new DataFrameAsserts(df, "b", "A")
        .expectHeight(2)
        .expectRow(0, "2", "1")
        .expectRow(1, "5", "4");
```

- 也可以先设置 header，然后使用新的 header 筛选 cols

```java
DataFrame df = new CsvLoader()
    	.header("X", "Y", "Z")
    	.cols("Y", "X")
    	.load(inPath("f1.csv"));
new DataFrameAsserts(df, "Y", "X")
        .expectHeight(3)
        .expectRow(0, "b", "A")
        .expectRow(1, "2", "1")
        .expectRow(2, "5", "4");
```

- 或者生成 "c0" 样式的 header，然后用该 header 筛选 cols

```java
DataFrame df = new CsvLoader()
    	.generateHeader()
    	.cols("c1", "c0")
    	.load(inPath("f1.csv"));
new DataFrameAsserts(df, "c1", "c0")
        .expectHeight(3)
        .expectRow(0, "b", "A")
        .expectRow(1, "2", "1")
        .expectRow(2, "5", "4");
```

#### limit

```java
CsvLoader limit(int len);
```

限制读取的最大 row 数。

f1.csv 文件：

```
A,b,C
1,2,3
4,5,6
```

- 只读一行

```java
DataFrame df = new CsvLoader().limit(1).load(inPath("f1.csv"));
new DataFrameAsserts(df, "A", "b", "C")
        .expectHeight(1)
        .expectRow(0, "1", "2", "3");
```

- 设置标题后，第一行也是数据

```java
DataFrame df = new CsvLoader()
        .header("C0", "C1", "C2")
        .limit(2)
        .load(inPath("f1.csv"));

new DataFrameAsserts(df, "C0", "C1", "C2")
        .expectHeight(2)
        .expectRow(0, "A", "b", "C")
        .expectRow(1, "1", "2", "3");
```

#### offset

```java
CsvLoader offset(int len);
```

`offset` 和 `limit` 相反：

- `limit` 限制只读前 `len` 行
- `offset` 限制跳过前 `len` 行

例如：

- 跳过第一行

```java
DataFrame df = new CsvLoader()
    	.offset(1)
    	.load(inPath("f1.csv"));
new DataFrameAsserts(df, "1", "2", "3")
        .expectHeight(1)
        .expectRow(0, "4", "5", "6");
```

- 跳过第 1 行，且限制只读 1 行

```java
DataFrame df = new CsvLoader()
        .generateHeader()
        .offset(1)
        .limit(1)
        .load(inPath("f1.csv"));

new DataFrameAsserts(df, "c0", "c1", "c2")
        .expectHeight(1)
        .expectRow(0, "1", "2", "3");
```





### 写入 CSV

写入 CSV 同样简单：

```java
Csv.save(df, "target/df.csv"); // 参数为 filename, File, Writer 或 Appendable
```

和 loader 一样，也可以设置：

```java
Csv.saver() // 使用 saver，而不是 save
        .createMissingDirs() // 如果输出位置目录缺失，则创建
        .format(CSVFormat.EXCEL) 
        .save(df, "target/csv/df.csv"); // 输出 CSV 格式
```

## Excel 文件

添加如下依赖以支持 excel 文件：

```xml
<dependency>
    <groupId>org.dflib</groupId>
    <artifactId>dflib-excel</artifactId>
</dependency>
```

## Avro 文件

Avro binary 格式在数据工程中很常见，其主要优点是 `.avro` 文件嵌入了 schema。因此在回读时，很容易将数据转换为对应 java 类型。这与 CSV 不同，读取 `.csv` 文件得到的 `DataFrame` 默认都是 str 类型。

添加如下依赖以支持 avro 格式：

```xml
<dependency>
    <groupId>org.dflib</groupId>
    <artifactId>dflib-avro</artifactId>
</dependency>
```

`Avro` 类是所有操作的入口。通过该类可以将 `DataFrame` 保存为 `.avro` 文件，以及加载 `.avro` 文件。

### Avro schema

大多情况不需要了解 avro schema。DBLib 在保存 `DataFrame` 时会自动生成一个 schema。当然，也可以根据 [Avro](https://avro.apache.org/docs/) 规范创建一个自定义 `Schema` 对象，并传递给 `AvroSaver`。

## Parquet

Parquey 时一种流行的基于 col 的 binary 格式。和 avro 一样，parquet 也内嵌了 schema，在回读时字段被转换为合适的 Java 类型。

添加如下依赖以支持 parquet 格式：

```xml
<dependency>
    <groupId>org.dflib</groupId>
    <artifactId>dflib-parquet</artifactId>
</dependency>
```

之后，`Parquet` 类为所有操作的入口。

## JShell

现在 JDK 附带了 jshell，即一个 Read-Evaluate-Print-Loop (REPL) 工具。可以在 JShell 中使用 DFLib 用于交互式数据探索。

首先需要配置，启动 jshell 并执行以下命令：

```sh
// 设置 classpath.
// jshell 没有自动依赖管理
// 需要显式包含 DFLib jars 及其依赖项

/env --class-path /path/to/dflib-1.0.0-M22.jar:/path/to/dflib-csv-1.0.0-M22.jar
```

```
// 禁用数据截断

/set mode mine normal -command
/set truncation mine 40000
/set feedback mine
```

添加 import：

```java
import org.dflib.*;
import org.dflib.csv.*;

import static org.dflib.Exp.*;
```

设置 tablular printer

```java
Environment.setPrinter(Printers.tabular());
```

然后就可以使用 `DataFrame`，查看输出：

```java
var df = Csv.load("../data/stuff.csv");
```

jshell 输出：

```
df ==>
id   name              salary
---- ----------------- --------
   1 Jerry Cosin       70000.0
   2 Juliana Walewski  85000.0
   3 Joan O'Hara       101000.0
...
 997 Jenny Harris      65000.0
 998 Matt Ostin        52000.0
 999 Andrew Tsui       99000.0
1000 rows x 3 columns
```

## Jupyter

jupyter 通过 web 与代码交互，可以逐步运行代码，检查每一步的运行结果。Jupyter 在 Python 中使用广泛，但也可以与 Java 一起使用。

## ECharts

DFLib 支持通过 Apache ECharts 从 `DataFrame` 生成 charts。charts 以 HTML/JavaScript 代码的形式生成，既可以在 Jupyter 中展示，也可以通过 web 显示。

这里讨论的重点是 DFLib API，对ECharts 的相关概念需要参考 ECharts 的文档。DFLib 方法主要差别在于，不需要显式指定数据集。

> [!NOTE]
>
> ECharts JavaScript API 包含大量 chart 类型和配置选项。DBLib 的 `EChart` 类只支持其中一部分功能。

为支持 ECharts，先添加依赖项：

```xml
<dependency>
    <groupId>org.dflib</groupId>
    <artifactId>dflib-echarts</artifactId>
</dependency>
```

然后就可以开始创建 charts。例如，创建 bar-chart：

```java
DataFrame df = DataFrame.foldByRow("name", "salary").of(
                "J. Cosin", 120000,
                "J. Walewski", 80000,
                "J. O'Hara", 95000)
        .sort($col("salary").desc());

EChartHtml chart = ECharts
        .chart()
        .xAxis("name")
        .series(SeriesOpts.ofBar(), "salary")
        .plot(df);
```

`EChartHtml` 对象包含在浏览器渲染 chart 所需的 HTML 和 JavaScript 代码：

<img src="./images/image-20240929125523504.png" alt="image-20240929125523504" style="zoom: 50%;" />

在 Jupyter 中运行该代码将在浏览器中呈现上面的 chart。如果在 web 应用中嵌入 charts，还需要检查 `EChartHtml.getContainer()`, `EChartHtml.getExternalScript()`, `EChartHtml.getScript()` 方法，并在合适的时候将它们插入 HTML。

## 参考

- https://github.com/dflib/dflib
