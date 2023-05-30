package matt.rtask.rinput

import kotlinx.serialization.Serializable
import matt.json.YesIUseJson
import matt.json.toJsonString

@Serializable
sealed interface RInput {
    private val yes get() = YesIUseJson
    fun serializeAsRinput() = toJsonString<RInput>()
}

@Serializable
object QuickCheck : RInput

@Serializable
object ExtractBriarMetadataInputs : RInput


@Serializable
object CheckSBatchOutputInput : RInput
