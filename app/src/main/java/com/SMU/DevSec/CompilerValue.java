package com.SMU.DevSec;

public class CompilerValue {
    Long systemTime;
    int function;
    int threshold;
    /**
     * Constructor for this POJO class to store side channel values
     */
    CompilerValue() {
    }

    public long getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(long systemTime) {
        this.systemTime = systemTime;
    }

    public void setFunctions(int function){this.function = function;}

    public int getFunctions(){return function;}

    public void setThresholds(int threshold) {
        this.threshold = threshold;
    }

    public int getThresholds(){
        return threshold;
    }
}
