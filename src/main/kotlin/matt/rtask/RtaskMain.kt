package matt.rtask

import kotlinx.serialization.json.Json
import matt.lang.RUNTIME
import matt.rtask.checksbatch.checkSBatch
import matt.rtask.gends.generateDatasetJsons
import matt.rtask.rinput.CheckSBatchOutputInput
import matt.rtask.rinput.ExtractBriarMetadataInputs
import matt.rtask.rinput.QuickCheck
import matt.rtask.rinput.RInput
import matt.rtask.toc.mimicBashToc


fun main(args: Array<String>) {

    println(
        "numCPUs=${RUNTIME.availableProcessors()}"
    )


    mimicBashToc("Started Java")
    println("Hello OpenMind 2!")


    val arg = args.singleOrNull() ?: error("there should be one and only one arg")
    val rArg = Json.decodeFromString<RInput>(arg)
    when (rArg) {
        QuickCheck                 -> println("all systems online!")
        ExtractBriarMetadataInputs -> generateDatasetJsons()
        CheckSBatchOutputInput     -> checkSBatch()
    }

    mimicBashToc("Finished Java")
}
