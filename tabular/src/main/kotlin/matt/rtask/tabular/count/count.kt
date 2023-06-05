package matt.rtask.tabular.count

import matt.rtask.tabular.TabularData
import matt.rtask.tabular.tabfields.TabularField
import matt.rtask.tabular.typesafe.NamedTable
import kotlin.reflect.KProperty1

@PublishedApi
internal const val COUNT_LABEL = "count"

fun <R, D> TabularData<R, String, D>.counts(
    value: TabularField<out Any?>,
) = counts(
    value.name
)


fun <R, C, D> TabularData<R, C, D>.counts(
    value: C,
) = rows().counts(
    name = value.toString(),
    getter = { it.get(value) }
)


fun <T, V> Iterable<T>.counts(
    value: KProperty1<T, V>,
    name: String = value.name
) = counts(
    name = name,
    getter = value
)


inline fun <T, V> Iterable<T>.counts(
    name: String,
    getter: (T) -> V
): NamedTable<V, String, Int> {

    val v1 = map(getter)
    val data = mutableMapOf<V, MutableMap<String, Int>>()
    v1.toSet().forEach { i1 ->
        data.getOrPut(i1) { mutableMapOf() }[COUNT_LABEL] = 0
    }

    v1.forEach { d ->
        val row = data[d]!!
        val old = row[COUNT_LABEL]!!
        row[COUNT_LABEL] = old + 1
    }

    return NamedTable("$name counts", data)
}