package com.SMU.DevSec;

class GroundTruthValue {
    Long systemTime;
    String app;
    String labels;
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

    public void setLabels(String labels){this.labels = labels;}

    public String getLabels(){return labels;}

    public String getCurrentApp() {return app;}

    public void setCurrentApp(String CurrentApp){this.app = CurrentApp;}

}