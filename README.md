# Cost Manager

Cost Manager ist eine moderne Android-App zur Verwaltung deiner persönlichen Finanzen und Einkäufe. Die App hilft dir, den Überblick über deine Ausgaben zu behalten, indem sie eine einfache Erfassung von Kassenbons ermöglicht – sowohl manuell als auch automatisch durch KI-gestützte Bilderkennung.

## Funktionen

*   **Intelligente Belegerfassung**: Fotografiere deine Kassenbons oder lade Bilder aus der Galerie hoch. Die App nutzt Google Generative AI (Gemini), um Informationen wie Laden, Datum und Gesamtpreis automatisch zu extrahieren.
*   **Monatliche Übersicht**: Deine Ausgaben werden übersichtlich nach Monaten gruppiert dargestellt, inklusive monatlicher Gesamtsummen.
*   **Detaillierte Einsicht**: Speichere nicht nur den Gesamtbetrag, sondern auch einzelne Positionen eines Einkaufs.
*   **Datenverwaltung**:
    *   **Export**: Exportiere deine Daten als JSON-Datei, um sie zu sichern oder zu teilen.
    *   **Import**: Stelle deine Daten aus einem Backup einfach wieder her.
*   **Intuitive Bedienung**: Lösche Einträge einfach durch Wischen (Swipe-to-Dismiss) und mache Aktionen bei Bedarf rückgängig.
*   **Modernes Design**: Entwickelt mit Material Design 3 Komponenten.

## Technologien

Das Projekt basiert auf modernen Android-Entwicklungspraktiken:

*   **Sprache**: [Kotlin](https://kotlinlang.org/)
*   **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **Architektur**: MVVM (Model-View-ViewModel)
*   **Datenbank**: [Room](https://developer.android.com/training/data-storage/room) (SQLite)
*   **KI / ML**: [Google Generative AI SDK](https://ai.google.dev/) (Gemini) für die Beleganalyse
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Navigation**: Navigation Compose (implizit durch Drawer und Intent-Nutzung)

## Voraussetzungen

*   Android Studio Ladybug oder neuer (empfohlen)
*   JDK 17 oder neuer

## Einrichtung

1.  **Repository klonen**:
    ```bash
    git clone https://github.com/dein-username/CostManager.git
    ```
2.  **Projekt öffnen**: Starte Android Studio und öffne den Projektordner.
3.  **API Key konfigurieren**:
    Da die App Google Generative AI nutzt, benötigst du einen API Key.
    *   Erstelle einen API Key in [Google AI Studio](https://aistudio.google.com/).
    *   Füge den Key in deine `local.properties` Datei im Projektstammverzeichnis ein (oder dort, wo er im Code abgerufen wird, z.B. `BuildConfig`):
        ```properties
        gemini.api.key=DEIN_API_KEY
        ```
4.  **Ausführen**: Baue das Projekt und starte es auf einem Emulator oder physischen Gerät.

## Lizenz

[Hier Lizenz einfügen, z.B. MIT License]
