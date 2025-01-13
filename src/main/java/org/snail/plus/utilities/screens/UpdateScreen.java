package org.snail.plus.utilities.screens;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class UpdateScreen {
    public static void showScreen() {
        mc.setScreen(new showScreen());
    }

    public static class showScreen extends WindowScreen {
        public showScreen() {
            super(GuiThemes.get(), "Placeholders");
        }

        List<String> items = List.of(
                "Update available for Snail++",
                "If you do not update, you may experience issues",
                "In the near future you may not be able to use the client without updating");

        @Override
        public void initWidgets() {
            for(String item : items) {
                this.add(theme.label(item));
            }

            this.add(theme.button("Okay")).widget().action = onClose;
        }
        Runnable onClose = () -> mc.player.closeScreen();
    }
}
