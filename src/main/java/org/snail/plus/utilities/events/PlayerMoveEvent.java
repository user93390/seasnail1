package org.snail.plus.utilities.events;

public class PlayerMoveEvent {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public PlayerMoveEvent(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}