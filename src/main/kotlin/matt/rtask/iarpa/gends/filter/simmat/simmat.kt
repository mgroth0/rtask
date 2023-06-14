package matt.rtask.iarpa.gends.filter.simmat

import matt.briar.SimilarityMatrix
import matt.file.commons.DATA_FOLDER
import matt.file.context.LocalComputeContext
import net.razorvine.pickle.Unpickler


context(LocalComputeContext)
fun loadSimMat(): SimilarityMatrix {
    val simMat = Unpickler().load(
        DATA_FOLDER["BRS1_Hojin_Similarity_Matrix"]["brs1_corr_similarity_for_j.pickle"].inputStream()
            .buffered()
    ) as Map<*, *>

    @Suppress("UNCHECKED_CAST")
    return SimilarityMatrix(
        ids = simMat["ids"] as List<String>,
        corrmat = simMat["corrmat"] as List<List<Double>>,
        sets = simMat["sets"] as List<String>,
    )
}