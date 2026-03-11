package com.amongus.core.impl.task.sabotageTask;

import com.amongus.core.api.minigame.MinigameProvider;
import com.amongus.core.api.task.TaskId;
import com.amongus.core.model.Position;

public class FixLightsSabotageTask extends SabotageTask {

    private static final String MAP_SPRITE =
        "minijuegos/fixLights/fixLightsObjt-removebg-preview.png";

    public FixLightsSabotageTask(TaskId id, Position location, MinigameProvider provider) {
        super(id, location, "Arreglar Luces", provider);
    }

    @Override public String getMapSpritePath()  { return MAP_SPRITE; }
    @Override public float  getMapSpriteScale() { return 1.3f; }
}


//Para futura implementacion de la vision------------------------


// ══════════════════════════════════════════════════════
// CAMBIOS EN SabotageManager.java
// Añadir LIGHTS al enum y el esqueleto de visión
// ══════════════════════════════════════════════════════

// 1. Añadir al enum SabotageType:
//    LIGHTS

// 2. Nuevo método en SabotageManager:
//    public boolean isLightsSabotaged() {
//        return activeSabotage == SabotageType.LIGHTS;
//    }

// 3. Esqueleto de efecto de visión — listo para implementar en GameScreen:
//
//    /**
//     * VISION EFFECT SKELETON
//     *
//     * Cuando isLightsSabotaged() == true, GameScreen debería:
//     *  1. Renderizar un overlay negro que cubra casi toda la pantalla
//     *  2. Dejar solo un círculo pequeño de visión alrededor del jugador
//     *
//     * Implementación futura en GameScreen.renderInGame():
//     *
//     *   if (sabotageManager.isLightsSabotaged()) {
//     *       renderVisionMask(playerScreenX, playerScreenY, visionRadius);
//     *   }
//     *
//     * Donde renderVisionMask() dibujaría con ShapeRenderer un overlay negro
//     * con un agujero circular usando blending:
//     *
//     *   private void renderVisionMask(float cx, float cy, float radius) {
//     *       // Técnica: FrameBuffer con máscara de stencil, o
//     *       // overlay negro semitransparente + círculo recortado con Pixmap
//     *       // LibGDX no tiene built-in masking, requiere ShaderProgram o FrameBuffer
//     *       // TODO: implementar cuando sea necesario
//     *   }
//     *
//     * El radio de visión recomendado: 120f (unidades de pantalla)
//     * Durante la transición encendido→apagado: lerp de 400f → 120f en 1 segundo
//     */


// ══════════════════════════════════════════════════════
// CAMBIOS EN GameSessionImpl.java
// Añadir la task de fix lights y activarla via sabotaje
// ══════════════════════════════════════════════════════

// Campo nuevo:
// private FixLightsSabotageTask fixLightsSabotageTask;

// En startGame(), junto a la internetSabotageTask:
//
//   FixLightsSabotageTask lightsTask = new FixLightsSabotageTask(
//       TaskId.random(),
//       new Position(600, 600),
//       new FixLightsMinigameProvider(engine, engine.getSabotageManager())
//   );
//   fixLightsSabotageTask = lightsTask;
//   allTasks.put(lightsTask.getId(), lightsTask);

// En activateSabotageTask(), añadir el case LIGHTS:
//
//   if (type == SabotageManager.SabotageType.LIGHTS && fixLightsSabotageTask != null) {
//       TaskId id = fixLightsSabotageTask.getId();
//       completedTaskIdsByPlayer.values().forEach(set -> set.remove(id));
//       players.values().forEach(p ->
//           assignedTaskIdsByPlayer
//               .computeIfAbsent(p.getId(), k -> new HashSet<>())
//               .add(id));
//       System.out.println("[Sabotage] Lights sabotage task activada para todos.");
//   }


// ══════════════════════════════════════════════════════
// CAMBIOS EN SabotageMapOverlay.java
// Añadir el botón de fix lights como segundo sabotaje
// ══════════════════════════════════════════════════════

// En SABOTAGE_TYPES reemplazar el segundo INTERNET por LIGHTS:
//
//   private static final SabotageManager.SabotageType[] SABOTAGE_TYPES = {
//       SabotageManager.SabotageType.INTERNET,
//       SabotageManager.SabotageType.LIGHTS,    // ← segundo botón
//   };
