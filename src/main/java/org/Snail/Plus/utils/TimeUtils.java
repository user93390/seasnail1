package org.Snail.Plus.utils;

public class TimeUtils {

    private static float GetTPS() {
        float tpsSupplier;
        tpsSupplier = (float)
                TPSSyncUtil.getCurrentTPS();

        return tpsSupplier;
    }

    public static float TicksPerSecond(double Time) {
        float currentTPS;
        currentTPS = (float) TPSSyncUtil.getCurrentTPS();

        float TicksPerSecond = currentTPS;
        float seconds = (float) (Time
                / TicksPerSecond);
        return seconds;
    }

    public static float TicksPerMiliseconds(float Time) {
        float currentTPS;
        currentTPS = (float) TPSSyncUtil.getCurrentTPS();

        float TicksPerMilisecond = currentTPS * 1000;
        float miliseconds = Time / TicksPerMilisecond;
        return miliseconds;
    }

    public static float SecondsPerTick(float time) {
        float currentTPS;
        currentTPS = (float) TPSSyncUtil.getCurrentTPS();

        float secondsPerTick =
                1.0f / currentTPS;
        float seconds = time / secondsPerTick;
        return seconds;
    }

    public static double SyncTime(float time) {
        float currentDelay = 1.0F;
        return currentDelay / TPSSyncUtil.getCurrentTPS();
    }

    public static float MillisecondsPerTick(float time) {
        float currentTPS;
        currentTPS = (float) TPSSyncUtil.getCurrentTPS();

        float millisecondsPerTick =
                1000.0f / currentTPS;
        float milliseconds = time / millisecondsPerTick;
        return milliseconds;
    }

}
