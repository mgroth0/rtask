package matt.rtask.profile

import matt.file.context.ComputeContext
import matt.lang.function.Op
import matt.lang.profiling.IsProfilingWithJProfiler
import matt.log.profile.jp.JProfiler
import matt.log.profile.real.Profiler

context(ComputeContext)
inline fun openMindProfile(op: Op) {
    val profiler = Profiler(
        enableAll = IsProfilingWithJProfiler,
        engine = JProfiler(snapshotFolder = files.snapshotFolder)
    ) {
        println("saved snapshot: $it")
        println("copying to ${files.latestJpSnapshot.name}...")
        it.copyTo(files.latestJpSnapshot, overwrite = true)
        println("finished copying snapshot")
    }
    profiler.recordCPU {
        op()
    }
}