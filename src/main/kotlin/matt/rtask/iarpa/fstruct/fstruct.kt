package matt.rtask.iarpa.fstruct

import matt.file.commons.rcommons.BRIAR_EXTRACT_DATA_FOLDER
import matt.rtask.iarpa.briar.BriarTrainingFolder
import matt.rtask.iarpa.briar.BriarVideo

val BriarVideo.extractFolder
    get() = vidFile
        .relativeTo(BriarTrainingFolder.folder)
        .let { BRIAR_EXTRACT_DATA_FOLDER[it] }
        .let { it.resolveSibling(it.name.substringBefore(".")) }
