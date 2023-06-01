package matt.rtask.profile

import matt.file.commons.rcommons.OM_LATEST_JP_SNAPSHOT
import matt.file.commons.rcommons.OM_SNAPSHOT_FOLDER
import matt.lang.function.Op
import matt.log.profile.jp.JProfiler
import matt.log.profile.real.Profiler

inline fun openMindProfile(op: Op) {
    val profiler = Profiler(
        engine = JProfiler(snapshotFolder = OM_SNAPSHOT_FOLDER)
    ) {
        println("saved snapshot: $it")
        println("copying to ${OM_LATEST_JP_SNAPSHOT.name}...")
        it.copyTo(OM_LATEST_JP_SNAPSHOT, overwrite = true)
        println("finished copying snapshot")
    }
    profiler.recordCPU {
        op()
    }
}