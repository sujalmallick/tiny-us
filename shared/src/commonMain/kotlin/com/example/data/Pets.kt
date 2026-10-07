package com.example.data

/*
 * Plan 10, E: other pets. Mochi is theirs from the start; the others are met, once each, in a
 * little moment somewhere in the world (the twist: they aren't bought or earned). After that the
 * couple can choose who lives with them, and Mochi goes on a sleepover at Grandpa Bao's.
 */

/** The pets, each with its default name and where it's met. */
enum class PetKind(val defaultName: String) {
    CAT("Mochi"),
    PUPPY("Boba"),
    BUNNY("Clover"),
    FOX("Ember"),
    HEDGEHOG("Hazel"),
    DUCK("Puddle"),
    OWL("Olive");

    companion object {
        fun from(name: String?): PetKind = entries.firstOrNull { it.name == name } ?: CAT
    }
}

/** Who they've met, and who lives with them, in `tiny_us_prefs` (so backups carry it). */
class PetStore(private val storage: KeyValueStorage) {
    /** The pets they've met; Mochi always. */
    fun met(): Set<PetKind> =
        storage.getStringSet(KEY_MET).mapNotNull { n -> PetKind.entries.firstOrNull { it.name == n } }.toSet() + PetKind.CAT

    fun hasMet(kind: PetKind): Boolean = kind in met()

    /** Meets [kind]; false if they already had. */
    fun meet(kind: PetKind): Boolean {
        if (hasMet(kind)) return false
        storage.putStringSet(KEY_MET, storage.getStringSet(KEY_MET) + kind.name)
        return true
    }

    /** Who lives with them now. */
    var chosen: PetKind
        get() = PetKind.from(storage.getString(KEY_CHOSEN, null)).takeIf { hasMet(it) } ?: PetKind.CAT
        set(value) {
            if (hasMet(value)) storage.putString(KEY_CHOSEN, value.name)
        }

    /** How many different days they've petted Boba at the cafe (plan 10, E: he's met on the third). */
    fun pupDays(): Set<String> = storage.getStringSet(KEY_PUP_DAYS)

    fun notePupPetted(day: String) = storage.putStringSet(KEY_PUP_DAYS, pupDays() + day)

    private companion object {
        const val KEY_MET = "pets_met"
        const val KEY_CHOSEN = "pet_chosen"
        const val KEY_PUP_DAYS = "pet_pup_days"
    }
}
