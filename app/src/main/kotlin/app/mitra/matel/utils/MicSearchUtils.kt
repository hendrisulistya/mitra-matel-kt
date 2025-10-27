package app.mitra.matel.utils

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Utility class for handling microphone search functionality
 * Includes license plate cleaning, phonetic mapping, and speech recognition configuration
 */
object MicSearchUtils {
    
    /**
     * Indonesian license plate prefixes by region
     */
    private val validPrefixes = setOf(
        // Jakarta
        "B",
        // West Java
        "D", "E", "F", "T", "Z",
        // Central Java
        "G", "H", "K", "R", "AA", "AD",
        // East Java
        "L", "M", "N", "S", "W", "AE", "AG", "P",
        // North Sumatra
        "BB", "BK",
        // West Sumatra
        "BA",
        // Riau
        "BM",
        // South Sumatra
        "BG",
        // Lampung
        "BE",
        // Bengkulu
        "BD",
        // Jambi
        "BH",
        // Bangka Belitung
        "BN",
        // Riau Islands
        "BP",
        // Banten
        "A",
        // Yogyakarta
        "AB",
        // Bali
        "DK",
        // West Nusa Tenggara
        "DR", "EA",
        // East Nusa Tenggara
        "DH", "EB",
        // West Kalimantan
        "KB",
        // Central Kalimantan
        "KH",
        // South Kalimantan
        "DA",
        // East Kalimantan
        "KT",
        // North Kalimantan
        "KU",
        // North Sulawesi
        "DB", "DL",
        // Central Sulawesi
        "DN",
        // South Sulawesi
        "DD", "DP", "DW",
        // Southeast Sulawesi
        "DT",
        // Gorontalo
        "DM",
        // West Sulawesi
        "DC",
        // Maluku
        "DE",
        // North Maluku
        "DG",
        // Papua
        "PA", "PB",
        // West Papua
        "PK"
    )
    
