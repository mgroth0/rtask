package matt.rtask

import kotlinx.serialization.json.Json
import matt.lang.NUM_LOGICAL_CORES
import matt.prim.str.elementsToString
import matt.rtask.iarpa.checksbatch.checkSBatch
import matt.rtask.iarpa.gends.crop.prepareBriarCrops
import matt.rtask.iarpa.gends.generateDatasetJsons
import matt.rtask.iarpa.gends.summarize.summarizeBriarMetadata
import matt.rtask.profile.openMindProfile
import matt.rtask.quick.quickCheck
import matt.rtask.rinput.CheckSBatchOutputInput
import matt.rtask.rinput.ExtractBriarMetadataInputs
import matt.rtask.rinput.PrepareBriarCrops
import matt.rtask.rinput.QuickCheck
import matt.rtask.rinput.RInput
import matt.rtask.rinput.SummarizeBriarMetadataInputs
import matt.rtask.toc.mimicBashToc


fun main(args: Array<String>) {

    println(
        "numCPUs=$NUM_LOGICAL_CORES"
    )


    mimicBashToc("Started Java")
    println("Hello OpenMind 2!")


    val arg = args.singleOrNull()
        ?: run {
            println("got ${args.size} args")
            args.forEachIndexed { index, s ->
                println("ARG$index:$s")
            }
            error("there should be one and only one arg, but I got ${args.elementsToString()}")
        }
    val rArg = Json.decodeFromString<RInput>(arg)
    with(rArg.computeContext) {
        openMindProfile {
            when (rArg) {
                is QuickCheck                   -> quickCheck()
                is ExtractBriarMetadataInputs   -> generateDatasetJsons(rArg)
                is PrepareBriarCrops            -> prepareBriarCrops(rArg)
                is SummarizeBriarMetadataInputs -> summarizeBriarMetadata(rArg)
                is CheckSBatchOutputInput       -> checkSBatch(rArg)
            }
        }
    }

    mimicBashToc("Finished Java")
}
