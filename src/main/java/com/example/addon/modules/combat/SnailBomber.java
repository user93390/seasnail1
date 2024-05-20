    package com.example.addon.modules.combat;

    import com.example.addon.Addon;
    import meteordevelopment.meteorclient.settings.*;
    import meteordevelopment.meteorclient.systems.modules.Module;
    import meteordevelopment.meteorclient.utils.misc.Keybind;
    import meteordevelopment.meteorclient.utils.render.color.SettingColor;
    import meteordevelopment.meteorclient.events.world.TickEvent;
    import meteordevelopment.meteorclient.renderer.ShapeMode;
    import meteordevelopment.meteorclient.settings.*;
    import meteordevelopment.meteorclient.systems.modules.Module;
    import meteordevelopment.meteorclient.utils.entity.TargetUtils;
    import meteordevelopment.meteorclient.utils.player.InvUtils;
    import meteordevelopment.meteorclient.utils.render.RenderUtils;
    import meteordevelopment.meteorclient.utils.render.color.Color;
    import meteordevelopment.meteorclient.utils.render.color.SettingColor;
    import net.minecraft.block.Blocks;
    import net.minecraft.entity.player.PlayerEntity;
    import net.minecraft.item.Items;
    import net.minecraft.util.math.BlockPos;
    import meteordevelopment.meteorclient.utils.world.BlockUtils;
    import meteordevelopment.orbit.EventHandler;
    import net.minecraft.text.Text;
    import net.minecraft.util.Formatting;
    import meteordevelopment.meteorclient.utils.player.ChatUtils;
    import meteordevelopment.meteorclient.utils.player.FindItemResult;
    import meteordevelopment.meteorclient.utils.entity.SortPriority;
    
    import java.util.ArrayList;
    
    public class SnailBomber extends Module {
        private final SettingGroup sgPlace = settings.createGroup("Place");
        private final SettingGroup sgBreak = settings.createGroup("Break");
        private final SettingGroup sgRange = settings.createGroup("Range");
        private final SettingGroup sgDamage = settings.createGroup("Damage");
        private final SettingGroup sgFacePlace = settings.createGroup("FacePlace");
        private final SettingGroup sgSwitch = settings.createGroup("Swap");
        private final SettingGroup sgSync = settings.createGroup("Sync");
        private final SettingGroup sgRotate = settings.createGroup("Rotate");
        private final SettingGroup sgDebugg = settings.createGroup("Debugg");
        private final SettingGroup sgPause = settings.createGroup("Pause");
        private final SettingGroup sgRender = settings.createGroup("Render");
        private final SettingGroup sgGeneral = settings.getDefaultGroup();
        public enum AutoSwitchMode {
            Silent,
         Normal,
        }
        // Place settings
        private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
            .name("place")
            .description("Determines if the module should place crystals.")
            .defaultValue(true)
            .build());
    
        private final Setting<Double> placeDelay = sgPlace.add(new DoubleSetting.Builder()
            .name("place-delay")
            .description("How quickly crystals should be placed, in ticks.")
            .defaultValue(5.0)
            .min(0)
            .sliderMax(25.0)
            .build());
    
        private final Setting<Integer> placeHealth = sgPlace.add(new IntSetting.Builder()
            .name("place-health")
            .description("Minimum health to place crystals.")
            .defaultValue(5)
            .min(0)
            .sliderMax(30)
            .build());
    
            private final Setting<Boolean> support = sgPlace.add(new BoolSetting.Builder()
            .name("Support place")
            .description("places obsidian to place the crystals")
            .defaultValue(true)
            .build());
    
        // Break settings
        private final Setting<Boolean> breakCrystal = sgBreak.add(new BoolSetting.Builder()
            .name("break")
            .description("Determines if the module should break crystals.")
            .defaultValue(true)
            .build());
    
        private final Setting<Double> breakDelay = sgBreak.add(new DoubleSetting.Builder()
            .name("break-delay")
            .description("How quickly crystals should be broken, in ticks.")
            .defaultValue(5.0)
            .min(0)
            .sliderMax(25.0)
            .build());
    
            private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());
    
        private final Setting<Integer> breakHealth = sgBreak.add(new IntSetting.Builder()
            .name("break-health")
            .description("Minimum health to break crystals.")
            .defaultValue(5)
            .min(0)
            .sliderMax(30)
            .build());
    
            
            private final Setting<Boolean> antiWeakness = sgSwitch.add(new BoolSetting.Builder()
               .name("anti-weakness")
               .description("Determines if the module should swap to sword.")
               .defaultValue(true)
               .build());
    
               
               private final Setting<AutoSwitchMode> antiWeaknessMode = sgSwitch.add(new EnumSetting.Builder<AutoSwitchMode>()
                  .name("anti weakness mode")
                  .description("swap mode for anti-weakness")
                  .defaultValue(AutoSwitchMode.Silent)
                  .build());
    
        // Range settings
        private final Setting<Double> Range = sgRange.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range to break and place crystals")
            .defaultValue(4.0)
            .min(0)
            .sliderMax(10.0)
            .build());
    
        private final Setting<Double> targetRange = sgRange.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players are targeted.")
            .defaultValue(4.0)
            .min(0)
            .sliderMax(10.0)
            .build());
    
        // Damage settings
        private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage for the Crystal Bomber.")
            .defaultValue(4.0)
            .min(0)
            .sliderMax(35.0)
            .build());
    
        private final Setting<Double> maxSelfDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("max-self-damage")
            .description("The maximum self damage allowed.")
            .defaultValue(6.0)
            .min(1)
            .sliderMax(35.0)
            .build());
    
        private final Setting<Double> damageRatio = sgDamage.add(new DoubleSetting.Builder()
            .name("damage-ratio")
            .description("The damage ratio of minimum damage to self damage.")
            .defaultValue(2.0)
            .min(0)
            .sliderMax(10.0)
            .build());
    
        // FacePlace settings
        private final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder()
            .name("face-place")
            .description("Determines if the module should face place crystals.")
            .defaultValue(true)
            .build());
    
        private final Setting<Double> facePlaceHealth = sgFacePlace.add(new DoubleSetting.Builder()
            .name("face-place-health")
            .description("Minimum target health to face place.")
            .defaultValue(6.0)
            .min(1)
            .sliderMax(35.0)
            .visible(facePlace::get)
            .build());
    
        private final Setting<Keybind> forceFacePlace = sgFacePlace.add(new KeybindSetting.Builder()
            .name("force-face-place")
            .description("Starts face place when this button is pressed.")
            .defaultValue(Keybind.none())
            .visible(facePlace::get)
            .build());
    
        // Switch settings
        private final Setting<AutoSwitchMode> autoSwitch = sgSwitch.add(new EnumSetting.Builder<AutoSwitchMode>()
            .name("swap-mode")
            .description("Method used to swap items.")
            .defaultValue(AutoSwitchMode.Silent)
            .build());
    
        private final Setting<Double> SwapDelay = sgRange.add(new DoubleSetting.Builder()
            .name("swap delay")
            .description("how fast should we swap to the crystal")
            .defaultValue(5.0)
            .min(0)
            .sliderMax(25.0)
            .build());
        private final Setting<Boolean> DamageSync = sgSync.add(new BoolSetting.Builder()
            .name("Damage Sync")
            .description("syncs damage to server TPS")
            .defaultValue(true)
            .build());
        private final Setting<Boolean> DelaySync = sgSync.add(new BoolSetting.Builder()
            .name("Delay Sync")
            .description("Syncs delay to server TPS")
            .defaultValue(false)
            .build());
        
        private final Setting<Boolean> rotate = sgRotate.add(new BoolSetting.Builder()
        .name("rotate")
        .description("rotates you to the crystal")
        .defaultValue(true)
        .build());
        
        private final Setting<Boolean> Debug = sgDebugg.add(new BoolSetting.Builder()
        .name("debug")
        .description("debugs the module")
        .defaultValue(true)
        .build());
        
        private final Setting<SettingColor> LineColor = sgRender.add(new ColorSetting.Builder()
        .name("Line Color")
        .description("Line color")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .build());
        
        private final Setting<SettingColor> SideColor = sgRender.add(new ColorSetting.Builder()
        .name("Side color")
        .description("Side color")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .build());
        
        public SnailBomber() {
            super(Addon.COMBAT, "Snail Bomber+", "Explodes crystals.");
        }
    
        @EventHandler
        private void onTick(TickEvent.Pre event) {
            PlayerEntity player = mc.player;
            FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
            PlayerEntity target = TargetUtils.getPlayerTarget(Range.get(), SortPriority.LowestDistance);
    
            if (player == null || crystal == null || target == null) return;
    
            BlockPos playerPos = player.getBlockPos();
            BlockPos crystalWest = playerPos.west(1);
    
            BlockUtils.place(crystalWest, InvUtils.findInHotbar(Items.END_CRYSTAL), rotate.get(), 0, false);
            BlockUtils.breakBlock(crystalWest, false);
        }
    }