package com.amongus.lwjgl3;

import com.amongus.core.AmongUsGame;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("AmongUs - UNEG");
        config.setWindowedMode(800, 600);
        config.setForegroundFPS(60);
        config.setWindowIcon("libgdx128.png");
        config.useVsync(true);

        new Lwjgl3Application(new AmongUsGame(), config);
    }
}
