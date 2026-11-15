package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.KaratPrice
import com.example.model.Shop
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

sealed interface AiAdvisorUiState {
    data object Idle : AiAdvisorUiState
    data object Loading : AiAdvisorUiState
    data class Streaming(val text: String) : AiAdvisorUiState
    data class Success(val text: String) : AiAdvisorUiState
    data class Error(val message: String) : AiAdvisorUiState
}

data class AdvisorMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
)

class AiGoldAdvisorViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val _uiState = MutableStateFlow<AiAdvisorUiState>(AiAdvisorUiState.Idle)
    val uiState: StateFlow<AiAdvisorUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<AdvisorMessage>>(emptyList())
    val messages: StateFlow<List<AdvisorMessage>> = _messages.asStateFlow()

    private var lastQuestion: String? = null
    private var lastPrices: List<KaratPrice> = emptyList()
    private var lastShops: List<Shop> = emptyList()

    fun askAdvisor(
        question: String,
        prices: List<KaratPrice>,
        shops: List<Shop>,
    ) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return

        lastQuestion = trimmed
        lastPrices = prices
        lastShops = shops

        _messages.update { it + AdvisorMessage(isUser = true, text = trimmed) }
        runInference(trimmed, prices, shops)
    }

    fun retryLastQuestion() {
        val q = lastQuestion ?: return
        runInference(q, lastPrices, lastShops)
    }

    private fun runInference(
        question: String,
        prices: List<KaratPrice>,
        shops: List<Shop>,
    ) {
        val prompt = buildGroundedPrompt(question, prices, shops)
        viewModelScope.launch {
            _uiState.value = AiAdvisorUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(TIMEOUT_MS) {
                    model.generateContentStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            accumulated.append(chunkText)
                            _uiState.value = AiAdvisorUiState.Streaming(accumulated.toString())
                        }
                    }
                }
                val finalAnswer = accumulated.toString().trim()
                if (finalAnswer.isNotEmpty()) {
                    _messages.update {
                        it + AdvisorMessage(isUser = false, text = finalAnswer)
                    }
                    _uiState.value = AiAdvisorUiState.Success(finalAnswer)
                } else {
                    _uiState.value = AiAdvisorUiState.Error("لم يرجع النموذج إجابة. حاول إعادة المحاولة.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini streaming timed out", e)
                _uiState.value = AiAdvisorUiState.Error(
                    "انتهت مهلة الاتصال بعد 60 ثانية. يرجى المحاولة مرة أخرى.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _uiState.value = AiAdvisorUiState.Error(
                    e.localizedMessage ?: "تعذّر الاتصال بمستشار Gemini حالياً.",
                )
            }
        }
    }

    private fun buildGroundedPrompt(
        question: String,
        prices: List<KaratPrice>,
        shops: List<Shop>,
    ): String {
        val priceLines = prices.joinToString("\n") { p ->
            val unit = if (p.karat == 0) "جنيه مصري للقطعة (8 جم عيار 21)" else "جنيه مصري / جرام"
            "- ${p.label}: ${String.format("%.0f", p.pricePerGram)} $unit"
        }
        val shopLines = shops.joinToString("\n") { s ->
            "- ${s.name} (${s.area}) | التقييم: ${s.rating}★ | المسافة: ${s.distanceKm} كم | المصنعية: ${String.format("%.0f", s.makingFeePerGram)} ج.م/جم"
        }
        return """
            أنت «مستشار الملك الذكي» داخل تطبيق King Gold لشراء الذهب الفعلي من محلات صاغة موثّقة في مصر بنظام الضمان (Escrow).
            أجب باللغة العربية الواضحة والمختصرة والعملية، واستند إلى البيانات اللحظية التالية عند إجراء أي حسابات:

            الأسعار الحالية في التطبيق:
            $priceLines

            المحلات الموثّقة المتاحة:
            $shopLines

            معلومة نظام الضمان: المبلغ يُحفظ في حساب الضمان ولا يُصرف للمحل إلا بعد معاينة الوزن والدمغة الرسمية واستلام الفاتورة وتأكيد رمز الاستلام.

            سؤال المستخدم:
            $question
        """.trimIndent()
    }

    private companion object {
        const val TAG = "AiGoldAdvisorVM"
        const val TIMEOUT_MS = 60_000L
    }
}
