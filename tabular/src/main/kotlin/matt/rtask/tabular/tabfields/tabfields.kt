package matt.rtask.tabular.tabfields

import matt.lang.delegation.provider
import matt.lang.delegation.valProp



interface TabularClass2

abstract class TabularClass<T : Any> {
    private val mFields = mutableMapOf<String, (T) -> Any?>()
    val fields: Map<String, (T) -> Any?> get() = mFields

    protected fun <V> field(op: (T) -> V) = provider {
        mFields[it] = op
        val f = TabularField<V>(it)
        valProp {
            f
        }
    }
}

class TabularField<V>(val name: String)


