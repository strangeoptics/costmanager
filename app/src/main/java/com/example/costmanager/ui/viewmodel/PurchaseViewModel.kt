package com.example.costmanager.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.costmanager.ai.GeminiProVisionModel
import com.example.costmanager.data.AppDatabase
import com.example.costmanager.data.Position
import com.example.costmanager.data.Purchase
import com.example.costmanager.data.PurchaseDao
import com.example.costmanager.data.PurchaseWithPositions
import com.example.costmanager.data.SettingsManager
import com.example.costmanager.ui.dialogs.PositionInput
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object SampleData {
    val purchases = listOf(
        PurchaseWithPositions(
            purchase = Purchase(id = 1, purchaseDate = Date(), store = "Supermarkt A", storeType = "supermarkt", totalPrice = 55.75),
            positions = listOf(
                Position(id = 1, purchaseId = 1, itemName = "Milch", itemType = "Lebensmittel", quantity = 2.0, unit = "Liter", unitPrice = 1.50, price = 3.00),
                Position(id = 2, purchaseId = 1, itemName = "Brot", itemType = "Lebensmittel", quantity = 1.0, unit = "Stück", unitPrice = 2.50, price = 2.50)
            )
        ),
        PurchaseWithPositions(
            purchase = Purchase(id = 2, purchaseDate = Date(), store = "Tankstelle B", storeType = "tankstelle", totalPrice = 75.50),
            positions = listOf(
                Position(id = 3, purchaseId = 2, itemName = "Super Benzin", itemType = "Treibstoff", quantity = 50.0, unit = "Liter", unitPrice = 1.51, price = 75.50)
            )
        )
    )
}

// --- Data classes for Gson parsing ---
data class GeminiPurchaseResponse(
    @SerializedName("purchaseDate") val purchaseDate: String,
    @SerializedName("store") val store: String,
    @SerializedName("storeType") val storeType: String,
    @SerializedName("totalPrice") val totalPrice: Double,
    @SerializedName("positions") val positions: List<GeminiPositionResponse>
)

data class GeminiPositionResponse(
    @SerializedName("itemName") val itemName: String,
    @SerializedName("itemType") val itemType: String,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName(value = "unit") val unit: String,
    @SerializedName("unitPrice") val unitPrice: Double,
    @SerializedName("price") val price: Double
)

data class DatePickerRequest(val purchaseId: Long)

sealed class UndoAction {
    data class DeletePurchase(val data: PurchaseWithPositions) : UndoAction()
    data class DeletePosition(val position: Position) : UndoAction()
}


class PurchaseRepository(private val purchaseDao: PurchaseDao) {
    fun getAllPurchasesStream() = purchaseDao.getAllPurchasesWithPositions().flowOn(Dispatchers.IO)

    fun getPurchaseStream(id: Long) = purchaseDao.getPurchaseWithPositions(id).flowOn(Dispatchers.IO)

    suspend fun getPurchaseWithPositions(id: Long): PurchaseWithPositions? {
        return purchaseDao.getPurchaseWithPositionsNow(id)
    }

    suspend fun getPurchasesBetween(startDate: Date, endDate: Date): List<PurchaseWithPositions> {
        return purchaseDao.getPurchasesWithPositionsBetween(startDate, endDate)
    }

    suspend fun insertPurchaseWithPositions(purchase: Purchase, positions: List<Position>): Long {
        return purchaseDao.insertPurchaseWithPositions(purchase, positions)
    }

    suspend fun insertPositions(positions: List<Position>) {
        purchaseDao.insertPositions(positions)
    }

    suspend fun updatePurchase(purchase: Purchase) {
        purchaseDao.updatePurchase(purchase)
    }

    suspend fun updatePosition(position: Position) {
        purchaseDao.updatePosition(position)
    }

    suspend fun updatePurchaseDate(purchaseId: Long, newDate: Date) {
        purchaseDao.updatePurchaseDate(purchaseId, newDate)
    }

    suspend fun deletePurchase(purchase: Purchase) {
        purchaseDao.deletePurchase(purchase)
    }

