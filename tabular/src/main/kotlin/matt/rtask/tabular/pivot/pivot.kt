package matt.rtask.tabular.pivot

import matt.rtask.tabular.TabularData
import matt.rtask.tabular.tabfields.TabularField
import matt.rtask.tabular.typesafe.NamedTable
import kotlin.reflect.KProperty1

fun <R, D> TabularData<R, String, D>.pivot(
    value1: TabularField<out Any?>,
    value2: TabularField<out Any?>
) = pivot(
    value1.name,
    value2.name
)

fun <R, C, D> TabularData<R, C, D>.pivot(
    value1: C,
    value2: C
) = rows().pivot(
    name1 = value1.toString(),
    getter1 = { it.get(value1) },
    name2 = value2.toString(),
    getter2 = { it.get(value2) },
)


fun <T, V1, V2> Iterable<T>.pivot(
    value1: KProperty1<T, V1>,
    value2: KProperty1<T, V2>,
) = pivot(
    name1 = value1.name,
    getter1 = value1,
    name2 = value2.name,
    getter2 = value2
)

fun <T, V1, V2> Iterable<T>.pivot(
    name1: String,
    getter1: (T) -> V1,
    name2: String,
    getter2: (T) -> V2
): NamedTable<V1, V2, Int> {
    val v1 = map(getter1)
    val v2 = map(getter2)
    val title = "$name1 by $name2"
    val pivotTable = mutableMapOf<V1, MutableMap<V2, Int>>()
    v1.toSet().forEach { i1 ->
        v2.toSet().forEach { i2 ->
            pivotTable.getOrPut(i1) { mutableMapOf() }[i2] = 0
        }
    }
    forEach {
        val mp = pivotTable[getter1(it)]!!
        val old = mp[getter2(it)]!!
        mp[getter2(it)] = old + 1
    }

    return NamedTable(title, pivotTable)
}