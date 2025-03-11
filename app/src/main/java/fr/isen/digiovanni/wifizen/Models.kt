package fr.isen.digiovanni.wifizen

// Data class pour un commentaire
data class Comment(
    var uid: String = "",
    var pseudo: String = "",
    var text: String = "",
    var timestamp: Long = 0L
)

// Data class pour un post
data class Post(
    var uid: String = "",
    var pseudo: String = "",
    var profileImageUrl: String = "",
    var text: String = "",
    var imageUrl: String = "",
    var timestamp: Long = 0L,
    var likes: Map<String, Boolean> = emptyMap(),
    var comments: Map<String, Comment> = emptyMap()
)