package matt.rtask.iarpa.checksbatch

import matt.prim.str.filterNotBlank
import matt.rtask.rinput.CheckSBatchOutputInput


fun checkSBatch(rArg: CheckSBatchOutputInput) {
    println("check sbatch placeholder")
    val outFiles = rArg.computeContext.files.sbatchOutputFolder.listFiles()!!.filter { it.mExtension.afterDot == "out" }
    println("outFiles count = ${outFiles.size}")
    println("outFilesToCheck count = ${outFiles.size}")
    val lastLines = outFiles.map {
        it to it.text.lines().filterNotBlank().last().trim()
    }
    println("pivot?")
    lastLines.groupBy { it.second }.forEach { (line, instances) ->
        println("\t${line}\t${instances.size}")
    }
}

