package com.github.serivesmejia.eocvsim.input;

import com.github.serivesmejia.eocvsim.EOCVSim;
import org.opencv.core.Mat;

public abstract class InputSource implements Comparable<InputSource> {

    public transient boolean isDefault = false;
    public transient EOCVSim eocvSim = null;

    protected transient String name = "";
    protected transient boolean isPaused = false;
    private transient boolean beforeIsPaused = false;

    protected long createdOn = -1L;

    public abstract boolean init();
    public abstract void reset();
    public abstract void close();

    public abstract void onPause();
    public abstract void onResume();

    public Mat update() {
        return null;
    }

    public final InputSource cloneSource() {
        InputSource source = internalCloneSource();
        source.createdOn = createdOn;
        return source;
    }

    protected abstract InputSource internalCloneSource();

    public final void setPaused(boolean paused) {

        isPaused = paused;

        if (beforeIsPaused != isPaused) {
            if (isPaused) {
                onPause();
            } else {
                onResume();
            }
        }

        beforeIsPaused = paused;

    }

    public final String getName() {
        return name;
    }

    @Override
    public final int compareTo(InputSource source) {
        return createdOn > source.createdOn ? 1 : -1;
    }

}