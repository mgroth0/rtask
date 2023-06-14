package matt.rtask.iarpa.gends.filter.stimselect

import matt.briar.SimilarityMatrix
import matt.briar.StimuliSelectionContext
import matt.briar.TrialConfiguration
import matt.briar.meta.MediaAnnotation
import matt.briar.meta.Subject
import matt.briar.meta.SubjectID
import matt.collect.itr.filterNotNull
import matt.rtask.iarpa.gends.filter.util.calculateBMI
import kotlin.math.abs

const val BMI_DIFF_THRESHOLD = 25
const val SIMILARITY_COEF_THRESHOLD = 0.65

class StimuliSelectionContextImpl(
    val simMat: SimilarityMatrix,
    val videos: List<MediaAnnotation>
) : StimuliSelectionContext {

    private val subjects by lazy {
        videos.map { it.subject }.filter { it.id.id in simMat.ids }.associateBy { it.id }
    }

    private val subjectIndices by lazy {
        subjects.keys.associateWith {
            simMat.ids.indexOf(it.id)
        }
    }

    class Similarity(val subject: Subject, val coef: Double)

    private val similarities by lazy {
        subjects.keys.associateWith { subjectID ->
            val subjectIndex = subjectIndices[subjectID]!!
            val list = simMat.corrmat[subjectIndex]
            list.mapIndexed { i, coef -> Similarity(subject = subjects[SubjectID(simMat.ids[i])]!!, coef = coef) }
                .sortedByDescending { it.coef }
                .filter { it.subject.id != subjectID }
        }
    }

    private val BMIs by lazy {
        subjects.entries.associate {
            it.key to it.value.bmi()
        }
    }

    val trialConfigs by lazy {
        simMat.ids.map { SubjectID(it) }.map { subID ->
            val querySubject = subjects[subID]!!
            val similarities = similarities[subID]!!
            val distractors = similarities.filter {
                it.subject.subjectPersistentInfo.sex == querySubject.subjectPersistentInfo.sex
            }.filter {
                val bmiDiff = BMIs[querySubject.id]!! - BMIs[it.subject.id]!!
                val absBmiDiff = abs(bmiDiff)
                absBmiDiff < BMI_DIFF_THRESHOLD
            }.filter {
                it.coef > SIMILARITY_COEF_THRESHOLD
            }.take(4).map { it.subject.id }
            if (distractors.size == 4) {
                TrialConfiguration(
                    query = subID,
                    distractors = distractors
                )
            } else null
        }.filterNotNull()
    }

    override val distractorImagesNeeded by lazy {
        subjects.keys.associateWith { subject ->
            trialConfigs.count {
                subject in it.distractors
            }
        }
    }

}

fun Subject.bmi() = calculateBMI(
    heightInInches = subjectPersistentInfo.height_inches.toDouble(),
    weightInPounds = subjectPersistentInfo.weight_lbs.toDouble()
)


