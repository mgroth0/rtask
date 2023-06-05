package matt.rtask.tabular.vector

import matt.rtask.tabular.TabularData
import matt.rtask.tabular.coord.CellCoordinates
import matt.rtask.tabular.tabfields.TabularField

interface TabularVector<N, V, D> {
    val value: N
    fun get(v: V): D
    fun values(): Map<V, D>
}

interface TabularRow<R, C, D> : TabularVector<R, C, D>


fun <R, C, D> TabularData<R, C, D>.row(value: R) = TabularRowImpl(this, value)
fun <C, D> TabularData<String, C, D>.row(field: TabularField<*>) = TabularRowImpl(this, field.name)

class TabularRowImpl<R, C, D>(private val table: TabularData<R, C, D>, override val value: R) :
    TabularRow<R, C, D> {
    override fun get(v: C) = table[CellCoordinates(value, v)]
    override fun values(): Map<C, D> = table.columns().associate { it.value to it.get(value) }
}


interface TabularColumn<R, C, D> : TabularVector<C, R, D>


fun <R, C, D> TabularData<R, C, D>.column(value: C) = TabularColumnImpl(this, value)
fun <R, D> TabularData<R, String, D>.column(field: TabularField<*>) = TabularColumnImpl(this, field.name)

class TabularColumnImpl<R, C, D>(private val table: TabularData<R, C, D>, override val value: C) :
    TabularColumn<R, C, D> {
    override fun get(v: R) = table[CellCoordinates(v, value)]
    override fun values(): Map<R, D> = table.rows().associate { it.value to it.get(value) }
}

fun <N, V, D, D2> TabularVector<N, V, D>.proxy(op: (D) -> D2): TabularVector<N, V, D2> = ProxyVector(this, op)
class ProxyVector<N, V, D, D2>(private val source: TabularVector<N, V, D>, private val op: (D) -> D2) :
    TabularVector<N, V, D2> {
    override val value: N get() = source.value

    override fun get(v: V): D2 {
        return op(source.get(v))
    }

    override fun values(): Map<V, D2> {
        return source.values().mapValues { op(it.value) }
    }
}

