package com.example.engine

import com.example.resources.Res
import com.example.resources.allPluralStringResources
import com.example.resources.allStringArrayResources
import com.example.resources.allStringResources
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray

/**
 * Translatable text for code that runs outside Compose: the scene engine, click handlers,
 * notifications. Compose resources can only be read in a coroutine there, so every shared string
 * is loaded once at start-up ([load]) and then looked up instantly, with names and other values
 * passed as format arguments. Screens use stringResource() as usual.
 */
object GameText {
    private var texts: Map<StringResource, String> = emptyMap()

    /** Each plural's text for quantity 0, 1 and 2 (2 standing for "many"). */
    private var plurals: Map<PluralStringResource, List<String>> = emptyMap()

    /** String arrays (the Tiny Games' question decks). */
    private var arrays: Map<StringArrayResource, List<String>> = emptyMap()

    /** Loads every shared string for the current language. Call at start-up and after a language change. */
    suspend fun load() {
        texts = Res.allStringResources.values.associateWith { getString(it) }
        plurals = Res.allPluralStringResources.values.associateWith { res -> (0..2).map { getPluralString(res, it) } }
        arrays = Res.allStringArrayResources.values.associateWith { getStringArray(it) }
    }

    /** The items of the string array [res]. Empty before [load]. */
    fun array(res: StringArrayResource): List<String> = arrays[res].orEmpty()

    val isLoaded: Boolean get() = texts.isNotEmpty()

    /** The text of [res], formatted with [args] when there are any. Empty before [load]. */
    fun get(res: StringResource, vararg args: Any?): String {
        val text = texts[res] ?: return ""
        return if (args.isEmpty()) text else format(text, args)
    }

    /**
     * The plural [res] for [quantity], formatted with [args]. Picks the one/other form the way
     * English and Hindi do (languages with more plural forms would need the full rules).
     */
    fun plural(res: PluralStringResource, quantity: Int, vararg args: Any?): String {
        val forms = plurals[res] ?: return ""
        return format(forms[quantity.coerceIn(0, 2)], args)
    }

    private val placeholder = Regex("""%(?:(\d+)\$)?([sd%])""")

    /** Fills Android-style placeholders: %1$s and %2$d by position, %s and %d in order, %% as %. */
    fun format(template: String, args: Array<out Any?>): String {
        var next = 0
        return placeholder.replace(template) { match ->
            if (match.groupValues[2] == "%") return@replace "%"
            val index = match.groupValues[1].toIntOrNull()?.minus(1) ?: next++
            if (index in args.indices) args[index].toString() else match.value
        }
    }
}
