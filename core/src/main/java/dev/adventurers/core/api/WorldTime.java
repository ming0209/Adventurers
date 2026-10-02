package dev.adventurers.core.api;

public final class WorldTime {
    public static final long TICKS_PER_DAY = 24_000;
    public static final int DAYS_PER_YEAR = 24;
    public static final int DAYS_PER_SEASON = 6;
    private WorldTime() {}
    public static long day(long tick) { return tick / TICKS_PER_DAY; }
    public static long year(long tick) { return day(tick) / DAYS_PER_YEAR; }
    public static int season(long tick) { return (int) (day(tick) % DAYS_PER_YEAR) / DAYS_PER_SEASON; }
}
