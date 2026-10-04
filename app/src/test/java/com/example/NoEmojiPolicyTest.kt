package com.example

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tiny Us has a zero-emoji policy: UI text, scene messages, notifications, comments, docs and tests.
 * Where a visual accent is wanted the app uses vector icons or pixel art instead.
 *
 * This scans the whole repository (Android, shared, iOS, docs) for emoji code points and for emoji
 * written as escape sequences, so a new one fails the build instead of slipping into a release.
 * Characters are identified by code point; this file must never contain the characters themselves.
 */
class NoEmojiPolicyTest {

    // Unicode Extended_Pictographic (emoji-data.txt, Unicode 15.1) plus emoji components:
    // VS15/VS16, ZWJ, combining keycap, skin tones, regional indicators, tag characters.
    private val emojiRanges: List<IntRange> = listOf(
        0x00A9..0x00A9, 0x00AE..0x00AE, 0x203C..0x203C, 0x2049..0x2049, 0x2122..0x2122,
        0x2139..0x2139, 0x2194..0x2199, 0x21A9..0x21AA, 0x231A..0x231B, 0x2328..0x2328,
        0x2388..0x2388, 0x23CF..0x23CF, 0x23E9..0x23F3, 0x23F8..0x23FA, 0x24C2..0x24C2,
        0x25AA..0x25AB, 0x25B6..0x25B6, 0x25C0..0x25C0, 0x25FB..0x25FE, 0x2600..0x2605,
        0x2607..0x2612, 0x2614..0x2685, 0x2690..0x2705, 0x2708..0x2712, 0x2714..0x2714,
        0x2716..0x2716, 0x271D..0x271D, 0x2721..0x2721, 0x2728..0x2728, 0x2733..0x2734,
        0x2744..0x2744, 0x2747..0x2747, 0x274C..0x274C, 0x274E..0x274E, 0x2753..0x2755,
        0x2757..0x2757, 0x2763..0x2767, 0x2795..0x2797, 0x27A1..0x27A1, 0x27B0..0x27B0,
        0x27BF..0x27BF, 0x2934..0x2935, 0x2B05..0x2B07, 0x2B1B..0x2B1C, 0x2B50..0x2B50,
        0x2B55..0x2B55, 0x3030..0x3030, 0x303D..0x303D, 0x3297..0x3297, 0x3299..0x3299,
        0x1F000..0x1F0FF, 0x1F10D..0x1F10F, 0x1F12F..0x1F12F, 0x1F16C..0x1F171,
        0x1F17E..0x1F17F, 0x1F18E..0x1F18E, 0x1F191..0x1F19A, 0x1F1AD..0x1F1FF,
        0x1F201..0x1F20F, 0x1F21A..0x1F21A, 0x1F22F..0x1F22F, 0x1F232..0x1F23A,
        0x1F23C..0x1F23F, 0x1F249..0x1F3FA, 0x1F3FB..0x1F3FF, 0x1F400..0x1F53D,
        0x1F546..0x1F64F, 0x1F680..0x1F6FF, 0x1F774..0x1F77F, 0x1F7D5..0x1F7FF,
        0x1F80C..0x1F80F, 0x1F848..0x1F84F, 0x1F85A..0x1F85F, 0x1F888..0x1F88F,
        0x1F8AE..0x1F8FF, 0x1F90C..0x1F93A, 0x1F93C..0x1F945, 0x1F947..0x1FAFF,
        0x1FC00..0x1FFFD,
        0xFE0E..0xFE0F, 0x200D..0x200D, 0x20E3..0x20E3, 0xE0020..0xE007F
    )

    // Pictographs that are not in Extended_Pictographic but render as emoji-like symbols.
    private val extraSymbols = setOf(0x2661, 0x2713, 0x2726)

    // Emoji smuggled in as escapes: surrogate pairs, Swift/Python code points, XML entities.
    private val escapePatterns = listOf(
        Regex("""\\u[dD]83[cCdDeE]\\u[dD][c-fC-F][0-9a-fA-F]{2}"""),
        Regex("""\\u\{1[fF][0-9a-fA-F]{3}\}"""),
        Regex("""\\U0001[fF][0-9a-fA-F]{3}"""),
        Regex("""&#[xX]1[fF][0-9a-fA-F]{3};""")
    )

    private val scannedExtensions = setOf(
        "kt", "kts", "xml", "md", "swift", "plist", "json", "yml", "yaml", "pro", "properties",
        "txt", "toml", "html", "strings", "gradle"
    )
    private val skippedDirs = setOf("build", ".gradle", ".gradle_tmp", ".kotlin", ".idea", ".git", ".claude", "xcuserdata", "DerivedData")

    private fun isEmoji(cp: Int) = cp in extraSymbols || emojiRanges.any { cp in it }

    private fun repoRoot(): File {
        // Unit tests run with the module directory as the working directory.
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return requireNotNull(dir) { "Could not locate the repository root" }
    }

    private fun sourceFiles(root: File): Sequence<File> =
        listOf("app/src", "shared/src", "docs", "iosApp", "README.md", ".github")
            .map { File(root, it) }
            .filter { it.exists() }
            .asSequence()
            .flatMap { start ->
                start.walkTopDown()
                    .onEnter { it.name !in skippedDirs }
                    .filter { it.isFile && it.extension.lowercase() in scannedExtensions }
            }

    @Test
    fun repositoryContainsNoEmoji() {
        val root = repoRoot()
        val hits = mutableListOf<String>()
        var scanned = 0
        for (file in sourceFiles(root)) {
            scanned++
            val rel = file.relativeTo(root).invariantSeparatorsPath
            file.readLines(Charsets.UTF_8).forEachIndexed { index, line ->
                val bad = line.codePoints().toArray().filter { isEmoji(it) }
                if (bad.isNotEmpty()) {
                    hits += "$rel:${index + 1} " + bad.joinToString(" ") { "U+%04X".format(it) }
                }
                escapePatterns.forEach { p ->
                    p.find(line)?.let { hits += "$rel:${index + 1} escaped ${it.value}" }
                }
            }
        }
        assertTrue("Scanned only $scanned files; the repo scan is not finding sources", scanned > 50)
        assertTrue(
            "Emoji found (zero-emoji policy). Replace with plain text or a vector/pixel-art icon:\n" +
                hits.joinToString("\n"),
            hits.isEmpty()
        )
    }
}
