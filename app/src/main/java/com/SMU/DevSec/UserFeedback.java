package com.SMU.DevSec;

public class UserFeedback {
    Long arisingtime;
    String app;
    int event;
    Long answeringtime;
    int choice;
    int pattern;
    /**
     * Constructor for this POJO class to store side channel values
     */
    UserFeedback() {
    }

    public long getArisingtime() {
        return arisingtime;
    }

    public void setArisingtime(long arisingtime) {
        this.arisingtime = arisingtime;
    }

    public long getAnsweringtime() {
        return answeringtime;
    }

    public void setAnsweringtime(long answeringtime) {
        this.answeringtime = answeringtime;
    }

    public int getChoice() {return choice;}

    public void setChoice(int Choice){this.choice = Choice;}

    public int getEvent() {return event;}

    public void setEvent(int event) {this.event = event;}

    public String getApp() {return app;}

    public void setApp(String app) {this.app = app;}

    public void setPattern(int cmp) {
        pattern = cmp;
    }

    public int getPattern() {
        return pattern;
    }
}