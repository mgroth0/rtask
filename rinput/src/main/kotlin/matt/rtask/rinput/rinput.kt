package matt.rtask.rinput

import kotlinx.serialization.Serializable
import matt.briar.BriarExtraction
import matt.file.context.ComputeContext
import matt.json.YesIUseJson
import matt.json.toJsonString
import matt.model.data.message.SFile

@Serializable
sealed interface RInput {
    val computeContext: ComputeContext
    private val yes get() = YesIUseJson
    fun serializeAsRinput() = toJsonString<RInput>()
}

@Serializable
class QuickCheck(override val computeContext: ComputeContext) : RInput

@Serializable
class ExtractBriarMetadataInputs(override val computeContext: ComputeContext, val extraction: BriarExtraction) : RInput

@Serializable
sealed interface RInputWithOutput : RInput {
    val remoteOutputFolder: SFile
}

@Serializable
class PrepareBriarCrops(override val computeContext: ComputeContext, val extraction: BriarExtraction) : RInput

@Serializable
class SummarizeBriarMetadataInputs(
    override val computeContext: ComputeContext,
    override val remoteOutputFolder: SFile,
    val localOutputFolder: SFile,
    val extraction: BriarExtraction
) : RInputWithOutput

@Serializable
class CheckSBatchOutputInput(override val computeContext: ComputeContext) : RInput
