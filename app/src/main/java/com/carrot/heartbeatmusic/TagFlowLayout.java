package com.carrot.heartbeatmusic;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class TagFlowLayout extends ViewGroup {
    private final int horizontalGap;
    private final int verticalGap;

    public TagFlowLayout(Context context, int horizontalGap, int verticalGap) {
        super(context);
        this.horizontalGap = horizontalGap;
        this.verticalGap = verticalGap;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int lineWidth = 0;
        int lineHeight = 0;
        int totalHeight = getPaddingTop() + getPaddingBottom();
        int usedWidth = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            if (lineWidth > 0 && lineWidth + horizontalGap + childWidth > maxWidth) {
                totalHeight += lineHeight + verticalGap;
                usedWidth = Math.max(usedWidth, lineWidth);
                lineWidth = childWidth;
                lineHeight = childHeight;
            } else {
                lineWidth += lineWidth == 0 ? childWidth : horizontalGap + childWidth;
                lineHeight = Math.max(lineHeight, childHeight);
            }
        }

        totalHeight += lineHeight;
        usedWidth = Math.max(usedWidth, lineWidth) + getPaddingLeft() + getPaddingRight();
        setMeasuredDimension(resolveSize(usedWidth, widthMeasureSpec), resolveSize(totalHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int maxWidth = right - left - getPaddingLeft() - getPaddingRight();
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int lineHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            if (x > getPaddingLeft() && x + childWidth > maxWidth + getPaddingLeft()) {
                x = getPaddingLeft();
                y += lineHeight + verticalGap;
                lineHeight = 0;
            }
            child.layout(x, y, x + childWidth, y + childHeight);
            x += childWidth + horizontalGap;
            lineHeight = Math.max(lineHeight, childHeight);
        }
    }
}
