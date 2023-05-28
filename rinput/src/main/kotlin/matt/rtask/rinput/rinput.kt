package matt.rtask.rinput

import kotlinx.serialization.Serializable
import matt.json.toJsonString

@Serializable
sealed interface RInput {
    fun serializeAsRinput() = toJsonString<RInput>()
}

@Serializable
object QuickCheck : RInput

@Serializable
object PrepareDatasetJsons : RInput


@Serializable
object CheckSBatchOutputInput : RInput
