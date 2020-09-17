package com.SMU.DevSec;

class GroundTruthValue {
    Long systemTime;
    int labels;
    /**
     * Constructor for this POJO class to store side channel values
     */
    GroundTruthValue() {
    }

    public long getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(long systemTime) {
        this.systemTime = systemTime;
    }

    public void setLabels(int labels){this.labels = labels;}

    public int getLabels(){return labels;}

}