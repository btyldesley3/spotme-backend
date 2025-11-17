package com.spotme.domain.model.exercise;

public class VolumeLandmarks {

    private final int mev; // Minimum Effective Volume
    private final int mav; // Maximum Adaptive Volume
    private final int mrv; // Maximum Recoverable Volume

    public VolumeLandmarks(int mev, int mav, int mrv) {
        this.mev = mev;
        this.mav = mav;
        this.mrv = mrv;
    }

    public int getMev() {
        return mev;
    }

    public int getMav() {
        return mav;
    }

    public int getMrv() {
        return mrv;
    }

}
