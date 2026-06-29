package com.zenith.app;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Glide configuration module for Zenith.
 *
 * Required by Glide v4's annotation processor. Without this class, the
 * processor emits warnings about every class with multiple constructors
 * (e.g., Room entities) and may fail to generate the GlideApp index.
 */
@GlideModule
public final class ZenithGlideModule extends AppGlideModule {
    // Default config — no overrides needed.
    // Custom caching, request options, etc. can be added here later.
}
