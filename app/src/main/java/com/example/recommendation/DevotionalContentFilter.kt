package com.example.recommendation

/**
 * Hard content-type exclusion filter that removes devotional/bhajan tracks
 * from the candidate pool BEFORE scoring. Uses whole-word matching to avoid
 * false positives (e.g. "Krishnamurthy" won't match "Krishna").
 */
object DevotionalContentFilter {

    // Deity names/epithets
    private val deityKeywords = setOf(
        "ram", "shri ram", "krishna", "gopal", "madhav", "kanha",
        "shiva", "mahadev", "bholenath", "shankara",
        "hanuman", "bajrangbali",
        "durga", "ambe", "jagdamba",
        "lakshmi", "vishnu",
        "ganesh", "ganapati",
        // Marathi
        "vitthal", "vithoba", "tukaram", "mauli", "gajanan",
        // Punjabi
        "waheguru", "nanak", "khalsa", "satnam",
        // Rajasthani
        "ramdevji", "pabuji", "karni mata", "khatushyam", "meera",
        // Bengali
        "loknath", "shyama",
        // South Indian
        "ayyappa", "murugan", "venkateshwara", "govinda", "tirupati"
    )

    // Devotional form words
    private val devotionalFormKeywords = setOf(
        "bhajan", "kirtan", "aarti", "stuti", "vandana", "prarthana",
        "darshan", "chalisa", "ashtak",
        // Marathi
        "abhang", "bharud", "ovi", "gondhal",
        // Punjabi
        "gurbani", "shabad", "ardas", "path", "japji", "sukhmani", "simran",
        // Rajasthani
        "lok bhajan",
        // Bengali
        "shyama sangeet", "agamani", "vijaya",
        // South Indian
        "suprabhatam", "stotram", "ashtakam", "sahasranamam", "devaranama", "thiruppavai", "bhajanai", "keerthana"
    )

    // Context words
    private val contextKeywords = setOf(
        "mandir", "satsang", "janmashtami", "navratri", "ram navami", "pandharpur", "sabarimala"
    )

    // Combined blocklist
    private val allBlockedKeywords: Set<String> = deityKeywords + devotionalFormKeywords + contextKeywords

    // Mood/genre tags that are automatic exclusions
    private val blockedMoodTags = setOf(
        "devotional", "bhajan", "bhakti", "spiritual", "chant", "mantra", "prayer", "worship", "god", "divine"
    )

    /**
     * Returns true if the track should be EXCLUDED (i.e. is devotional content).
     * Checks moodGenreTag and explicit apiTags first (fast path), then does 
     * whole-word matching against title, album, and artist.
     */
    fun shouldExclude(
        title: String,
        album: String,
        artist: String,
        moodGenreTag: String,
        apiTags: List<String> = emptyList()
    ): Boolean {
        // Fast path: mood/genre tag or API tags exclusion
        if (moodGenreTag.lowercase() in blockedMoodTags) return true
        
        for (tag in apiTags) {
            val lowerTag = tag.lowercase()
            if (lowerTag in blockedMoodTags || allBlockedKeywords.any { lowerTag.contains(it) }) {
                return true
            }
        }

        // Whole-word matching against title, album, artist
        val fieldsToCheck = listOf(title, album, artist)
        for (field in fieldsToCheck) {
            if (containsBlockedWord(field)) return true
        }

        return false
    }

    /**
     * Overload that accepts TrackFeatures directly.
     */
    fun shouldExclude(track: TrackFeatures): Boolean {
        return shouldExclude(
            title = track.title,
            album = track.album,
            artist = track.artist,
            moodGenreTag = track.moodGenreTag
        )
    }

    /**
     * Performs whole-word matching: tokenizes the input string into words and
     * checks if any word (or consecutive pair of words for multi-word keywords
     * like "shri ram") matches a blocked keyword.
     *
     * This prevents false positives like "Krishnamurthy" matching "Krishna".
     */
    private fun containsBlockedWord(input: String): Boolean {
        if (input.isBlank()) return false

        val lower = input.lowercase()
        // Tokenize: split on non-alphanumeric characters
        val words = lower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

        // Check single words
        for (word in words) {
            if (word in allBlockedKeywords) return true
        }

        // Check consecutive word pairs for multi-word keywords (e.g. "shri ram", "ram navami")
        for (i in 0 until words.size - 1) {
            val pair = "${words[i]} ${words[i + 1]}"
            if (pair in allBlockedKeywords) return true
        }

        return false
    }
}
