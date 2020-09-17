package com.SMU.DevSec;

class FrontAppValue {
    Long systemTime;
    String app;
    /**
     * Constructor for this POJO class to store side channel values
     */
    FrontAppValue() {
    }

    public long getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(long systemTime) {
        this.systemTime = systemTime;
    }

    public String getCurrentApp() {return app;}

    public void setCurrentApp(String CurrentApp){this.app = CurrentApp;}

}