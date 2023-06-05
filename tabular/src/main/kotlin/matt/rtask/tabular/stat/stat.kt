package matt.rtask.tabular.stat

import matt.math.reduce.mean
import matt.rtask.tabular.typesafe.NamedTable
import kotlin.reflect.KProperty1


fun <T> List<T>.stats(prop: KProperty1<T, Double?>) = map(prop).stats(prop.name)
fun <T, V> List<T>.stats(prop: KProperty1<T, V>, op: (V) -> Double?) = map(prop).map(op).stats(prop.name)
fun List<Double?>.stats(name: String): NamedTable<String, String, Number> {
    val data = mutableMapOf<String, MutableMap<String, Number>>()
    val nonNullNumbers = filterNotNull()
    data.getOrPut("count") { mutableMapOf() }["value"] = nonNullNumbers.size
    data.getOrPut("nullCount") { mutableMapOf() }["value"] = size - nonNullNumbers.size
    data.getOrPut("min") { mutableMapOf() }["value"] = nonNullNumbers.min()
    data.getOrPut("mean") { mutableMapOf() }["value"] = nonNullNumbers.mean()
    data.getOrPut("max") { mutableMapOf() }["value"] = nonNullNumbers.max()
    return NamedTable("Statistics of $name", data)
}
