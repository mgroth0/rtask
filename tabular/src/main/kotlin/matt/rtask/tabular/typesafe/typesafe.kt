package matt.rtask.tabular.typesafe

import matt.collect.mapToSet
import matt.rtask.tabular.TabularDataIdea
import kotlin.collections.Map.Entry


data class NamedTable<R, C, D>(
    val name: String,
    val data: Map<R, Map<C, D>>,
) : TabularDataIdea {
    fun rows() = data.entries
    fun columns(): Set<Entry<C, Map<R, D>>> {
        return rows().flatMap { it.value.keys }.toSet().mapToSet { colKey ->
            object : Entry<C, Map<R, D>> {
                override val key: C = colKey
                override val value: Map<R, D> = object : Map<R, D> {
                    override val entries: Set<Entry<R, D>>
                        get() = TODO("Not yet implemented")
                    override val keys: Set<R>
                        get() = TODO("Not yet implemented")
                    override val size: Int
                        get() = TODO("Not yet implemented")
                    override val values: Collection<D>
                        get() = TODO("Not yet implemented")

                    override fun isEmpty(): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun get(key: R): D? {
                        return data[key]?.get(colKey)
                    }

                    override fun containsValue(value: D): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun containsKey(key: R): Boolean {
                        TODO("Not yet implemented")
                    }
                }
            }
        }
    }
}






