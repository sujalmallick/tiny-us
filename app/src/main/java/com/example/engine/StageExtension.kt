package com.example.engine

/**
 * Continues a scene's background above and below its stage (see [WorldCamera]) on a frame of ARGB
 * pixels, so a tall screen shows more sky and more ground instead of an empty band.
 *
 * It reads a band of rows at each edge of the stage:
 * - when the band's rows repeat exactly (a checkerboard floor, planks with joints), that pattern
 *   is tiled on;
 * - otherwise each row's most common colour is used; rows that repeat (plank seams, brick
 *   courses) keep repeating, and the edge row's colour carries on;
 * - above the stage, columns that are one colour all the way through the band (a wall stripe, a
 *   lamp cord, a window mullion) carry on as columns;
 * - below the stage, band colours close to the ground's own are scattered thinly as texture
 *   (grass, gravel), leaving out the colours of props;
 * - above an outdoor night sky (starry), a few stars.
 */
object StageExtension {
    private const val BAND = 16

    fun fill(px: IntArray, w: Int, h: Int, stageTop: Int, stageBottom: Int, starry: Boolean = false) {
        val stageH = stageBottom - stageTop
        if (stageH <= 0) return
        val band = minOf(BAND, stageH / 4).coerceAtLeast(1)
        if (stageTop > 0 && !tilePattern(px, w, stageTop, band, up = true, from = stageTop - 1, to = 0)) {
            fillAbove(px, w, stageTop, band, starry)
        }
        if (stageBottom < h &&
            !tilePattern(px, w, stageBottom - band, band, up = false, from = stageBottom, to = h - 1) &&
            !tileFloor(px, w, stageBottom, minOf(FLOOR_BAND, stageH / 3), h)
        ) {
            fillBelow(px, w, h, stageBottom, band)
        }
    }

    /**
     * When the band's rows repeat exactly with some period, copies that pattern row by row into
     * [from]..[to]. Returns false when there is no such pattern.
     */
    private fun tilePattern(px: IntArray, w: Int, first: Int, band: Int, up: Boolean, from: Int, to: Int): Boolean {
        if (band < 4) return false
        var period = 0
        for (p in 1..band / 2) {
            var ok = true
            loop@ for (i in 0 until band - p) {
                val a = (first + i) * w
                val b = (first + i + p) * w
                for (x in 0 until w) if (px[a + x] != px[b + x]) { ok = false; break@loop }
            }
            if (ok) { period = p; break }
        }
        // One repeated row is a flat band, which the row-colour fill handles better.
        if (period < 2) return false
        if (up) {
            for (y in from downTo to) {
                val d = first - y
                val src = first + (period - d % period) % period
                System.arraycopy(px, src * w, px, y * w, w)
            }
        } else {
            val last = first + band - 1
            for (y in from..to) {
                val d = y - last
                val src = last - period + 1 + (period - 1 + d) % period
                System.arraycopy(px, src * w, px, y * w, w)
            }
        }
        return true
    }

    /** Rows sampled when looking for a floor pattern that has props standing on it. */
    private const val FLOOR_BAND = 72 // finds repeats up to 36 rows (two 16-pixel tile rows and more)

    /**
     * A floor pattern (checkerboard tiles, planks) that repeats with some period even though a
     * few props stand on it: rows match their period-mates for most pixels. The pattern is rebuilt
     * from the band with the floor's own colours winning over the props', then tiled downward.
     */
    private fun tileFloor(px: IntArray, w: Int, bottom: Int, band: Int, h: Int): Boolean {
        if (band < 8) return false
        val first = bottom - band
        // The band's colour counts: the two most common are the floor's own colours.
        val counts = HashMap<Int, Int>()
        for (i in first until bottom) for (x in 0 until w) {
            val c = px[i * w + x]
            counts[c] = (counts[c] ?: 0) + 1
        }
        val floorColours = counts.entries.sortedByDescending { it.value }.take(2).map { it.key }.toSet()
        var period = 0
        for (p in 2..band / 2) {
            var same = 0
            var compared = 0
            for (i in 0 until band - p) {
                val a = (first + i) * w
                val b = (first + i + p) * w
                for (x in 0 until w) {
                    val ca = px[a + x]
                    val cb = px[b + x]
                    // Props (rugs, crates, plants) don't count either way.
                    if (ca !in floorColours || cb !in floorColours) continue
                    compared++
                    if (ca == cb) same++
                }
            }
            if (compared >= band * w / 3 && same >= compared * 0.97f) { period = p; break }
        }
        if (period == 0) return false
        // Template row k continues the pattern k rows after the band's end.
        val template = IntArray(period * w)
        for (k in 0 until period) {
            for (x in 0 until w) {
                // Prefer a floor colour from any period-mate row; fall back to the most common one.
                var best = 0
                var bestN = -1
                var i = bottom - period + k
                while (i >= first) {
                    val c = px[i * w + x]
                    val n = (counts[c] ?: 0) + (if (c in floorColours) 1_000_000 else 0)
                    if (n > bestN) { bestN = n; best = c }
                    i -= period
                }
                template[k * w + x] = best
            }
        }
        for (y in bottom until h) System.arraycopy(template, ((y - bottom) % period) * w, px, y * w, w)
        return true
    }

