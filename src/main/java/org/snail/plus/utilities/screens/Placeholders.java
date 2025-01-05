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

    public Placeholders setTitle(String title) {
        Placeholders.title = title;
        return this;
    }

    public Placeholders addItem(String... items) {
        Placeholders.items = List.of(items);
        return this;
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
