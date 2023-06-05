package matt.rtask.iarpa.gends.crop

import matt.async.executors.withFailableDaemonPool
import matt.async.par.parMap
import matt.async.thread.interrupt.checkIfInterrupted
import matt.briar.meta.extract.ExtractedFramesMetaData
import matt.briar.meta.extract.ExtractedMetaData
import matt.ffmpeg.filtergraph.crop.crop
import matt.ffmpeg.filtergraph.filterGraph
import matt.ffmpeg.filtergraph.select.SelectedFrame
import matt.ffmpeg.filtergraph.select.select
import matt.file.construct.mFile
import matt.file.ext.FileExtension
import matt.json.prim.loadJson
import matt.json.prim.save
import matt.log.CountPrinter
import matt.rtask.iarpa.gends.crop.crop.generateCrop
import matt.rtask.iarpa.gends.crop.crop.toCropBox
import matt.rtask.rinput.PrepareBriarCrops


fun prepareBriarCrops(rArg: PrepareBriarCrops) {
    val extractedVidsCounter = CountPrinter(printEvery = 100) { "finished preparing filtergraph of vid $it..." }

    val metadata = rArg.computeContext.files.briarExtractMetadataFile.loadJson<ExtractedMetaData>()
    withFailableDaemonPool {
        metadata.videos.parMap { vid ->
            checkIfInterrupted()
            extractedVidsCounter.click()
            val framesFile = mFile(vid.framesMetaDataFile)
            val framesMetadata = framesFile.loadJson<ExtractedFramesMetaData>()
            val totalRes = vid.metadata.mediaInfo.resolution
            val totalWidth = totalRes.width
            val totalHeight = totalRes.height
            val frames = framesMetadata.frames
            val ffmpegFilterFile = framesFile.resRepExt(FileExtension.FFMPEG)
            val selectedFrames = frames.map {
                val theCrop = it.generateCrop(totalWidth = totalWidth, totalHeight = totalHeight)
                it.crop = theCrop
                SelectedFrame(
                    frame = it.index
                )
            }
            val streams = selectedFrames.map { "N${it.frame}" }
            val theFilter = filterGraph {
                chain {
                    select(selectedFrames, inputs = listOf(), outputs = streams)
                }
                streams.zip(frames.map {
                    it.crop!!.toCropBox()
                }).forEach { (s, c) ->
                    chain {
                        crop(
                            c,
                            inputs = listOf(s),
                            outputs = listOf()
                        )
                    }
                }
            }

            framesFile.save(framesMetadata)
            ffmpegFilterFile.text = theFilter.arg()
        }
    }

}
