package com.example.recommendation

data class MoodTag(
    val tag: String,
    val confidence: Float
)

object MoodExtractionStrategy {

    // Expanded keywords targeting Hindi/Bollywood titles and popular artists
    private val devotionalKeywords = listOf("bhajan", "devotional", "aarti", "mantra", "chalisa", "stotra", "sai", "shiv", "mata", "ganesh", "krishna", "ram", "hanuman", "baba", "darshan", "kirtan", "darbar", "shyam", "hari", "anuradha paudwal", "anup jalota", "lakhbir singh lakkha", "narendra chanchal", "gulshan kumar", "bhakti", "satsang", "katha", "amritwani", "stuti")
    private val partyKeywords = listOf("party", "dance", "remix", "dj ", "item", "club", "honey singh", "badshah", "guru randhawa", "mika singh", "neha kakkar", "tony kakkar", "punjabi", "naach", "beat", "bass", "chatpata", "bhangra")
    private val sadKeywords = listOf("sad", "broken", "judai", "dard", "cry", "lonely", "bewafa", "tanhai", "gham", "ansu", "bheegi", "b praak", "jubin nautiyal", "rahat fateh ali", "bhula", "rote")
    private val romanticKeywords = listOf("love", "romantic", "ishq", "pyaar", "pyar", "mohabbat", "dil", "aashiqui", "teri", "meri", "arijit singh", "atif aslam", "armaan malik", "shreya ghoshal", "darshan raval", "sanam", "zindagi", "sanam", "tum", "tujh")
    private val happyKeywords = listOf("happy", "smile", "joy", "khushi", "masti", "fun", "celebration", "zindabad", "shaadi")
    private val lullabyKeywords = listOf("lofi", "lo-fi", "chill", "relax", "sleep", "acoustic", "unplugged")
    private val energeticKeywords = listOf("workout", "gym", "rock", "metal", "bass", "anthem", "josh", "ziddi")

    fun extractMoods(title: String, album: String, artist: String): List<MoodTag> {
        val searchString = "${title.lowercase()} ${album.lowercase()} ${artist.lowercase()}"
        val moods = mutableListOf<MoodTag>()

        // Check each category
        if (devotionalKeywords.any { searchString.contains(it) }) {
            moods.add(MoodTag("devotional", 0.9f))
        }
        
        if (partyKeywords.any { searchString.contains(it) }) {
            moods.add(MoodTag("party", 0.85f))
        }

        if (romanticKeywords.any { searchString.contains(it) }) {
            moods.add(MoodTag("romantic", 0.85f))
        }
        
        if (sadKeywords.any { searchString.contains(it) }) {
            moods.add(MoodTag("sad", 0.8f))
        }

        if (happyKeywords.any { searchString.contains(it) }) {
            moods.add(MoodTag("happy", 0.75f))
        }
        
        if (lullabyKeywords.any { searchString.contains(it) }) {
            moods.add(MoodTag("lullaby", 0.7f))
        }
        
        if (energeticKeywords.any { searchString.contains(it) }) {
            moods.add(MoodTag("energetic", 0.7f))
        }

        // If no mood detected, use safe fallbacks
        if (moods.isEmpty()) {
            moods.add(MoodTag("neutral", 0.5f))
            moods.add(MoodTag("unknown", 0.1f))
            
            // We can also assume typical Bollywood if no other flag is hit, but we keep it neutral
            // to avoid forcing everything into Bollywood if they are playing English pop.
        }

        // Sort by confidence descending
        return moods.sortedByDescending { it.confidence }
    }

    /**
     * Gets the primary mood tag for legacy compatibility.
     */
    fun getPrimaryMood(title: String, album: String, artist: String): String {
        val moods = extractMoods(title, album, artist)
        return moods.firstOrNull()?.tag ?: "neutral"
    }

    /**
     * Maps a raw mood string into a macro compatibility group.
     */
    fun getMoodGroup(mood: String): String {
        return when (mood.lowercase()) {
            "party", "dance", "remix", "hip-hop", "workout", "edm", "rock", "energetic", "happy", "masti" -> "party"
            "romantic", "love", "ishq" -> "romantic"
            "sad", "melancholy", "sleep", "lullaby", "relax", "chill" -> "sad"
            "devotional", "bhajan" -> "devotional"
            else -> "neutral"
        }
    }
}
