package org.snail.plus.utilities.screens;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Placeholders {

    public static String title = "Placeholders";
    public static List<String> items;

    public static void showScreen() {
        mc.setScreen(new showScreen());
    }

    public  static class showScreen extends WindowScreen {
        public showScreen() {
            super(GuiThemes.get(), "Placeholders");
        }

        @Override
        public void initWidgets() {
            for(String item : items) {
                this.add(theme.label(item));
            }

            this.add(theme.button("Close")).widget().action = onClose;
        }
        Runnable onClose = () -> mc.player.closeScreen();
    }
}
