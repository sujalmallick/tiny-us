package com.example.engine

class AntiRepeatRandomPicker<T>(private val items: List<T>) {
    private var lastPicked: T? = null
    private var isFirstPick: Boolean = true

    fun pick(): T {
        if (items.isEmpty()) throw NoSuchElementException("Cannot pick from empty list")
        if (items.size == 1) return items[0]

        val chosen = if (isFirstPick) {
            isFirstPick = false
            items[0]
        } else {
            val candidates = items.filter { it != lastPicked }
            candidates.randomOrNull(WorldRandom.rng) ?: items.random(WorldRandom.rng)
        }
        lastPicked = chosen
        return chosen
    }

    fun reset() {
        lastPicked = null
        isFirstPick = true
    }

    fun pickFrom(dynamicItems: List<T>): T {
        if (dynamicItems.isEmpty()) throw NoSuchElementException("Cannot pick from empty list")
        if (dynamicItems.size == 1) {
            lastPicked = dynamicItems[0]
            return dynamicItems[0]
        }
        val candidates = dynamicItems.filter { it != lastPicked }
        val chosen = candidates.randomOrNull(WorldRandom.rng) ?: dynamicItems.random(WorldRandom.rng)
        lastPicked = chosen
        return chosen
    }
}

class ShuffledDice(sides: Int) {
    private val picker = AntiRepeatRandomPicker((0 until sides).toList())
    fun pick(): Int = picker.pick()
    fun reset() = picker.reset()
}
