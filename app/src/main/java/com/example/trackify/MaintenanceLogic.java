package com.example.trackify;

public class MaintenanceLogic {

    // intervals in KM
    public static final double OIL_INTERVAL = 10000;
    public static final double TIRES_INTERVAL = 50000;
    public static final double BRAKES_INTERVAL = 30000;

    // time-based interval (days)
    public static final int MAINTENANCE_DAYS = 90;

    // Calculate remaining km for each maintenance
    public static double remainingOil(double totalKm, double lastOilKm) {
        return OIL_INTERVAL - (totalKm - lastOilKm);
    }

    public static double remainingTires(double totalKm, double lastTireKm) {
        return TIRES_INTERVAL - (totalKm - lastTireKm);
    }

    public static double remainingBrakes(double totalKm, double lastBrakeKm) {
        return BRAKES_INTERVAL - (totalKm - lastBrakeKm);
    }

    // Time-based maintenance
    public static int daysUntilMaintenance(long lastDateMillis) {
        long now = System.currentTimeMillis();
        long diff = now - lastDateMillis;

        int daysPassed = (int) (diff / (1000 * 60 * 60 * 24));
        return MAINTENANCE_DAYS - daysPassed;
    }
}