    private fun fillAbove(px: IntArray, w: Int, top: Int, band: Int, starry: Boolean) {
        val rows = IntArray(band) { dominant(px, w, top + it) }
        val columns = steadyColumns(px, w, top, band, rows[0])
        val period = period(rows)
        val edge = rows[0]
        // Stars as thick as the stage's own (none when clouds or rain hide them).
        val starColour = if (starry) brightest(px, w, top, band, edge) else null
        val starEvery = if (starColour == null) 0 else starSpacing(px, w, top, band, edge)
        for (y in top - 1 downTo 0) {
            val d = top - y
            val base = if (period > 0) rows[(period - d % period) % period] else edge
            val row = y * w
            for (x in 0 until w) {
                var c = columns[x].takeIf { it != NONE } ?: base
                if (starEvery > 0 && c == base && hash(x, y) % starEvery == 0) c = starColour!!
                px[row + x] = c
            }
        }
    }

    private fun fillBelow(px: IntArray, w: Int, h: Int, bottom: Int, band: Int) {
        val first = bottom - band
        val rows = IntArray(band) { dominant(px, w, first + it) }
        val period = period(rows)
        val edge = rows[band - 1]
        // Texture: band pixels that differ from their row's colour but stay close to it.
        val texture = ArrayList<Int>()
        var total = 0
        for (i in 0 until band) {
            val row = (first + i) * w
            for (x in 0 until w) {
                total++
                val c = px[row + x]
                if (c != rows[i] && close(c, rows[i]) && texture.size < 256) texture += c
            }
        }
        val density = if (total == 0) 0f else (texture.size.toFloat() / total).coerceAtMost(0.05f)
        val threshold = (density * 1000).toInt()
        for (y in bottom until h) {
            val d = y - bottom + 1
            val base = if (period > 0) rows[band - period + (period - 1 + d) % period] else edge
            val row = y * w
            for (x in 0 until w) {
                var c = base
                if (texture.isNotEmpty()) {
                    val hv = hash(x, y)
                    if (hv % 1000 < threshold) c = texture[(hv / 1000) % texture.size]
                }
                px[row + x] = c
            }
        }
    }

    private const val NONE = 0

    /** Per column: its colour when it is one colour through the whole band (and not the background), else [NONE]. */
    private fun steadyColumns(px: IntArray, w: Int, first: Int, band: Int, background: Int): IntArray {
        val out = IntArray(w)
        if (band < 4) return out
        for (x in 0 until w) {
            val c = px[first * w + x]
            if (c == background || c == NONE) continue
            var steady = true
            for (i in 1 until band) if (px[(first + i) * w + x] != c) { steady = false; break }
            if (steady) out[x] = c
        }
        return out
    }

    /** The shortest repeat in [rows] (at least two rows, and not all one colour), or 0. */
    private fun period(rows: IntArray): Int {
        if (rows.all { it == rows[0] }) return 0
        for (p in 2..rows.size / 2) {
            var ok = true
            for (i in 0 until rows.size - p) if (rows[i] != rows[i + p]) { ok = false; break }
            if (ok) return p
        }
        return 0
    }

    private fun dominant(px: IntArray, w: Int, y: Int): Int {
        val counts = HashMap<Int, Int>()
        var best = px[y * w]
        var bestN = 0
        for (x in 0 until w) {
            val c = px[y * w + x]
            val n = (counts[c] ?: 0) + 1
            counts[c] = n
            if (n > bestN) { bestN = n; best = c }
        }
        return best
    }

    /** One star per this many sky pixels, as in the band; 0 when the band has no stars. */
    private fun starSpacing(px: IntArray, w: Int, first: Int, band: Int, background: Int): Int {
        var bright = 0
        for (i in 0 until band) for (x in 0 until w) {
            val c = px[(first + i) * w + x]
            if (c != background && luma(c) > 150) bright++
        }
        return if (bright == 0) 0 else (band * w / bright).coerceAtLeast(40)
    }

    private fun brightest(px: IntArray, w: Int, first: Int, band: Int, background: Int): Int? {
        var best: Int? = null
        var bestL = 150
        for (i in 0 until band) for (x in 0 until w) {
            val c = px[(first + i) * w + x]
            if (c == background) continue
            val l = luma(c)
            if (l > bestL) { bestL = l; best = c }
        }
        return best
    }

    /** True when two colours differ by at most about a fifth on every channel. */
    private fun close(a: Int, b: Int): Boolean {
        for (shift in intArrayOf(16, 8, 0)) {
            if (kotlin.math.abs(((a shr shift) and 0xFF) - ((b shr shift) and 0xFF)) > 48) return false
        }
        return true
    }

    private fun luma(c: Int): Int = (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000

    /** Deterministic per-pixel noise, so the texture holds still from frame to frame. */
    private fun hash(x: Int, y: Int): Int {
        var v = x * 73856093 xor y * 19349663
        v = v xor (v ushr 13)
        v *= 1274126177
        v = v xor (v ushr 16)
        return v and 0x7FFFFFFF
    }
}
