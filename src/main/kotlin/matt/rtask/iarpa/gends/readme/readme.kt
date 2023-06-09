package matt.rtask.iarpa.gends.readme

import matt.file.context.ComputeContextFiles.Companion.BRIAR_EXTRACT_METADATA_FILE_NAME
import matt.model.email.MY_MIT_EMAIL

fun briarExtractReadme() = """
        
        BRIAR data extracted by $MY_MIT_EMAIL
        
        $BRIAR_EXTRACT_METADATA_FILE_NAME contains a simple list of all metadata files, one per video. Each metadata file contains further information on that video.
        
        
    """.trimIndent()