    /**
     * Phonetic mapping for common speech recognition errors
     */
    private val phoneticMap = mapOf(
        // Common misinterpretations of license plate prefixes
        "ABI" to "AB",      // "AB" often heard as "Abi"
        "ABE" to "AB",      // Alternative pronunciation
        "ABEH" to "AB",     // Indonesian pronunciation variant
        "ABEY" to "AB",     // Another variant
        "ADEH" to "AD",     // "AD" sometimes heard as "Adeh"
        "ADE" to "AD",      // "AD" sometimes heard as "Ade"
        "ADEY" to "AD",     // "AD" sometimes heard as "Adey"
        "ADI" to "AD",      // "AD" sometimes heard as "Adi" - common Indonesian speech pattern
        "BEH" to "B",       // "B" sometimes heard as "Beh"
        "BEE" to "B",       // "B" sometimes heard as "Bee"
        "BEY" to "B",       // "B" sometimes heard as "Bey"
        "CEH" to "C",       // "C" sometimes heard as "Ceh"
        "SEE" to "C",       // "C" sometimes heard as "See"
        "SEY" to "C",       // "C" sometimes heard as "Sey"
        "DEH" to "D",       // "D" sometimes heard as "Deh"
        "DEE" to "D",       // "D" sometimes heard as "Dee"
        "DEY" to "D",       // "D" sometimes heard as "Dey"
        "EH" to "E",        // "E" sometimes heard as "Eh"
        "EY" to "E",        // "E" sometimes heard as "Ey"
        "EF" to "F",        // "F" sometimes heard as "Ef"
        "GEH" to "G",       // "G" sometimes heard as "Geh"
        "JEE" to "G",       // "G" sometimes heard as "Jee"
        "GEY" to "G",       // "G" sometimes heard as "Gey"
        "AITCH" to "H",     // "H" sometimes heard as "Aitch"
        "HAH" to "H",       // "H" sometimes heard as "Hah"
        "HEH" to "H",       // "H" sometimes heard as "Heh"
        "JAY" to "J",       // "J" sometimes heard as "Jay"
        "JEH" to "J",       // "J" sometimes heard as "Jeh"
        "KAY" to "K",       // "K" sometimes heard as "Kay"
        "KEH" to "K",       // "K" sometimes heard as "Keh"
        "KEI" to "K",       // "K" sometimes heard as "Kei"
        "EL" to "L",        // "L" sometimes heard as "El"
        "LEH" to "L",       // "L" sometimes heard as "Leh"
        "EM" to "M",        // "M" sometimes heard as "Em"
        "MEH" to "M",       // "M" sometimes heard as "Meh"
        "EN" to "N",        // "N" sometimes heard as "En"
        "NEH" to "N",       // "N" sometimes heard as "Neh"
        "PEE" to "P",       // "P" sometimes heard as "Pee"
        "PEH" to "P",       // "P" sometimes heard as "Peh"
        "PEY" to "P",       // "P" sometimes heard as "Pey"
        "AR" to "R",        // "R" sometimes heard as "Ar"
        "REH" to "R",       // "R" sometimes heard as "Reh"
        "ES" to "S",        // "S" sometimes heard as "Es"
        "SEH" to "S",       // "S" sometimes heard as "Seh"
        "TEE" to "T",       // "T" sometimes heard as "Tee"
        "TEH" to "T",       // "T" sometimes heard as "Teh"
        "TEY" to "T",       // "T" sometimes heard as "Tey"
        "WEE" to "W",       // "W" sometimes heard as "Wee"
        "WEH" to "W",       // "W" sometimes heard as "Weh"
        "DOUBLE U" to "W",  // "W" sometimes heard as "Double U"
        "ZED" to "Z",       // "Z" sometimes heard as "Zed"
        "ZEE" to "Z",       // "Z" sometimes heard as "Zee"
        "ZEH" to "Z",       // "Z" sometimes heard as "Zeh"
        
        // Indonesian name-like pronunciations (common misrecognitions)
        "EDI" to "ED",      // "ED" sometimes heard as "Edi"
        "EFI" to "EF",      // "EF" sometimes heard as "Efi"
        "AGI" to "AG",      // "AG" sometimes heard as "Agi"
        "AEI" to "AE",      // "AE" sometimes heard as "Aei"
        "BAI" to "BA",      // "BA" sometimes heard as "Bai"
        "BEI" to "BE",      // "BE" sometimes heard as "Bei"
        "BDI" to "BD",      // "BD" sometimes heard as "Bdi"
        "BGI" to "BG",      // "BG" sometimes heard as "Bgi"
        "BHI" to "BH",      // "BH" sometimes heard as "Bhi"
        "BKI" to "BK",      // "BK" sometimes heard as "Bki"
        "BMI" to "BM",      // "BM" sometimes heard as "Bmi"
        "BNI" to "BN",      // "BN" sometimes heard as "Bni"
        "BPI" to "BP",      // "BP" sometimes heard as "Bpi"
        "DAI" to "DA",      // "DA" sometimes heard as "Dai"
        "DBI" to "DB",      // "DB" sometimes heard as "Dbi"
        "DCI" to "DC",      // "DC" sometimes heard as "Dci"
        "DDI" to "DD",      // "DD" sometimes heard as "Ddi"
        "DEI" to "DE",      // "DE" sometimes heard as "Dei"
        "DGI" to "DG",      // "DG" sometimes heard as "Dgi"
        "DHI" to "DH",      // "DH" sometimes heard as "Dhi"
        "DKI" to "DK",      // "DK" sometimes heard as "Dki"
        "DLI" to "DL",      // "DL" sometimes heard as "Dli"
        "DMI" to "DM",      // "DM" sometimes heard as "Dmi"
        "DNI" to "DN",      // "DN" sometimes heard as "Dni"
        "DPI" to "DP",      // "DP" sometimes heard as "Dpi"
        "DRI" to "DR",      // "DR" sometimes heard as "Dri"
        "DTI" to "DT",      // "DT" sometimes heard as "Dti"
        "DWI" to "DW",      // "DW" sometimes heard as "Dwi" - very common Indonesian name
        "EAI" to "EA",      // "EA" sometimes heard as "Eai"
        "EBI" to "EB",      // "EB" sometimes heard as "Ebi"
        "KBI" to "KB",      // "KB" sometimes heard as "Kbi"
        "KHI" to "KH",      // "KH" sometimes heard as "Khi"
        "KTI" to "KT",      // "KT" sometimes heard as "Kti"
        "KUI" to "KU",      // "KU" sometimes heard as "Kui"
        "PAI" to "PA",      // "PA" sometimes heard as "Pai"
        "PBI" to "PB",      // "PB" sometimes heard as "Pbi"
        "PKI" to "PK",      // "PK" sometimes heard as "Pki"
        
        // Double letter combinations
        "ABEH ABEH" to "AA",
        "ABI ABI" to "AA",
        "BEH BEH" to "BB",
        "BEE BEE" to "BB",
        "DEH DEH" to "DD",
        "DEE DEE" to "DD",
        // Common Indonesian words that might be misheard
        "ALPHA" to "A",
        "BRAVO" to "B",
        "CHARLIE" to "C",
        "DELTA" to "D",
        "ECHO" to "E",
        "FOXTROT" to "F",
        "GOLF" to "G",
        "HOTEL" to "H",
        // Numbers that might be misheard
        "SATU" to "1",
        "DUA" to "2", 
        "TIGA" to "3",
        "EMPAT" to "4",
        "LIMA" to "5",
        "ENAM" to "6",
        "TUJUH" to "7",
        "DELAPAN" to "8",
        "SEMBILAN" to "9",
        "NOL" to "0",
        "KOSONG" to "0"
    )
    
