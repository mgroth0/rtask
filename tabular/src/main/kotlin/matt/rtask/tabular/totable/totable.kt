package matt.rtask.tabular.totable

import matt.rtask.tabular.typesafe.NamedTable


fun <T, R, C, D> Iterable<T>.toTable(
    name: String,
    rowHeaders: (T) -> R,
    op: T.() -> Map<C, D>
): NamedTable<R, C, D> {
    val data = mutableMapOf<R, Map<C, D>>()
    forEach {

        val rowData = it.op()

        data[rowHeaders(it)] = rowData
    }
    return NamedTable(name = name, data)
}