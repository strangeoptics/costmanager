package com.example.costmanager.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.costmanager.data.SettingsManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                  "itemType": "Kategorie des Artikels (z.B. Lebensmittel, Kleidung, Treibstoff, Elektronik)",
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
            - Ignoriere irrelevante Informationen wie Rabatte, Steuern oder Treuepunkte, es sei denn, sie sind direkt im Gesamtpreis enthalten.
            - Der 'totalPrice' im Hauptobjekt muss die Summe aller 'price'-Werte in den 'positions' sein.
        """.trimIndent()

        return withContext(Dispatchers.IO) {
            try {
                val generativeModel = getGenerativeModel()
                val inputContent = content {
                    image(image)
                    text(prompt)
                }
                val response = generativeModel.generateContent(inputContent)
                Log.d("GeminiResponse", "Raw JSON Response: ${response.text}")
                response.text
            } catch (e: Exception) {
                Log.e("GeminiResponse", "Error generating content", e)
                null
            }
        }
    }
}
