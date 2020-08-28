package com.SMU.DevSec;

class SideChannelValue {

    Long systemTime;
    int volume;
    long allocatableBytes;
    long cacheQuotaBytes;
    long cacheSize;
    long freeSpace;
    long usableSpace;
    long elapsedCpuTime;
    float currentBatteryLevel;
    long batteryChargeCounter;
    long mobileTxBytes;
    long totalTxBytes;
    long mobileTxPackets;
    long totalTxPackets;
    long mobileRxBytes;
    long totalRxBytes;
    long mobileRxPackets;
    long totalRxPackets;

    int labels;
    /**
     * Constructor for this POJO class to store side channel values
     */
    public SideChannelValue() {
    }

    public long getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(long systemTime) {
        this.systemTime = systemTime;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public long getAllocatableBytes() {
        return allocatableBytes;
    }

    public void setAllocatableBytes(long allocatableBytes) {
        this.allocatableBytes = allocatableBytes;
    }

    public long getCacheQuotaBytes() {
        return cacheQuotaBytes;
    }

    public void setCacheQuotaBytes(long cacheQuotaBytes) {
        this.cacheQuotaBytes = cacheQuotaBytes;
    }

    public long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(long freeSpace) {
        this.freeSpace = freeSpace;
    }

    public long getUsableSpace() {
        return usableSpace;
    }

    public void setUsableSpace(long usableSpace) {
        this.usableSpace = usableSpace;
    }

    public long getElapsedCpuTime() {
        return elapsedCpuTime;
    }

    public void setElapsedCpuTime(long elapsedCpuTime) {
        this.elapsedCpuTime = elapsedCpuTime;
    }

    public float getCurrentBatteryLevel() {
        return currentBatteryLevel;
    }

    public void setCurrentBatteryLevel(float currentBatteryLevel) {
        this.currentBatteryLevel = currentBatteryLevel;
    }

    public long getBatteryChargeCounter() {
        return batteryChargeCounter;
    }

    public void setBatteryChargeCounter(long batteryChargeCounter) {
        this.batteryChargeCounter = batteryChargeCounter;
    }

    public long getMobileTxBytes() {
        return mobileTxBytes;
    }

    public void setMobileTxBytes(long mobileTxBytes) {
        this.mobileTxBytes = mobileTxBytes;
    }

    public long getTotalTxBytes() {
        return totalTxBytes;
    }

    public void setTotalTxBytes(long totalTxBytes) {
        this.totalTxBytes = totalTxBytes;
    }

    public long getMobileTxPackets() {
        return mobileTxPackets;
    }

    public void setMobileTxPackets(long mobileTxPackets) {
        this.mobileTxPackets = mobileTxPackets;
    }

    public long getTotalTxPackets() {
        return totalTxPackets;
    }

    public void setTotalTxPackets(long totalTxPackets) {
        this.totalTxPackets = totalTxPackets;
    }

    public long getMobileRxBytes() {
        return mobileRxBytes;
    }

    public void setMobileRxBytes(long mobileRxBytes) {
        this.mobileRxBytes = mobileRxBytes;
    }

    public long getTotalRxBytes() {
        return totalRxBytes;
    }

    public void setTotalRxBytes(long totalRxBytes) {
        this.totalRxBytes = totalRxBytes;
    }

    public long getMobileRxPackets() {
        return mobileRxPackets;
    }

    public void setMobileRxPackets(long mobileRxPackets) {
        this.mobileRxPackets = mobileRxPackets;
    }

    public long getTotalRxPackets() {
        return totalRxPackets;
    }

    public void setTotalRxPackets(long totalRxPackets) {
        this.totalRxPackets = totalRxPackets;
    }

    public void setLabels(int labels){this.labels = labels;}

    public int getLabels(){return labels;}

}