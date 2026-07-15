package com.zenith.app.util;

import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.widget.TextView;

/**
 * Applies a two-color linear gradient fill directly to a TextView's text,
 * matching the "grad-text" effect used in the design preview (CSS
 * background-clip: text). Android has no XML attribute for this, so it's
 * done here with a Shader on the TextView's Paint, sized to the text's own
 * measured width once layout has happened.
 */
public final class GradientTextUtil {

    private GradientTextUtil() {}

    public static void applyGradient(TextView textView, int startColor, int endColor) {
        textView.post(() -> {
            float width = textView.getPaint().measureText(textView.getText().toString());
            if (width <= 0f) return;
            LinearGradient gradient = new LinearGradient(
                0, 0, width, 0,
                new int[]{startColor, endColor},
                null,
                Shader.TileMode.CLAMP
            );
            textView.getPaint().setShader(gradient);
            textView.invalidate();
        });
    }

    /** Call again after changing a gradient TextView's text, so the shader is re-measured. */
    public static void refresh(TextView textView, int startColor, int endColor) {
        applyGradient(textView, startColor, endColor);
    }
}
