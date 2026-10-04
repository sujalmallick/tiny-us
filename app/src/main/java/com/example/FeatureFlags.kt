package com.example

import com.example.scene.SceneType

/** Compile-time switches for features that are built but not ready to show to the public. */
object FeatureFlags {
    /**
     * Features that only make sense once two phones can exchange data (long-distance signals,
     * mood "shared/private" visibility). Off until offline partner pairing ("Love Packets") ships;
     * the data layer stays in place so nothing saved is lost.
     */
    const val PARTNER_SYNC = false

    /**
     * Plan 03 pixel-art overhaul: draw the world at game resolution and enlarge it without
     * smoothing. Only [PIXEL_RENDERER_SCENES] use it while the look is being tried out.
     */
    const val PIXEL_RENDERER = true

    /** Scenes drawn by the low-res renderer (Phase 1 test: the campfire only). */
    val PIXEL_RENDERER_SCENES: Set<SceneType> = setOf(SceneType.CAMPFIRE)
}
