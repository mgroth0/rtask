package matt.rtask.iarpa.checksbatch

import matt.file.commons.rcommons.OpenMindFiles.SBATCH_OUTPUT_FOLDER
import matt.prim.str.filterNotBlank


fun checkSBatch() {
    println("check sbatch placeholder")
    SBATCH_OUTPUT_FOLDER
    val outFiles = SBATCH_OUTPUT_FOLDER.listFiles()!!.filter { it.mExtension.afterDot == "out" }
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

