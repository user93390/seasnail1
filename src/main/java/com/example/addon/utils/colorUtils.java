package com.example.addon.utils;

import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public class colorUtils {

    // Assuming boundingBox is an instance of Box, which is part of the method's parameter
    public static double[] getPos(Box boundingBox) {
        double x1 = boundingBox.minX;
        double y1 = boundingBox.minY;
        double z1 = boundingBox.minZ;
        double x2 = boundingBox.maxX;
        double y2 = boundingBox.maxY;
        double z2 = boundingBox.maxZ;
        return new double[]{x1, y1, z1, x2, y2, z2};
    }
    
        public static void colorFade(Renderer3D renderer, Box boundingBox, BlockPos pos, ShapeMode shapeMode, Color color1, Color color2, double delayFactor) {
            int red = (int) (color1.r * (1 - delayFactor) + color2.r * delayFactor);
            int green = (int) (color1.g * (1 - delayFactor) + color2.g * delayFactor);
            int blue = (int) (color1.b * (1 - delayFactor) + color2.b * delayFactor);
            int alpha = (int) (color1.a * (1 - delayFactor) + color2.a * delayFactor);
    
            Color interpolatedColor = new Color(red, green, blue, alpha);
    
            renderer.box(boundingBox.minX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ, interpolatedColor, interpolatedColor, shapeMode, 0);
        }
    

    public static void colorRender(SettingColor color1, SettingColor color2, boolean sides) {
        // Assuming there is a context where these colors are applied for rendering
        // Implementation should include rendering logic using color1, color2, and sides
    }

    public static boolean breakSync(double mineProgress) {
        double progress = Math.min(10, mineProgress + 1);
        double delayFactor = progress / 10.0;
        // Return true or false based on some conditions if necessary
        return true;
    }
}