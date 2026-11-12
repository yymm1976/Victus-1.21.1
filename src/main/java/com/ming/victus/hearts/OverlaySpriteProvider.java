package com.ming.victus.hearts;

public interface OverlaySpriteProvider {
    int getOverlayTint();
    int getOverlayIndex();
    default boolean shouldRenderOverlay() {
        return true;
    }
}