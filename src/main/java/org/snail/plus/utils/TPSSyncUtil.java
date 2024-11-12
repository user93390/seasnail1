package org.snail.plus.utils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TPSSyncUtil {

    public static double getSync(Double time, java.util.concurrent.TimeUnit unit) {
        int count;
        long millis = unit.toMillis(time.longValue());
        synchronized (TPSSyncUtil.class) {
            count = (int) (millis / mc.getServer().getAverageTickTime());
        }
        return count;
    }
}