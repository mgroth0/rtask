package matt.rtask.toc

import matt.log.warn.warnOnce

private var last_toc = System.getenv("last_toc")?.toLong()

/*See: [[bashToc]]*/
fun mimicBashToc(label: String) {
    val tic = System.getenv("tic")?.toLong() ?: run {
        warnOnce("tic was null")
        return
    }
    val toc = System.currentTimeMillis()
    val diff = toc - (last_toc ?: run {
        warnOnce("last_toc was null")
        return
    })
    val total = toc - tic
    last_toc = toc
    System.out.printf(
        "%-35s %-10s %-10s%n",
        "$label:",
        "D:$diff ms",
        "T:$total ms"
    )
}