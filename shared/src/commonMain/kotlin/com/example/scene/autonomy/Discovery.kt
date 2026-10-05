package com.example.scene.autonomy

/** Small things that turn up in the world now and then for the couple to notice. */
enum class DiscoveryKind { WILDFLOWER, RED_LEAF, LOVE_NOTE, MOCHI_TOY, SEASHELL, STAR_PEBBLE }

/** The one discoverable item currently in the scene (there is never more than one). */
class Discovery {
    var kind: DiscoveryKind = DiscoveryKind.WILDFLOWER
    var x: Float = 0f
    var y: Float = 0f
    var active: Boolean = false
    /** A character has noticed it and is on their way. */
    var claimed: Boolean = false
    var age: Float = 0f

    fun place(kind: DiscoveryKind, x: Float, y: Float) {
        this.kind = kind
        this.x = x
        this.y = y
        active = true
        claimed = false
        age = 0f
    }

    fun clear() {
        active = false
        claimed = false
        age = 0f
    }
}
