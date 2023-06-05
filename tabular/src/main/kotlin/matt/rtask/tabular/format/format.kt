package matt.rtask.tabular.format

import matt.lang.compare.comparableComparator
import matt.lang.compare.nullFirstComparableComparator
import matt.lang.go
import matt.model.code.idea.FigIdea
import matt.prim.str.mybuild.string
import matt.prim.str.times
import matt.prim.str.truncateOrCenterInSpaces
import matt.rtask.tabular.typesafe.NamedTable

object TextTableCounts : FigIdea

private const val DEFAULT_ROW_WIDTH = 25
private fun <D> defaultFormatter(): (D) -> String = { it.toString() }
val HIDE_ZEROS: (Int) -> String = { if (it == 0) "" else it.toString() }
private fun <T : Comparable<T>?> nullComparator(): Comparator<in T?> = nullFirstComparableComparator<T>()

@JvmName("formatForConsole1")
fun <R : Comparable<R>, C : Comparable<C>, D : Any> NamedTable<R, C, D>.formatForConsole(
    rowWidth: Int = DEFAULT_ROW_WIDTH,
    formatter: (D) -> String = defaultFormatter()
): String = formatForConsole(
    rowWidth = rowWidth,
    rowComparator = comparableComparator(),
    colComparator = comparableComparator(),
    formatter = formatter
)

@JvmName("formatForConsole2")
fun <R : Comparable<R>?, C : Comparable<C>, D : Any> NamedTable<R?, C, D>.formatForConsole(
    rowWidth: Int = DEFAULT_ROW_WIDTH,
    formatter: (D) -> String = defaultFormatter()
): String = formatForConsole(
    rowWidth = rowWidth,
    rowComparator = nullComparator(),
    colComparator = comparableComparator(),
    formatter = formatter
)

@JvmName("formatForConsole3")
fun <R : Comparable<R>, C : Comparable<C>?, D : Any> NamedTable<R, C?, D>.formatForConsole(
    rowWidth: Int = DEFAULT_ROW_WIDTH,
    formatter: (D) -> String = defaultFormatter()
): String = formatForConsole(
    rowWidth = rowWidth,
    rowComparator = comparableComparator(),
    colComparator = nullComparator(),
    formatter = formatter
)

@JvmName("formatForConsole4")
fun <R : Comparable<R>?, C : Comparable<C>?, D : Any> NamedTable<R?, C?, D>.formatForConsole(
    rowWidth: Int = DEFAULT_ROW_WIDTH,
    formatter: (D) -> String = defaultFormatter()
): String = formatForConsole(
    rowWidth = rowWidth,
    rowComparator = nullComparator(),
    colComparator = nullComparator(),
    formatter = formatter
)

@JvmName("formatForConsole5")
fun <R, C : Comparable<C>, D : Any> NamedTable<R, C, D>.formatForConsole(
    rowWidth: Int = DEFAULT_ROW_WIDTH,
    rowComparator: Comparator<R>,
    formatter: (D) -> String = defaultFormatter()
): String = formatForConsole(
    rowWidth = rowWidth,
    rowComparator = rowComparator,
    colComparator = comparableComparator(),
    formatter = formatter
)

@JvmName("formatForConsole6")
fun <R, C : Comparable<C>?, D : Any> NamedTable<R, C?, D>.formatForConsole(
    rowWidth: Int = DEFAULT_ROW_WIDTH,
    rowComparator: Comparator<R>,
    formatter: (D) -> String = defaultFormatter()
): String = formatForConsole(
    rowWidth = rowWidth,
    rowComparator = rowComparator,
    colComparator = nullComparator(),
    formatter = formatter
)

@JvmName("formatForConsole7")
fun <R : Comparable<R>, C, D : Any> NamedTable<R, C, D>.formatForConsole(
    rowWidth: Int = DEFAULT_ROW_WIDTH,
    colComparator: Comparator<C>,
    formatter: (D) -> String = defaultFormatter()
): String = formatForConsole(
    rowWidth = rowWidth,
    rowComparator = comparableComparator(),
    colComparator = colComparator,
    formatter = formatter
)

@JvmName("formatForConsole8")
fun <R : Comparable<R>?, C, D : Any> NamedTable<R?, C, D>.formatForConsole(
    rowWidth: Int = DEFAULT_ROW_WIDTH,
    colComparator: Comparator<C>,
    formatter: (D) -> String = defaultFormatter()
): String = formatForConsole(
    rowWidth = rowWidth,
    rowComparator = nullComparator(),
    colComparator = colComparator,
    formatter = formatter
)


@JvmName("formatForConsole9")
fun <R, C, D : Any> NamedTable<R, C, D>.formatForConsole(
    rowWidth: Int = DEFAULT_ROW_WIDTH,
    rowComparator: Comparator<in R>,
    colComparator: Comparator<in C>,
    formatter: (D) -> String = defaultFormatter()
) = string {
    fun Any?.cell() = toString().truncateOrCenterInSpaces(rowWidth)
    fun D?.cell() = (if (this == null) toString() else formatter(this)).truncateOrCenterInSpaces(rowWidth)
    lineDelimited {
        blankLine()
        name.go {
            +it
        }
        val cols = columns().sortedWith(compareBy(colComparator) { it.key })
        val headerRow = listOf(
            "",
        ) + cols.map { it.key }
        +headerRow.joinToString("|") { it.cell() }
        fun hLine() = List(headerRow.size) { "-" * rowWidth }.joinToString("|") { it }
        +hLine()
        val rws = rows().sortedWith(compareBy(rowComparator) { it.key })
        rws.forEach { r ->
            +(listOf(
                r.key.cell()
            ) + cols.map {
                it.value[r.key].cell()
            }).joinToString("|") { it }
        }
        blankLine()
    }
}