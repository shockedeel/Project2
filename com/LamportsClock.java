package com;

public class LamportsClock {
    private int d;
    private int time;

    public LamportsClock() {
        this.d = 1;
        this.time = 0;
    }

    public LamportsClock(int d) {
        this.d = d;
        this.time = 0;
    }

    public int getClock() {
        return time;
    }

    public int sendMessageRule() {
        this.time += d;
        return this.time;
    }

    public int internalEvent() {
        this.time += d;
        return this.time;
    }

    public int receiveMessageRule(int msgTimestamp) {
        this.time = Math.max(msgTimestamp, this.time) + this.d;
        return this.time;
    }

}