package com.example.data

/*
 * Plan 09, I: the friends in the world (Leo at the cafe, Grandpa Bao and Pip on the pier) keep to
 * a little routine by the clock and have favourite things from the keepsake box. No friendship
 * meters: the twist is that the first favourite gift opens each one's small secret. Bao's old
 * letter, Leo's sketchbook, and where Pip hides his treasure.
 */
enum class Friend {
    LEO, BAO, PIP;

    /** The keepsakes they love, from the box. */
    val favourites: List<String>
        get() = when (this) {
            BAO -> listOf("dish:tea", "dish:herb_tea", "catch:OLD_BOOT")
            LEO -> listOf("dish:pumpkin_pie", "dish:apple_crumble")
            PIP -> listOf("catch:MINNOW", "catch:CARP", "catch:RAIN_TROUT", "catch:ICE_COD", "catch:BLOSSOM_KOI")
        }

    /** The first of their favourites in [keepsakes], or null. */
    fun favouriteIn(keepsakes: Map<String, Int>): String? = favourites.firstOrNull { (keepsakes[it] ?: 0) > 0 }

    /** The keepsake key their secret goes in the collection book under. */
    val secretKey: String get() = "secret:$name"

    companion object {
        /** The cafe's hours: open from 7 till 21. */
        fun cafeOpen(hour: Int): Boolean = hour in 7..20

        /** Bao's tea, at four. */
        fun baoTeaTime(hour: Int): Boolean = hour == 16

        /** Pip is at his most daring around noon. */
        fun pipDaring(hour: Int): Boolean = hour in 11..13
    }
}

/** Which secrets are open, and the day each friend was last given something, in `tiny_us_prefs`. */
class FriendsStore(private val storage: KeyValueStorage) {
    fun secretOpen(friend: Friend): Boolean = friend.name in storage.getStringSet(KEY_SECRETS)

    /** Opens [friend]'s secret; false if it was open already. */
    fun openSecret(friend: Friend): Boolean {
        if (secretOpen(friend)) return false
        storage.putStringSet(KEY_SECRETS, storage.getStringSet(KEY_SECRETS) + friend.name)
        return true
    }

    /** One gift a day each, so it's a kindness, not a routine. */
    fun giftedOn(friend: Friend, day: String): Boolean = storage.getString(KEY_LAST_GIFT + friend.name, null) == day

    fun noteGift(friend: Friend, day: String) = storage.putString(KEY_LAST_GIFT + friend.name, day)

    private companion object {
        const val KEY_SECRETS = "friends_secrets"
        const val KEY_LAST_GIFT = "friends_last_gift_"
    }
}
