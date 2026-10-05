package com.example.engine

import androidx.compose.ui.graphics.Color

/** Local palette used by both cottage rooms. Pixel art keeps the same shapes and scale. */
enum class RoomTheme(
    val title: String,
    val description: String,
    val wall: Color,
    val panel: Color,
    val rug: Color,
    val rugTrim: Color,
    val bedding: Color,
    val beddingAccent: Color,
    val light: Color,
    val mug: Color,
    val mugAccent: Color
) {
    WARM_AUTUMN_COTTAGE(
        "Warm Autumn Cottage", "Maple, honey, and cinnamon",
        Color(0xFFF6EDE2), Color(0xFFEAD9C7), Color(0xFFE07A5F), Color(0xFF81B29A),
        Color(0xFFD4A373), Color(0xFFFAEDCD), Color(0xFFFFD166), Color(0xFF457B9D), Color(0xFFFFB5C2)
    ),
    MIDNIGHT_PASTEL(
        "Midnight Pastel", "Moonlit lilac and soft blue",
        Color(0xFFB9B3D2), Color(0xFF8983AA), Color(0xFF65558F), Color(0xFFE7C6FF),
        Color(0xFF8884B8), Color(0xFFD9D6FF), Color(0xFFB6A8FF), Color(0xFF7D91D6), Color(0xFFE6A9C7)
    ),
    FOREST_GREEN(
        "Forest Green", "Fern, moss, and cream",
        Color(0xFFDCE8D4), Color(0xFFB7C9A8), Color(0xFF486B54), Color(0xFFB7C99B),
        Color(0xFF658B66), Color(0xFFD9E8C8), Color(0xFFC7DBA3), Color(0xFF52796F), Color(0xFFD9B99B)
    ),
    STRAWBERRY_MILK(
        "Strawberry Milk", "Blush pink and vanilla",
        Color(0xFFFFEAF0), Color(0xFFF5C6D3), Color(0xFFE786A1), Color(0xFFFFF1D6),
        Color(0xFFF3A6BD), Color(0xFFFFE7EE), Color(0xFFFFB7C9), Color(0xFFD96C91), Color(0xFFFFDCE7)
    ),
    /** A reward: a hundred days together. */
    STARRY_NIGHT(
        "Starry Night", "Deep blue and starlight gold",
        Color(0xFF3A4570), Color(0xFF2C355A), Color(0xFF26315A), Color(0xFFE9C46A),
        Color(0xFF4A5A8C), Color(0xFFFFE8A3), Color(0xFFFFE08A), Color(0xFF2C355A), Color(0xFFE9C46A)
    )
}
