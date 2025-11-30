package com.example.costmanager.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.costmanager.data.SettingsManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

class GeminiProVisionModel(private val context: Context) {

    private fun getGenerativeModel(): GenerativeModel {
        val settingsManager = SettingsManager(context)
        val apiKey = settingsManager.getApiKey()
        if (apiKey.isBlank()) {
            Log.e("GeminiResponse", "API Key is not set in settings.")
            // You might want to throw an exception here or handle it gracefully
        }
        return GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val shortSide = min(bitmap.width, bitmap.height)
        if (shortSide <= 1536) {
            return bitmap
        }

        val scaleFactor = 1536.0 / shortSide
        val newWidth = (bitmap.width * scaleFactor).roundToInt()
        val newHeight = (bitmap.height * scaleFactor).roundToInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    suspend fun getPurchaseFromText(text: String): String? {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())
        val prompt = """
        Analysiere den folgenden Text, der einen Einkauf beschreibt. Extrahiere die folgenden Informationen und gib sie als ein einziges JSON-Objekt zurück.
        Das JSON-Objekt sollte KEINE Markdown-Formatierung wie ```json am Anfang oder ``` am Ende enthalten.
        Das heutige Datum ist $currentDate. Wenn kein Datum genannt wird, verwende das heutige Datum.

        Struktur des JSON-Objekts:
        {
          "purchaseDate": "YYYY-MM-DD", // Das Datum des Einkaufs.
          "store": "Name des Geschäfts",
          "storeType": "Klassifiziere das Geschäft. Mögliche Werte sind: Supermarkt, Tankstelle, Klamottenladen, Baumarkt, Unbekannt",
          "totalPrice": 123.45, // Der Gesamtbetrag des Einkaufs.
          "positions": [
            {
              "itemName": "Name des Artikels",
              "itemType": "Kategorie des Artikels (z.B. Lebensmittel, Körperpflege, Kleidung, Treibstoff, Elektronik, Dekorativ, Baumarkt, Büro, Rabatt)",
              "quantity": 1.0, // Die Menge als Zahl.
              "unit": "Die Einheit (z.B. Stück, kg, Liter, g)",
              "unitPrice": 1.23, // Der Preis pro Einheit.
              "price": 1.23 // Der Gesamtpreis für diese Position.
            }
          ]
        }

        Wichtige Hinweise:
        - Gib nur das JSON-Objekt als String zurück.
        - Wenn keine oder nur eine Position genannt wird, erstelle trotzdem ein valides JSON mit einer leeren oder einer "positions"-Liste.
        - Leite den 'totalPrice' aus der Summe der Positionen ab, falls keine Gesamtsumme genannt wird. Wenn eine Gesamtsumme genannt wird, verwende diese.
        - Wenn keine Preise genannt werden, setze die Preis-Felder auf 0.0.
        - Wenn keine Mengenangaben gemacht werden, gehe von einer Menge von 1.0 und der Einheit "Stück" aus.
        - Erkenne Rabatte und führe sie als eigene Position mit einem negativen Preis auf. Der 'itemName' sollte "Rabatt" oder ähnlich sein und der 'itemType' "Rabatt".
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val generativeModel = getGenerativeModel()
                val inputContent = content {
                    text(prompt)
                    text(text)
                }
                val response = generativeModel.generateContent(inputContent)
                Log.d("GeminiResponse", "Raw JSON Response from Text: ${response.text}")
                response.text
            } catch (e: Exception) {
                Log.e("GeminiResponse", "Error generating content from text", e)
                null
            }
        }
    }

    suspend fun getPurchaseFromImage(image: Bitmap): String? {
        val prompt = """
            Analysiere den folgenden Kassenbon. Extrahiere die folgenden Informationen und gib sie als ein einziges JSON-Objekt zurück.
            Das JSON-Objekt sollte KEINE Markdown-Formatierung wie ```json am Anfang oder ``` am Ende enthalten.

            Struktur des JSON-Objekts:
            {
              "purchaseDate": "YYYY-MM-DD", // Das Datum des Einkaufs.
              "store": "Name des Geschäfts",
              "storeType": "Klassifiziere das Geschäft. Mögliche Werte sind: Supermarkt, Tankstelle, Klamottenladen, Baumarkt, Unbekannt",
              "totalPrice": 123.45, // Der Gesamtbetrag des Einkaufs.
              "positions": [
                {
                  "itemName": "Name des Artikels",
                  "itemType": "Kategorie des Artikels (z.B. Lebensmittel, Körperpflege, Kleidung, Treibstoff, Elektronik, Dekorativ, Baumarkt, Büro, Rabatt)",
                  "quantity": 1.0, // Die Menge als Zahl.
                  "unit": "Die Einheit (z.B. Stück, kg, Liter, g)",
                  "unitPrice": 1.23, // Der Preis pro Einheit.
                  "price": 1.23 // Der Gesamtpreis für diese Position.
                }
              ]
            }

            Wichtige Hinweise:
            - Gib nur das JSON-Objekt als String zurück.
            - Fasse Artikel, die zusammengehören, aber auf dem Bon getrennt sind (z.B. Name und Preis in verschiedenen Zeilen), korrekt zu einer Position zusammen.
            - Erkenne Rabatte und führe sie als eigene Position mit einem negativen Preis auf. Der 'itemName' sollte "Rabatt" oder ähnlich sein und der 'itemType' "Rabatt".
            - Ignoriere irrelevante Informationen wie Steuern oder Treuepunkte, es sei denn, sie sind direkt im Gesamtpreis enthalten.
            - Der 'totalPrice' im Hauptobjekt muss die Summe aller 'price'-Werte in den 'positions' sein.
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val generativeModel = getGenerativeModel()
                val scaledImage = scaleBitmap(image)
                val inputContent = content {
                    image(scaledImage)
                    text(prompt)
                }
                val response = generativeModel.generateContent(inputContent)
                //Log.d("GeminiResponse", "Raw JSON Response: ${response.text}")
                // 2. Extrahiere die Metadaten zur Token-Nutzung
                val usageMetadata = response.usageMetadata
                if (usageMetadata != null) {
                    val promptTokens = usageMetadata.promptTokenCount
                    val responseTokens = usageMetadata.candidatesTokenCount
                    val totalTokens = usageMetadata.totalTokenCount
                    Log.d("GeminiTokens", "Bitmap Size: ${scaledImage.width}x${scaledImage.height}")
                    Log.d("GeminiTokens", "Prompt: $promptTokens")
                    Log.d("GeminiTokens", "Request Tokens: $promptTokens")
                    Log.d("GeminiTokens", "Response Tokens: $responseTokens")
                    Log.d("GeminiTokens", "Total Tokens used: $totalTokens")
                } else {
                    Log.w("GeminiTokens", "Usage metadata not available in the response.")
                }
                response.text
            } catch (e: Exception) {
                Log.e("GeminiResponse", "Error generating content", e)
                null
            }
        }
    }
}
