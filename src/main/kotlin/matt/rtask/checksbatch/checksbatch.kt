package matt.rtask.checksbatch

import matt.file.commons.rcommons.OpenMindFiles.SBATCH_OUTPUT_FOLDER
import matt.model.code.idea.TabularDataIdea
import matt.prim.str.filterNotBlank


fun checkSBatch() {
    println("check sbatch placeholder")
    SBATCH_OUTPUT_FOLDER
    /*    val rawSqueueOutput = ShellOutputTable(shell("/usr/bin/squeue", "-u", "mjgroth"))*/

    val outFiles = SBATCH_OUTPUT_FOLDER.listFiles()!!.filter { it.mExtension.afterDot == "out" }

    /*

        val currentRunningJobIds = rawSqueueOutput.filter {
            it["ST"] != "PD"
        }.onEach {
            require(it["ST"] == "R")
        }.map {
            it["JOBID"]
        }
    */


    println("outFiles count = ${outFiles.size}")
    /*    println("currentRunningJobs count = ${currentRunningJobIds.size}")

        val outFilesToCheck = outFiles.filter {
            it.nameWithoutExtension !in currentRunningJobIds
        }*/
    val outFilesToCheck = outFiles

    println("outFilesToCheck count = ${outFilesToCheck.size}")

    val lastLines = outFilesToCheck.map {
        it to it.text.lines().filterNotBlank().last().trim()
    }

    /*temp debug*/
    lastLines.forEach {
        if ("n=9" !in it.second) {
            println("problem? ${it.first}")
        }
    }

    println("pivot?")
    lastLines.groupBy { it.second }.forEach { (line, instances) ->
        println("\t${line}\t${instances.size}")
    }


}


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
        require(lineData.size == fieldNames.size)
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

    override fun subList(fromIndex: Int, toIndex: Int): List<Map<String, String>> {
        TODO("Not yet implemented")
    }
}