    suspend fun deletePosition(position: Position) {
        purchaseDao.deletePosition(position)
    }

    suspend fun insertSampleData() {
        SampleData.purchases.forEach { purchaseWithPositions ->
            purchaseDao.insertPurchaseWithPositions(
                purchaseWithPositions.purchase,
                purchaseWithPositions.positions
            )
        }
    }
}


class PurchaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PurchaseRepository
    private val geminiModel: GeminiProVisionModel
    private val settingsManager: SettingsManager

    val allPurchases: StateFlow<List<PurchaseWithPositions>>

    private val _datePickerRequest = MutableStateFlow<DatePickerRequest?>(null)
    val datePickerRequest = _datePickerRequest.asStateFlow()

    private val _undoState = MutableStateFlow<UndoAction?>(null)
    val undoState = _undoState.asStateFlow()

    // --- Speech Recognition Properties ---
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val audioBuffer = ByteArrayOutputStream()
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var recordingJob: Job? = null


    init {
        val purchaseDao = AppDatabase.getDatabase(application).purchaseDao()
        repository = PurchaseRepository(purchaseDao)
        geminiModel = GeminiProVisionModel(application)
        settingsManager = SettingsManager(application)
        allPurchases = repository.getAllPurchasesStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // --- Speech-to-Text Methods ---
    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return

        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)

        audioBuffer.reset()
        audioRecord?.startRecording()
        isRecording = true

        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            val data = ByteArray(bufferSize)
            while (isActive && isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize)
                if (read != null && read > 0) {
                    audioBuffer.write(data, 0, read)
                }
            }
        }
    }

    fun stopRecordingAndTranscribe(onResult: (String?) -> Unit) {
        if (!isRecording) {
            onResult(null)
            return
        }

        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        if (audioBuffer.size() == 0) {
            Log.w("SpeechToText", "Audio buffer is empty. No data to transcribe.")
            onResult(null)
            return
        }


        val audioData = audioBuffer.toByteArray()
        val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)

        viewModelScope.launch {
            val apiKey = settingsManager.getApiKey()
            if (apiKey.isBlank()) {
                Log.e("SpeechToText", "API Key is not set.")
                onResult(null)
                return@launch
            }
            val result = callSpeechApi(base64Audio, apiKey)
            onResult(result)
        }
    }

    private suspend fun callSpeechApi(base64Audio: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://speech.googleapis.com/v1/speech:recognize?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true

            val jsonRequest = JSONObject().apply {
                put("config", JSONObject().apply {
                    put("encoding", "LINEAR16")
                    put("sampleRateHertz", 16000)
                    put("languageCode", Locale.getDefault().toLanguageTag())
                })
                put("audio", JSONObject().apply {
                    put("content", base64Audio)
                })
            }

            connection.outputStream.use { it.write(jsonRequest.toString().toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("SpeechToText", "API Response: $response")
                val jsonResponse = JSONObject(response)

                if (jsonResponse.has("results")) {
                    val results = jsonResponse.getJSONArray("results")
                    if (results.length() > 0) {
                        val alternatives = results.getJSONObject(0).getJSONArray("alternatives")
                        if (alternatives.length() > 0) {
                            return@withContext alternatives.getJSONObject(0).getString("transcript")
                        }
                    }
                } else {
                    Log.w("SpeechToText", "API response does not contain 'results' key.")
                }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e("SpeechToText", "API Error: $responseCode - $error")
            }
        } catch (e: Exception) {
            Log.e("SpeechToText", "Exception in callSpeechApi", e)
        }
        null
    }


    fun getPurchase(id: Long): StateFlow<PurchaseWithPositions?> {
        return repository.getPurchaseStream(id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    suspend fun getPurchasesAsJson(startDate: Date, endDate: Date): String {
        val purchases = repository.getPurchasesBetween(startDate, endDate)
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(purchases)
    }

    fun importPurchasesFromJson(jsonString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<PurchaseWithPositions>>() {}.type
                val purchasesToImport: List<PurchaseWithPositions> = gson.fromJson(jsonString, type)

                purchasesToImport.forEach { pwp ->
                    // Make sure IDs are reset so the DB can generate new ones
                    val purchaseToInsert = pwp.purchase.copy(id = 0)
                    val positionsToInsert = pwp.positions.map { it.copy(id = 0, purchaseId = 0) }
                    repository.insertPurchaseWithPositions(purchaseToInsert, positionsToInsert)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Optionally, provide feedback to the UI about the failure
            }
        }
    }

    fun deletePurchase(purchaseWithPositions: PurchaseWithPositions) {
        viewModelScope.launch(Dispatchers.IO) {
            _undoState.value = UndoAction.DeletePurchase(purchaseWithPositions)
            repository.deletePurchase(purchaseWithPositions.purchase)
        }
    }

    fun deletePosition(position: Position) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePosition(position)
            _undoState.value = UndoAction.DeletePosition(position)

            // Recalculate total and update purchase
            val purchaseWithPositions = repository.getPurchaseWithPositions(position.purchaseId)
            if (purchaseWithPositions != null) {
                val newTotalPrice = purchaseWithPositions.positions.sumOf { it.price }
                val updatedPurchase = purchaseWithPositions.purchase.copy(totalPrice = newTotalPrice)
                repository.updatePurchase(updatedPurchase)
            }
        }
    }

    fun undoDelete() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val action = _undoState.value) {
                is UndoAction.DeletePurchase -> {
                    // Reset IDs to 0 to force new insertion and avoid conflicts
                    val newPurchase = action.data.purchase.copy(id = 0)
                    val newPositions = action.data.positions.map { it.copy(id = 0) }
                    repository.insertPurchaseWithPositions(newPurchase, newPositions)
                }
                is UndoAction.DeletePosition -> {
                    // Reset ID to 0 to force new insertion
                    repository.insertPositions(listOf(action.position.copy(id = 0)))

                    // Recalculate total and update purchase
                    val purchaseWithPositions = repository.getPurchaseWithPositions(action.position.purchaseId)
                    if (purchaseWithPositions != null) {
                        val newTotalPrice = purchaseWithPositions.positions.sumOf { it.price }
                        val updatedPurchase = purchaseWithPositions.purchase.copy(totalPrice = newTotalPrice)
                        repository.updatePurchase(updatedPurchase)
                    }
                }
                null -> {} // Nothing to undo
            }
            _undoState.value = null
        }
    }


    fun createPurchaseFromImage(bitmap: Bitmap, onFinished: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val resultJson = geminiModel.getPurchaseFromImage(bitmap)
            if (resultJson != null) {
                try {
                    val cleanedJson = resultJson.replace("```json", "").replace("```", "").trim()
                    val gson = Gson()
                    val responseType = object : TypeToken<GeminiPurchaseResponse>() {}.type
                    val geminiResponse: GeminiPurchaseResponse = gson.fromJson(cleanedJson, responseType)

                    var purchaseDate: Date? = null
                    var dateNeedsCorrection = false

                    if (geminiResponse.purchaseDate.isNullOrEmpty() || geminiResponse.purchaseDate.contains("YYYY-MM-DD", ignoreCase = true)) {
                        dateNeedsCorrection = true
                    } else {
                        try {
                            purchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).parse(geminiResponse.purchaseDate)
                        } catch (e: ParseException) {
                            dateNeedsCorrection = true
                            e.printStackTrace()
                        }
                    }

                    if (purchaseDate == null) {
                        purchaseDate = Date() // Fallback to current date
                    }

                    val newPurchase = Purchase(
                        purchaseDate = purchaseDate,
                        store = geminiResponse.store,
                        storeType = geminiResponse.storeType.lowercase(Locale.GERMANY),
                        totalPrice = geminiResponse.totalPrice
                    )

                    val newPositions = geminiResponse.positions.map {
                        Position(
                            purchaseId = 0,
                            itemName = it.itemName,
                            itemType = it.itemType,
                            quantity = it.quantity,
                            unit = it.unit,
                            unitPrice = it.unitPrice,
                            price = it.price
                        )
                    }
                    val newPurchaseId = repository.insertPurchaseWithPositions(newPurchase, newPositions)

                    if (dateNeedsCorrection) {
                        _datePickerRequest.value = DatePickerRequest(newPurchaseId)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    withContext(Dispatchers.Main) {
                        onFinished()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onFinished()
                }
            }
        }
    }

    fun processSpeechInput(
        text: String,
        callback: (store: String, storeType: String, date: Date, position: PositionInput?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val resultJson = geminiModel.getPurchaseFromText(text)
            if (resultJson != null) {
                try {
                    val cleanedJson = resultJson.replace("```json", "").replace("```", "").trim()
                    val gson = Gson()
                    val responseType = object : TypeToken<GeminiPurchaseResponse>() {}.type
                    val geminiResponse: GeminiPurchaseResponse = gson.fromJson(cleanedJson, responseType)

                    val purchaseDate = try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).parse(geminiResponse.purchaseDate) ?: Date()
                    } catch (e: ParseException) {
                        Date()
                    }

                    val firstPosition = geminiResponse.positions.firstOrNull()?.let {
                        PositionInput(
                            itemName = it.itemName,
                            itemType = it.itemType,
                            quantity = it.quantity,
                            unit = it.unit,
                            unitPrice = it.unitPrice
                        )
                    }

                    withContext(Dispatchers.Main) {
                        callback(
                            geminiResponse.store,
                            geminiResponse.storeType,
                            purchaseDate,
                            firstPosition
                        )
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    fun addPurchase(store: String, storeType: String, date: Date, positionInput: PositionInput?) {
        viewModelScope.launch(Dispatchers.IO) {
            val newPositions = mutableListOf<Position>()
            var totalPrice = 0.0

            positionInput?.let {
                val price = it.quantity * it.unitPrice
                totalPrice = price
                newPositions.add(
                    Position(
                        purchaseId = 0, // Will be set by Room
                        itemName = it.itemName,
                        itemType = it.itemType,
                        quantity = it.quantity,
                        unit = it.unit,
                        unitPrice = it.unitPrice,
                        price = price
                    )
                )
            }

            val newPurchase = Purchase(
                purchaseDate = date,
                store = store,
                storeType = storeType,
                totalPrice = totalPrice
            )
            repository.insertPurchaseWithPositions(newPurchase, newPositions)
        }
    }


    fun addPosition(purchaseId: Long, itemName: String, itemType: String, quantity: Double, unit: String, unitPrice: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val price = quantity * unitPrice
            val newPosition = Position(
                purchaseId = purchaseId,
                itemName = itemName,
                itemType = itemType,
                quantity = quantity,
                unit = unit,
                unitPrice = unitPrice,
                price = price
            )
            repository.insertPositions(listOf(newPosition))

            val purchaseWithPositions = repository.getPurchaseWithPositions(purchaseId)
            if (purchaseWithPositions != null) {
                val newTotalPrice = purchaseWithPositions.positions.sumOf { it.price }
                val updatedPurchase = purchaseWithPositions.purchase.copy(totalPrice = newTotalPrice)
                repository.updatePurchase(updatedPurchase)
            }
        }
    }

    fun updatePurchase(purchase: Purchase) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePurchase(purchase)
        }
    }

    fun updatePosition(position: Position) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePosition(position)
            val purchaseWithPositions = repository.getPurchaseWithPositions(position.purchaseId)
            if (purchaseWithPositions != null) {
                val newTotalPrice = purchaseWithPositions.positions.sumOf { it.price }
                val updatedPurchase = purchaseWithPositions.purchase.copy(totalPrice = newTotalPrice)
                repository.updatePurchase(updatedPurchase)
            }
        }
    }

    fun updatePurchaseDate(purchaseId: Long, newDate: Date) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePurchaseDate(purchaseId, newDate)
        }
        _datePickerRequest.value = null // Reset request
    }

    fun dismissDatePicker() {
        _datePickerRequest.value = null
    }

    fun insertSampleData() = viewModelScope.launch(Dispatchers.IO) {
        repository.insertSampleData()
    }
}