package com.carrot.heartbeatmusic;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class AudioWaveView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] levels = new float[]{0.34f, 0.66f, 0.48f, 0.82f, 0.56f};
    private ValueAnimator animator;
    private float phase = 0f;

    public AudioWaveView(Context context) {
        super(context);
        paint.setColor(Color.rgb(255, 106, 0));
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void start() {
        if (animator != null && animator.isStarted()) {
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(720);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void stop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        phase = 0f;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float barWidth = width / 13f;
        float gap = barWidth * 0.8f;
        float startX = (width - (levels.length * barWidth + (levels.length - 1) * gap)) / 2f;
        paint.setStrokeWidth(barWidth);
        for (int i = 0; i < levels.length; i++) {
            float wave = (float) Math.sin((phase * Math.PI * 2f) + (i * 0.85f));
            float normalized = levels[i] + (wave * 0.18f);
            float barHeight = Math.max(height * 0.18f, height * normalized);
            float x = startX + (i * (barWidth + gap)) + (barWidth / 2f);
            float y1 = (height - barHeight) / 2f;
            float y2 = y1 + barHeight;
            canvas.drawLine(x, y1, x, y2, paint);
        }
    }
}
