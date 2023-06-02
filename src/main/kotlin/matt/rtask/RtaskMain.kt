package matt.rtask

import kotlinx.serialization.json.Json
import matt.lang.NUM_LOGICAL_CORES
import matt.rtask.iarpa.checksbatch.checkSBatch
import matt.rtask.iarpa.gends.generateDatasetJsons
import matt.rtask.quick.quickCheck
import matt.rtask.rinput.CheckSBatchOutputInput
import matt.rtask.rinput.ExtractBriarMetadataInputs
import matt.rtask.rinput.QuickCheck
import matt.rtask.rinput.RInput
import matt.rtask.toc.mimicBashToc


fun main(args: Array<String>) {

    println(
        "numCPUs=$NUM_LOGICAL_CORES"
    )


    mimicBashToc("Started Java")
    println("Hello OpenMind 2!")


    val arg = args.singleOrNull() ?: error("there should be one and only one arg")
    val rArg = Json.decodeFromString<RInput>(arg)
    when (rArg) {
        QuickCheck                 -> quickCheck()
        ExtractBriarMetadataInputs -> generateDatasetJsons()
        CheckSBatchOutputInput     -> checkSBatch()
    }

    mimicBashToc("Finished Java")
}
