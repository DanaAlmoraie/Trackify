package com.example.trackify;

// Distance manager to calculate daily + total km
public class DistanceManager {

    private double dailyKm = 0;
    private double totalKm = 0;

    private boolean isLoaded = false; // wait until Firebase loads totalKm

    // Called only once after Firebase loads totalKm
    public void setInitialTotalKm(double km) {
        this.totalKm = km;
        isLoaded = true;
    }

    // Add distance (from Google Maps OR mock testing)
    public void addDistance(double km) {
        if (!isLoaded) return; // protect from early calls
        dailyKm += km;
        totalKm += km;
    }

    public double getDailyKm() { return dailyKm; }

    public double getTotalKm() { return totalKm; }

    // Reset daily on shift end
    public void resetDaily() { dailyKm = 0; }
}
