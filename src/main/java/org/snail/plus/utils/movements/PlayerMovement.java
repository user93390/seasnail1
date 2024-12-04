package org.snail.plus.utils.movements;

public class PlayerMovement {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public PlayerMovement(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
