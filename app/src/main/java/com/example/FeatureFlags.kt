package com.example

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
     * smoothing. Off falls back to the classic full-resolution renderer.
     */
    const val PIXEL_RENDERER = true
}
