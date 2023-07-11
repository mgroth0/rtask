package matt.rtask.shelltable

import matt.lang.require.requireEquals
import matt.model.code.idea.TabularDataIdea
import matt.prim.str.filterNotBlank

class ShellOutputTable(raw: String) : TabularDataIdea, List<Map<String, String>> {

    private val lines = raw.trim().lines().map { it.trim() }

    private val headerLine = lines.first()
    private val dataLines = lines.drop(1)

    private val fieldNames by lazy {
        headerLine.split("\t").filterNotBlank()
    }

    override val size get() = dataLines.size
    override fun isEmpty() = dataLines.isEmpty()
    override fun get(index: Int): Map<String, String> {
        val line = dataLines[index]
        val lineData = line.split("\t").filterNotBlank()
        requireEquals(lineData.size, fieldNames.size)
        return fieldNames.zip(lineData).toMap()
    }

    override fun contains(element: Map<String, String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<Map<String, String>>): Boolean {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: Map<String, String>): Int {
        TODO("Not yet implemented")
    }


    override fun lastIndexOf(element: Map<String, String>): Int {
        TODO("Not yet implemented")
    }


    override fun iterator(): Iterator<Map<String, String>> {
        return sequence {
            dataLines.indices.forEach {
                yield(get(it))
            }
        }.iterator()
    }


    override fun listIterator(): ListIterator<Map<String, String>> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<Map<String, String>> {
        TODO("Not yet implemented")
    }

    override fun subList(
        fromIndex: Int,
        toIndex: Int
    ): List<Map<String, String>> {
        TODO("Not yet implemented")
    }
}