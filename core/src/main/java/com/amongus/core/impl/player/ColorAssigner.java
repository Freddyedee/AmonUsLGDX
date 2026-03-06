package com.amongus.core.impl.player;

import com.amongus.core.api.player.SkinColor;

import java.util.HashSet;
import java.util.Set;

public class ColorAssigner {

    private final SkinColor[] avaible = SkinColor.values();
    private final Set<SkinColor> used = new HashSet<>();
    private int index = 0;

    public SkinColor assign() {
        for(int i = 0; i < avaible.length; i++){
            SkinColor candidate = avaible[index % avaible.length];
            index++;

            if(!used.contains(candidate)){
                used.add(candidate);
                return candidate;
            }
        }
        //Si todos estan utlizadas vuelve a utilizar los del inicio.
        return avaible[index++ % avaible.length];
    }

}
