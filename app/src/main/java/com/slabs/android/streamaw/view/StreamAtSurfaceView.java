package com.slabs.android.streamaw.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.slabs.android.streamaw.media.StreamAtControl;

public class StreamAtSurfaceView  extends SurfaceView {
    StreamAtControl mControl;
    public StreamAtSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mControl = new StreamAtControl(context, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mControl.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }
}
