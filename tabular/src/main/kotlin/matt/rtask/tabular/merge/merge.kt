package matt.rtask.tabular.merge

import matt.lang.require.requireEquals
import matt.rtask.tabular.MutableTabularData
import matt.rtask.tabular.TabularDataImpl

fun <R, C, D> TabularDataImpl<R, C, D>.updatedWith(other: TabularDataImpl<R, C, D>): TabularDataImpl<R, C, D> {
    requireEquals(title, other.title)
    val new = MutableTabularData(this)
    new.mCells.putAll(other.cells)
    return new
}


fun <R, C, D> MutableTabularData<R, C, D>.add(other: TabularDataImpl<R, C, D>) {
    mCells.putAll(other.cells)
}

fun <R, C, D> MutableTabularData<R, C, D>.addAll(vararg others: TabularDataImpl<R, C, D>) {
    others.forEach {
        mCells.putAll(it.cells)
    }
}

fun <R, C, D> MutableTabularData<R, C, D>.addAll(others: Iterable<TabularDataImpl<R, C, D>>) {
    others.forEach {
        mCells.putAll(it.cells)
    }
}