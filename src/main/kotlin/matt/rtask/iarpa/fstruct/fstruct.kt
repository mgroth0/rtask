package matt.rtask.iarpa.fstruct

import matt.briar.BriarExtraction
import matt.file.ext.FileExtension
import matt.rtask.iarpa.briar.BriarVideo

context(BriarExtraction)
val BriarVideo.trackCacheFile get() = cacheFolder[relativeVidFile].resRepExt(FileExtension.CBOR)

context(BriarExtraction)
val BriarVideo.extractMetadataFile get() = extractFolder.resRepExt(FileExtension.JSON)

context(BriarExtraction)
val BriarVideo.extractFolder
    get() = relativeVidFile
        .let {
            briarExtractDataFolder[it]
        }
        .let { it.resolveSibling(it.name.substringBefore(".")) }
