package matt.rtask.iarpa.gends.summarize

import com.google.common.collect.Iterables
import matt.async.executors.withFailableDaemonPool
import matt.async.par.parMap
import matt.briar.meta.SubjectID
import matt.briar.meta.extract.ExtractedFramesMetaData
import matt.briar.meta.extract.ExtractedMetaData
import matt.briar.meta.extract.ProcessedFrameMetadata
import matt.briar.orientation.OrientationBinner
import matt.briar.orientation.OrientationBinner.PITCH_RADIUS
import matt.briar.orientation.OrientationBinner.YAW_RADIUS
import matt.file.construct.mFile
import matt.file.toMFile
import matt.html.fig.report.htmlReport
import matt.json.prim.loadJson
import matt.log.CountPrinter
import matt.prim.str.mybuild.lineDelimitedString
import matt.prim.str.takeIfNotBlank
import matt.rtask.iarpa.gends.summarize.check.check
import matt.rtask.iarpa.gends.tabular.tabulartwo.TabularVideo2
import matt.rtask.rinput.SummarizeBriarMetadataInputs
import matt.rtask.tabular.count.counts
import matt.rtask.tabular.csv.toCsv
import matt.rtask.tabular.format.HIDE_ZEROS
import matt.rtask.tabular.format.formatForConsole
import matt.rtask.tabular.pivot.pivot
import matt.rtask.tabular.stat.stats
import matt.rtask.tabular.totable.toTable
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.time.DurationUnit.MINUTES

fun summarizeBriarMetadata(input: SummarizeBriarMetadataInputs) {
    val extractedVidsCounter = CountPrinter(printEvery = 1000) { "finished processing vid $it..." }
    val metadata = input
        .extraction
        .briarExtractMetadataFile
        .loadJson<ExtractedMetaData>()
    val videoTable = LinkedList<TabularVideo2>()
    val framesMonitor = object {}
    var frameTable: Iterable<ProcessedFrameMetadata> = LinkedList<ProcessedFrameMetadata>()
    withFailableDaemonPool {
        metadata.videos.filter { it.framesMetaDataFile != null }.parMap { vid ->
            extractedVidsCounter.click()
            val framesMetadata = mFile(vid.framesMetaDataFile!!).loadJson<ExtractedFramesMetaData>()
            synchronized(videoTable) {
                videoTable += TabularVideo2(vid.metadata)
            }
            val vidMetadata = vid.metadata
            val processedFrames = framesMetadata.frames.map {
                ProcessedFrameMetadata(
                    OrientationBinner.bin(
                        yaw = it.faceOrientation!!.yaw,
                        pitch = it.faceOrientation!!.pitch
                    ),
                    vid = vidMetadata
                )

            }
            synchronized(framesMonitor) {
                frameTable = Iterables.concat(frameTable, processedFrames)
            }

            vidMetadata.check()
        }

    }


    val dist = TabularVideo2::distance
    val videoPivotTables = lineDelimitedString {
        fun <P1 : Comparable<P1>, P2 : Comparable<P2>> piv(
            p1: KProperty1<TabularVideo2, P1>,
            p2: KProperty1<TabularVideo2, P2>
        ): String = videoTable.pivot(p1, p2).formatForConsole(formatter = HIDE_ZEROS)
        +piv(TabularVideo2::cameraModel, dist)
        +piv(TabularVideo2::cameraModel, TabularVideo2::sensorElevation)
        +piv(TabularVideo2::clothingSet, dist)
        +piv(TabularVideo2::subject, dist)
        +piv(TabularVideo2::activity, dist)
        +piv(TabularVideo2::venueName, dist)
        +piv(TabularVideo2::raceLabel, TabularVideo2::raceAdditionalInfo)
        +piv(TabularVideo2::spectrum, dist)
        +piv(TabularVideo2::modality, dist)
        +piv(TabularVideo2::modality, TabularVideo2::resolution)
        +piv(TabularVideo2::modality, TabularVideo2::activity)
        +piv(TabularVideo2::modality, TabularVideo2::trackDescription)
        +piv(TabularVideo2::modality, TabularVideo2::colorSpace)
        +piv(TabularVideo2::modality, TabularVideo2::fps)
        +piv(TabularVideo2::modality, TabularVideo2::subject)
        +piv(TabularVideo2::modality, TabularVideo2::cameraModel)
        +piv(TabularVideo2::modality, TabularVideo2::cameraType)
        +piv(TabularVideo2::modality, TabularVideo2::focalLength)
        +piv(TabularVideo2::modality, TabularVideo2::camera)
        +frameTable.pivot(
            "modality",
            { it.vid.modality },
            "trackDescription",
            { it.vid.detailedAnnotation.completeAnnotation.description }
        ).formatForConsole(formatter = HIDE_ZEROS)
        +piv(TabularVideo2::bitsPerPixel, dist)
        +piv(TabularVideo2::resolution, dist)
        +piv(TabularVideo2::colorSpace, dist)
        +piv(TabularVideo2::fps, dist)
    }


    val framePivotTables = lineDelimitedString {
        +"yawRadius=$YAW_RADIUS"
        +"pitchRadius=$PITCH_RADIUS"
        +frameTable.counts(
            ProcessedFrameMetadata::orientation,
        ).formatForConsole(formatter = HIDE_ZEROS)
    }


    val rootOutputFolder = input.remoteOutputFolder.toMFile()
    rootOutputFolder.deleteIfExists()
    val extractOutputFolder = rootOutputFolder[input.extraction.extractName]
    /*if (extractOutputFolder.exists()) {
        extractOutputFolder.listFilesAsList()?.forEach {
            it.deleteIfExists()
        }
    }*/


    extractOutputFolder["summary.html"].text = htmlReport {
        pre(videoTable.stats(TabularVideo2::duration) { it.toDouble(MINUTES) }.formatForConsole())
        pre(videoTable.stats(TabularVideo2::totalFrames) { it.toDouble() }.formatForConsole())
        pre(videoTable.stats(TabularVideo2::cn2) { it?.raw?.takeIfNotBlank()?.toDouble() }.formatForConsole())
        pre(videoPivotTables)
        pre(framePivotTables)
    }


    extractOutputFolder["subjects.csv"].text =
        videoTable.associateBy { it.subject }.entries.toTable<Map.Entry<SubjectID, TabularVideo2>, SubjectID, String, Any>(
            name = "subjects",
            rowHeaders = { it.key }
        ) {
            value.run {
                mapOf(
                    "weight_lbs" to weight,
                    "height_inches" to height,
                )
            }
        }.toCsv()

    extractOutputFolder["videos.csv"].text = videoTable.withIndex().toTable<IndexedValue<TabularVideo2>, Int, String, Any>(
        name = "videos",
        rowHeaders = { it.index }
    ) {
        value.run {
            mapOf(
                "subject" to subject,
                "clothing" to clothingSet,
                "distance" to distance,
                "sex" to sex,
                "race" to raceLabel,
                "raceInfo" to raceAdditionalInfo
            )
        }
    }.toCsv()


}