    /**
     * Clean and format nomor polisi from voice input
     * Handles phonetic corrections, space normalization, and multiple parsing strategies
     * 
     * @param input Raw voice input text
     * @return Cleaned license plate string or empty string if invalid
     */
    fun cleanNomorPolisi(input: String): String {
        if (input.isBlank()) return ""
        
        // Apply phonetic corrections first
        var correctedInput = input.uppercase()
        phoneticMap.forEach { (phonetic, correct) ->
            correctedInput = correctedInput.replace(phonetic, correct)
        }
        
        // Remove common speech recognition artifacts and normalize
        val cleaned = correctedInput
            .replace(Regex("[^A-Z0-9\\s]"), "") // Keep letters, numbers, and spaces temporarily
            .replace(Regex("\\s+"), " ") // Normalize multiple spaces to single space
            .trim()
        
        // Try different parsing strategies to handle speech recognition variations
        
        // Support both full and partial formats:
        // Full: AB1234CD (prefix + numbers + suffix)
        // Partial: AB1234 or B2 (prefix + numbers only)
        val fullPlatePattern = Regex("^([A-Z]{1,2})(\\d{1,4})([A-Z]{1,3})$")
        val partialPlatePattern = Regex("^([A-Z]{1,2})(\\d{1,4})$")
        
        // Strategy 0: Apply additional phonetic corrections for spaced input
        val phoneticCorrectedText = cleaned.split("\\s+".toRegex())
            .map { word ->
                // Check if this word matches any phonetic pattern
                // Try exact match first, then try uppercase
                phoneticMap[word] ?: phoneticMap[word.uppercase()] ?: word
            }
            .joinToString("")
        
        // Also try applying phonetic corrections to the entire cleaned string
        var globalCorrected = cleaned
        phoneticMap.forEach { (phonetic, correct) ->
            globalCorrected = globalCorrected.replace("\\b$phonetic\\b".toRegex(RegexOption.IGNORE_CASE), correct)
        }
        
        // Try phonetic corrected text first
        val phoneticFullMatch = fullPlatePattern.find(phoneticCorrectedText)
        if (phoneticFullMatch != null) {
            val (prefix, numbers, suffix) = phoneticFullMatch.destructured
            if (validPrefixes.contains(prefix)) {
                return "${prefix}${numbers}${suffix}"
            }
        }
        
        val phoneticPartialMatch = partialPlatePattern.find(phoneticCorrectedText)
        if (phoneticPartialMatch != null) {
            val (prefix, numbers) = phoneticPartialMatch.destructured
            if (validPrefixes.contains(prefix)) {
                return "${prefix}${numbers}"
            }
        }
        
        // Try globally corrected text (handles word boundary corrections)
        val globalCorrectedNoSpaces = globalCorrected.replace(Regex("\\s+"), "")
        val globalFullMatch = fullPlatePattern.find(globalCorrectedNoSpaces)
        if (globalFullMatch != null) {
            val (prefix, numbers, suffix) = globalFullMatch.destructured
            if (validPrefixes.contains(prefix)) {
                return "${prefix}${numbers}${suffix}"
            }
        }
        
        val globalPartialMatch = partialPlatePattern.find(globalCorrectedNoSpaces)
        if (globalPartialMatch != null) {
            val (prefix, numbers) = globalPartialMatch.destructured
            if (validPrefixes.contains(prefix)) {
                return "${prefix}${numbers}"
            }
        }
        
        // Strategy 1: Remove all spaces and try direct parsing
        val noSpaces = cleaned.replace(Regex("\\s+"), "")
        
        // Try full format first
        val strategy1FullMatch = fullPlatePattern.find(noSpaces)
        if (strategy1FullMatch != null) {
            val (prefix, numbers, suffix) = strategy1FullMatch.destructured
            if (validPrefixes.contains(prefix)) {
                return "${prefix}${numbers}${suffix}"
            }
        }
        
        // Try partial format (prefix + numbers only)
        val strategy1PartialMatch = partialPlatePattern.find(noSpaces)
        if (strategy1PartialMatch != null) {
            val (prefix, numbers) = strategy1PartialMatch.destructured
            if (validPrefixes.contains(prefix)) {
                return "${prefix}${numbers}"
            }
        }
        
        // Strategy 2: Reconstruction from separated parts (e.g., "B 1234 ABC" -> "B1234ABC")
        val parts = cleaned.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (parts.size >= 2) {
            val reconstructed = parts.joinToString("")
            
            // Try full format
            val strategy2FullMatch = fullPlatePattern.find(reconstructed)
            if (strategy2FullMatch != null) {
                val (prefix, numbers, suffix) = strategy2FullMatch.destructured
                if (validPrefixes.contains(prefix)) {
                    return "${prefix}${numbers}${suffix}"
                }
            }
            
            // Try partial format
            val strategy2PartialMatch = partialPlatePattern.find(reconstructed)
            if (strategy2PartialMatch != null) {
                val (prefix, numbers) = strategy2PartialMatch.destructured
                if (validPrefixes.contains(prefix)) {
                    return "${prefix}${numbers}"
                }
            }
        }
        
        // Strategy 3: Manual extraction of letters and numbers
        val letters = noSpaces.filter { it.isLetter() }
        val numbers = noSpaces.filter { it.isDigit() }
        
        if (letters.isNotEmpty() && numbers.isNotEmpty()) {
            // Check if we have letters at the end (full format)
            val letterStart = letters.takeWhile { char -> 
                noSpaces.indexOf(char) < noSpaces.indexOfFirst { it.isDigit() } 
            }
            val letterEnd = letters.drop(letterStart.length)
            
            val manuallyConstructed = if (letterEnd.isNotEmpty()) {
                "$letterStart$numbers$letterEnd"
            } else {
                "$letterStart$numbers"
            }
            
            // Try full format
            val strategy3FullMatch = fullPlatePattern.find(manuallyConstructed)
            if (strategy3FullMatch != null) {
                val (prefix, nums, suffix) = strategy3FullMatch.destructured
                if (validPrefixes.contains(prefix)) {
                    return "${prefix}${nums}${suffix}"
                }
            }
            
            // Try partial format
            val strategy3PartialMatch = partialPlatePattern.find(manuallyConstructed)
            if (strategy3PartialMatch != null) {
                val (prefix, nums) = strategy3PartialMatch.destructured
                if (validPrefixes.contains(prefix)) {
                    return "${prefix}${nums}"
                }
            }
        }
        
        // Strategy 4: Fallback extraction based on presence of letters at end
        if (letters.isNotEmpty() && numbers.isNotEmpty()) {
            val letterStart = letters.takeWhile { char -> 
                noSpaces.indexOf(char) < noSpaces.indexOfFirst { it.isDigit() } 
            }
            val letterEnd = letters.drop(letterStart.length)
            
            val result = if (letterEnd.isNotEmpty()) {
                // Full format: prefix + numbers + suffix
                "$letterStart$numbers$letterEnd"
            } else {
                // Partial format: prefix + numbers only
                "$letterStart$numbers"
            }
            
            // Validate prefix before returning
            if (validPrefixes.contains(letterStart)) {
                return result
            }
        }
        
        // Final safety net: return empty if no valid pattern found
        return ""
    }
    
