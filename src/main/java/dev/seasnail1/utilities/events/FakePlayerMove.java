package dev.seasnail1.utilities.events;

import net.minecraft.util.math.Direction;

public class FakePlayerMove {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public Direction direction;
    public float headYaw;
    public float bodyYaw;

    public FakePlayerMove(double x, double y, double z, float yaw, float pitch, Direction direction, float headYaw) {
        this.direction = direction;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.headYaw = headYaw;
    }
}

