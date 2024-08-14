package org.snail.plus.utils;

public class TimeUtils {
    private static float GetTPS() {
        float tpsSupplier;
        tpsSupplier = (float) TPSSyncUtil.getCurrentTPS(); return tpsSupplier;
    }

    public static float TicksPerSecond(double Time) {
        float currentTPS;
        currentTPS = (float) TPSSyncUtil.getCurrentTPS();

        float TicksPerSecond = currentTPS;
        return (float) (Time / TicksPerSecond);
    }

    public static float TicksPerMiliseconds(float Time) {
        float currentTPS;
        currentTPS = (float) TPSSyncUtil.getCurrentTPS();

        float TicksPerMilisecond = currentTPS * 1000;
        return Time / TicksPerMilisecond;
    }

    public static float SecondsPerTick(float time) {
        float currentTPS;
        currentTPS = (float) TPSSyncUtil.getCurrentTPS();

        float secondsPerTick = 1.0f / currentTPS;
        return time / secondsPerTick;
    }

    public static double SyncTime(float time) {
        float currentDelay = 1.0F;
        return currentDelay / TPSSyncUtil.getCurrentTPS();
    }

    public static float MillisecondsPerTick(float time) {
        float currentTPS;
        currentTPS = (float) TPSSyncUtil.getCurrentTPS();

        float millisecondsPerTick = 1000.0f / currentTPS;
        return time / millisecondsPerTick;
    }
    public static long setDelay(double delay) {
        return (long) (delay * 1000);
    }
}
