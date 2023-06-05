package matt.rtask.tabular

import matt.collect.map.sync.synchronized
import matt.collect.mapToSet
import matt.lang.model.value.Value
import matt.rtask.tabular.coord.CellCoordinates
import matt.rtask.tabular.vector.TabularColumn
import matt.rtask.tabular.vector.TabularColumnImpl
import matt.rtask.tabular.vector.TabularRow
import matt.rtask.tabular.vector.TabularRowImpl

interface TabularDataIdea

interface TabularData<R, C, D>: TabularDataIdea {
    operator fun get(coords: CellCoordinates<R, C>): D
    fun rows(): Set<TabularRow<R, C, D>>
    fun columns(): Set<TabularColumn<R, C, D>>
    val title: String?
}

interface MutableTabularDataInter<R, C, D> : TabularData<R, C, D> {
    operator fun set(coords: CellCoordinates<R, C>, value: D)
    fun getOrPut(coords: CellCoordinates<R, C>, defaultProducer: () -> D): D
    override var title: String?
}

abstract class TabularDataImpl<R, C, D> : TabularData<R, C, D> {


    abstract val cells: Map<CellCoordinates<R, C>, Value<D>>

    override fun get(coords: CellCoordinates<R, C>): D {
        return (cells[coords] ?: error("no value at $coords..")).value
    }

    protected fun currentRows() = cells.keys.map { it.rowId }.toSet().mapToSet { TabularRowImpl(this, it) }
    protected fun currentColumns() = cells.keys.map { it.colId }.toSet().mapToSet { TabularColumnImpl(this, it) }

}

abstract class MutableTabularDataImpl<R, C, D>(
    override var title: String? = null
) : TabularDataImpl<R, C, D>(), MutableTabularDataInter<R, C, D>

class MutableTabularData<R, C, D>(title: String? = null) : MutableTabularDataImpl<R, C, D>(title = title) {

    constructor(data: TabularData<R, C, D>) : this(data.title) {
        (data as TabularDataImpl).cells.forEach {
            this[it.key] = it.value.value
        }
    }

    internal val mCells = mutableMapOf<CellCoordinates<R, C>, Value<D>>()
    override val cells: Map<CellCoordinates<R, C>, Value<D>> = mCells

    override operator fun set(coords: CellCoordinates<R, C>, value: D) {
        mCells[coords] = Value(value)
    }


    override fun rows(): Set<TabularRow<R, C, D>> = currentRows()

    override fun columns(): Set<TabularColumn<R, C, D>> = currentColumns()

    override fun getOrPut(coords: CellCoordinates<R, C>, defaultProducer: () -> D): D {
        return get(coords) ?: defaultProducer().also { set(coords, it) }
    }

}

class SynchronizedTabularData<R, C, D>(title: String? = null) : MutableTabularDataImpl<R, C, D>(title = title) {
    private val mCells = mutableMapOf<CellCoordinates<R, C>, Value<D>>().synchronized()
    override val cells: Map<CellCoordinates<R, C>, Value<D>> = mCells

    @Synchronized
    override operator fun set(coords: CellCoordinates<R, C>, value: D) {
        mCells[coords] = Value(value)
    }

    @Synchronized
    override fun get(coords: CellCoordinates<R, C>): D {
        return super.get(coords)
    }


    @Synchronized
    override fun rows(): Set<TabularRow<R, C, D>> = currentRows()

    @Synchronized
    override fun columns(): Set<TabularColumn<R, C, D>> = currentColumns()

    @Synchronized
    override fun getOrPut(coords: CellCoordinates<R, C>, defaultProducer: () -> D): D {
        return get(coords) ?: defaultProducer().also { set(coords, it) }
    }
}

fun <R, C, D> MutableTabularDataImpl<R, C, D>.readOnly() = ImmutableTabularData(this)

fun <R, C, D> TabularData<R, C, D>.mutable() = MutableTabularData(this)


class ImmutableTabularData<R, C, D>(from: TabularDataImpl<R, C, D>) : TabularDataImpl<R, C, D>() {
    override val title = from.title
    override val cells = from.cells.toMap()

    private val cachedRows by lazy {
        currentRows()
    }

    override fun rows(): Set<TabularRow<R, C, D>> = cachedRows

    private val cachedColumns by lazy {
        currentColumns()
    }

    override fun columns(): Set<TabularColumn<R, C, D>> = cachedColumns

}

