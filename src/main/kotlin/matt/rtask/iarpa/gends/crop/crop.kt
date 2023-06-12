package matt.rtask.iarpa.gends.crop

import matt.async.executors.withFailableDaemonPool
import matt.async.par.parMap
import matt.async.thread.interrupt.checkIfInterrupted
import matt.briar.meta.extract.ExtractedFramesMetaData
import matt.briar.meta.extract.ExtractedMetaData
import matt.ffmpeg.eval.Div
import matt.ffmpeg.filtergraph.crop.CropBox
import matt.ffmpeg.filtergraph.crop.crop
import matt.ffmpeg.filtergraph.filterGraph
import matt.ffmpeg.filtergraph.scale.Absolute
import matt.ffmpeg.filtergraph.scale.ScaleEval.init
import matt.ffmpeg.filtergraph.scale.scale
import matt.ffmpeg.filtergraph.select.SelectedFrame
import matt.ffmpeg.filtergraph.select.select
import matt.ffmpeg.filtergraph.sendcmd.ffmpegCommand
import matt.ffmpeg.filtergraph.sendcmd.sendcmdFile
import matt.ffmpeg.filtergraph.setpts.setPts
import matt.file.CaseSensitivity.CaseSensitive
import matt.file.construct.mFile
import matt.file.ext.FileExtension
import matt.imagemagick.ImageMagickOptions
import matt.imagemagick.mogrify
import matt.json.prim.loadJson
import matt.json.prim.save
import matt.lang.anno.SeeURL
import matt.log.CountPrinter
import matt.model.data.byte.bytes
import matt.prim.str.mybuild.lineDelimitedString
import matt.rtask.iarpa.gends.crop.crop.CroppedFrame
import matt.rtask.iarpa.gends.crop.crop.generateCrop
import matt.rtask.iarpa.gends.crop.png.PngSizePredictor
import matt.rtask.rinput.PrepareBriarCrops
import matt.shell.CommandReturner
import matt.shell.DEFAULT_LINUX_PROGRAM_PATH_CONTEXT
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds


fun prepareBriarCrops(rArg: PrepareBriarCrops) {
    val extractedVidsCounter = CountPrinter(printEvery = 100) { "finished preparing filtergraph of vid $it..." }
    val metadata = rArg.computeContext.files.briarExtractMetadataFile.loadJson<ExtractedMetaData>()
    val totalBytesEstimate = AtomicLong()
    val imageCount = AtomicInteger()
    val pngSizePredictor = PngSizePredictor()
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
            val framesFolder = mFile(framesFile.abspath.substringBeforeLast("."), caseSensitivity = CaseSensitive)
            val ffmpegFilterFile = framesFile.resRepExt(FileExtension.FFMPEG)
            val ffmpegCommandFile = mFile(ffmpegFilterFile.abspath + FileExtension.COMMAND.withPrefixDot)
            val mogScript = ffmpegFilterFile.resRepExt(FileExtension.MOG)

            val selectedFrames = frames.map {
                val theCrop = it.generateCrop(totalWidth = totalWidth, totalHeight = totalHeight)
                it.crop = theCrop
                imageCount.incrementAndGet()
                totalBytesEstimate.addAndGet(
                    pngSizePredictor.predictSize(
                        width = theCrop.width,
                        height = theCrop.height
                    ).bytes
                )
                SelectedFrame(frame = it.index)
            }
            val crops = frames.map { CroppedFrame(it.index, it.crop!!) }
            val maxWidth = crops.maxOf { it.crop.width }
            val maxHeight = crops.maxOf { it.crop.height }

            ffmpegCommandFile.text = ffmpegCommand {
                crops.forEach {
                    interval(it.index.seconds) {
                        enter {
                            crop x it.crop.x
                            crop y it.crop.y
                        }
                    }
                }
            }.code
            mogScript.text = lineDelimitedString {
                crops.forEach {
                    +CommandReturner(DEFAULT_LINUX_PROGRAM_PATH_CONTEXT).mogrify.run(
                        options = ImageMagickOptions(crop = "${it.crop.width}x${it.crop.height}+0+0"),
                        file = framesFolder[it.index.toString() + ".png"]
                    ).rawWithNoEscaping()
                }
            }
            val theFilter = filterGraph {
                chain {
                    setPts {
                        @SeeURL("https://ffmpeg.org/ffmpeg-filters.html#toc-Examples-163")
                        Div(N, TB)
                    }
                    select(selectedFrames, inputs = listOf(), outputs = listOf())
                    sendcmdFile(ffmpegCommandFile)
                    crop(crop = CropBox(width = maxWidth, height = maxHeight), keepAspect = null, exact = 1)
                    scale(width = Absolute(maxWidth), height = Absolute(maxHeight), eval = init)
                }
            }
            framesFile.save(framesMetadata)
            ffmpegFilterFile.text = theFilter.arg()
        }
    }

    val numImages = imageCount.get()
    val estimatedSize = totalBytesEstimate.get().bytes
    println("Num Images: $numImages")
    println("Estimated size: $estimatedSize")
    println("Estimated average size per PNG: ${estimatedSize / numImages}")

}
