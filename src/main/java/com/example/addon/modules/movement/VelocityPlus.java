package com.example.addon.modules.movement;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import com.example.addon.modules.misc.Notifications;

import com.example.addon.Addon;

public class VelocityPlus extends Module {
        public VelocityPlus() {
            super(Addon.MOVEMENT, "Velocity Plus", "Adjusts velocity for different server configurations.");
    
            final SettingGroup sg2b2t = settings.createGroup("grim-Velocity");
            final SettingGroup ncpStrict = settings.createGroup("ncp-strict");
            final SettingGroup ncp = settings.createGroup("normal-velocity");
    
            final Setting<Boolean> grim = sg2b2t.add(new BoolSetting.Builder()
                    .name("Grim Velocity")
                    .description("2b2t Velocity")
                    .defaultValue(false)
                    .build()
            );
    
            final Setting<Boolean> strictNcp = ncpStrict.add(new BoolSetting.Builder()
                    .name("NCP Strict")
                    .description("Made for more strict servers like strict.2b2tpvp.org")
                    .defaultValue(false)
                    .build()
            );
            final Setting<Boolean> normal = ncp.add(new BoolSetting.Builder()
            .name("Normal")
            .description("made for almost every server")
            .defaultValue(true)
            .build()
    );
        }
    }
