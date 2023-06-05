package matt.rtask.iarpa.gends.summarize.check

import matt.briar.meta.MediaAnnotation
import matt.briar.meta.Modality.face
import matt.briar.meta.SensorType.specialized
import matt.lang.go
import matt.prim.str.takeIfNotBlank


fun MediaAnnotation.check() {
    description?.takeIfNotBlank()?.go {
        error("description: $it")
    }

    subject.subjectImageSpecificInfo.attire.description?.takeIfNotBlank()?.go {
        error("got an attire description: $it")
    }
    environment.location.venueDescription.takeIfNotBlank()?.takeIf { it.trim() != "ORNL west gate" }
        ?.go {
            error("got an unusual venue description: $it")
        }
    if (modality == face) {
        require(sensorInfo.type == specialized) {
            "I thought all face modality videos were from \"specialized\" cameras..."
        }
    }
}