    /**
     * Creates and configures a speech recognition intent with optimized settings for Indonesian
     */
    fun createSpeechRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "app.mitra.matel")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, android.media.MediaRecorder.AudioSource.MIC)
        }
    }
    
    /**
     * Processes speech recognition results with confidence scoring to find the best match
     * @param results List of recognized speech results
     * @param confidenceScores Array of confidence scores for each result
     * @return Pair of (original text, cleaned text) for the best match, or null if no valid match found
     */
    fun processSpeechResults(
        results: List<String>, 
        confidenceScores: FloatArray? = null
    ): Pair<String, String>? {
        var bestMatch = ""
        var bestCleanedText = ""
        var bestConfidence = 0f
        
        results.forEachIndexed { index, spokenText ->
            val cleanedText = cleanNomorPolisi(spokenText)
            val confidence = confidenceScores?.getOrNull(index) ?: 0f
            
            if (cleanedText.isNotEmpty()) {
                if (bestMatch.isEmpty() || confidence > bestConfidence) {
                    bestMatch = spokenText
                    bestCleanedText = cleanedText
                    bestConfidence = confidence
                }
            }
        }
        
        return if (bestMatch.isNotEmpty()) {
            Pair(bestMatch, bestCleanedText)
        } else {
            null
        }
    }
    
    /**
     * Gets error message for speech recognition errors in Indonesian
     * @param errorCode The error code from SpeechRecognizer
     * @return Localized error message
     */
    fun getSpeechRecognitionErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Error audio. Periksa mikrofon Anda."
            SpeechRecognizer.ERROR_CLIENT -> "Error klien speech recognition."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon diperlukan."
            SpeechRecognizer.ERROR_NETWORK -> "Error jaringan. Periksa koneksi internet."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout jaringan. Coba lagi."
            SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara yang dikenali. Coba ucapkan lebih jelas."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer sedang sibuk. Coba lagi."
            SpeechRecognizer.ERROR_SERVER -> "Error server speech recognition."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tidak ada suara terdeteksi. Coba lagi."
            else -> "Error speech recognition tidak dikenal."
        }
    }
    
    /**
     * Checks if speech recognition is available on the device
     */
    fun isSpeechRecognitionAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Validates if a prefix is a valid Indonesian license plate prefix
     */
    fun isValidPrefix(prefix: String): Boolean {
        return validPrefixes.contains(prefix.uppercase())
    }
    
    /**
     * Gets all valid Indonesian license plate prefixes
     */
    fun getValidPrefixes(): Set<String> {
        return validPrefixes.toSet()
    }
}