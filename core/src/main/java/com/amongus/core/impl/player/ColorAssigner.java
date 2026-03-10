package com.amongus.core.impl.player;

import com.amongus.core.api.player.SkinColor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ColorAssigner {
    private final List<SkinColor> available;

    public ColorAssigner() {
        // Cargamos todos los colores a la lista de disponibles
        available = new ArrayList<>(Arrays.asList(SkinColor.values()));
    }

    // Intenta asignar el color que el jugador quiere
    public SkinColor assign(SkinColor preferred) {
        if (preferred != null && available.contains(preferred)) {
            available.remove(preferred);
            return preferred;
        }
        return assignRandom();
    }

    // Asigna un color aleatorio si el preferido ya está en uso
    public SkinColor assignRandom() {
        if (available.isEmpty()) {
            // Si se llenó la sala (más jugadores que colores), repetimos
            return SkinColor.values()[(int)(Math.random() * SkinColor.values().length)];
        }
        Collections.shuffle(available);
        return available.remove(0);
    }

    // Método especial para la RED: Obliga a asignar un color porque otro cliente ya lo eligió en su pantalla
    public SkinColor assignForce(SkinColor networkColor) {
        available.remove(networkColor); // Lo quitamos de los disponibles locales
        return networkColor;
    }
}
