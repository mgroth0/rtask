package matt.rtask.iarpa.fstruct

import matt.file.context.ComputeContext
import matt.rtask.iarpa.briar.BriarVideo

context(ComputeContext)
val BriarVideo.extractFolder
    get() = relativeVidFile
        .let {
            files.briarExtractDataFolder[it]
        }
        .let { it.resolveSibling(it.name.substringBefore(".")) }
