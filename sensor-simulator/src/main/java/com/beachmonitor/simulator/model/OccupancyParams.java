package com.beachmonitor.simulator.model;

/**
 * Parameters sent from the dashboard to control beach occupancy simulation.
 * Occupancy is calculated based on: simulated hour and day of week.
 * Temperature is intentionally excluded — it is already governed by the active
 * weather scenario (NORMAL, LEVANTE, STORM, etc.), so including it here would
 * create a double dependency with the scenario bounds applied in OccupancySimulator.
 */
public class OccupancyParams {

    private int hour = 12;           // 0-23, simulated hour of day
    private String dayOfWeek = "MONDAY"; // MONDAY-SUNDAY

    public OccupancyParams() {}

    /**
     * Calculate base occupancy percentage (0-100) from the parameters.
     *
     * Hour factor (0.0 - 1.0):
     *   Night (22-6): 0.02
     *   Morning (8-11): 0.3 - 0.55
     *   Midday (12-14): 0.95
     *   Afternoon (15-19): 0.4 - 0.85
     *   Evening (20-21): 0.15
     *
     * Weekend factor: weekdays 1.0, weekends 1.4
     */
    public double calculateBaseOccupancy() {
        double hourFactor = calculateHourFactor();
        double weekendFactor = isWeekend() ? 1.4 : 1.0;

        double base = hourFactor * weekendFactor * 100.0;
        return Math.min(100.0, Math.max(0.0, base));
    }

    private double calculateHourFactor() {
        if (hour >= 22 || hour <= 6) return 0.02;
        if (hour == 7) return 0.1;
        if (hour >= 8 && hour <= 9) return 0.3;
        if (hour >= 10 && hour <= 11) return 0.55;
        if (hour >= 12 && hour <= 14) return 0.95;
        if (hour == 15) return 0.85;
        if (hour >= 16 && hour <= 17) return 0.65;
        if (hour >= 18 && hour <= 19) return 0.4;
        if (hour >= 20 && hour <= 21) return 0.15;
        return 0.3;
    }

    private boolean isWeekend() {
        return "SATURDAY".equalsIgnoreCase(dayOfWeek) || "SUNDAY".equalsIgnoreCase(dayOfWeek);
    }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = Math.max(0, Math.min(23, hour)); }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}
