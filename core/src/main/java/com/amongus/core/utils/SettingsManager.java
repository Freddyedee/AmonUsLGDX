package com.amongus.core.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlWriter;

import java.io.StringWriter;

public class SettingsManager {
    private static final String FILE_NAME = "settings.xml";
    public static String playerColor = "ROJO"; // Color por defecto

    // Valores por defecto
    public static String playerName = "Jugador";
    public static int width = 1280;
    public static int height = 720;

    public static void load() {
        // Gdx.files.local guarda el archivo en la carpeta de ejecución del juego
        FileHandle file = Gdx.files.local(FILE_NAME);

        if (file.exists()) {
            try {
                XmlReader reader = new XmlReader();
                XmlReader.Element root = reader.parse(file);

                playerName = root.getChildByName("playerName").getText();
                width = Integer.parseInt(root.getChildByName("width").getText());
                height = Integer.parseInt(root.getChildByName("height").getText());
                playerColor = root.getChildByName("playerColor") != null ? root.getChildByName("playerColor").getText() : "ROJO";
            } catch (Exception e) {
                System.out.println("Error parseando settings.xml, usando valores por defecto: " + e.getMessage());
            }
        } else {
            // Si el archivo no existe (primera vez que se abre el juego), lo crea
            save();
        }
    }



    public static void save() {
        FileHandle file = Gdx.files.local(FILE_NAME);
        try {
            StringWriter writer = new StringWriter();
            XmlWriter xml = new XmlWriter(writer);

            // Construcción del documento XML
            xml.element("settings")
                .element("playerName", playerName)
                .element("width", width)
                .element("height", height)
                .element("playerColor", playerColor)
                .pop(); // Cierra la etiqueta <settings>

            file.writeString(writer.toString(), false);
        } catch (Exception e) {
            System.out.println("Error guardando settings.xml: " + e.getMessage());
        }
    }

    public static void applyResolution() {
        Gdx.graphics.setWindowedMode(width, height);
    }
}
