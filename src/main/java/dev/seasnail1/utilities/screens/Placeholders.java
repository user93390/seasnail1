package dev.seasnail1.utilities.screens;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Placeholders {

    public static String title = "Placeholders";
    public static List<String> items = new ArrayList<>();

    public static void showScreen() {
        mc.setScreen(new showScreen());
    }

    public static class showScreen extends WindowScreen {
        Runnable onClose = () -> {
            if(mc.player != null) {
                mc.player.closeScreen();
            }
        };

        public showScreen() {
            super(GuiThemes.get(), "Placeholders");
        }

        @Override
        public void initWidgets() {
            for (String item : items) {
                this.add(theme.label(item));
            }

            this.add(theme.button("Close")).widget().action = onClose;
        }
    }
}
