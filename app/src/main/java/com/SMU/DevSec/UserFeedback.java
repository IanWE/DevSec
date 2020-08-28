package com.SMU.DevSec;

public class UserFeedback {
    Long systemTime;
    int choice;
    /**
     * Constructor for this POJO class to store side channel values
     */
    UserFeedback() {
    }

    public long getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(long systemTime) {
        this.systemTime = systemTime;
    }

    public int getChoice() {return choice;}

    public void setChoice(int Choice){this.choice = Choice;}

}