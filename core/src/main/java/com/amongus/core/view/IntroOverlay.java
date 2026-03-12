package com.amongus.core.view;

import com.amongus.core.api.player.Role;
import com.amongus.core.model.Position;
import com.amongus.core.view.GameSnapshot;
import com.amongus.core.view.PlayerView;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

/**
 * Overlay que muestra la pantalla de rol al inicio de la partida.
 * Se "disfraza" como una pantalla de carga de información útil.
 */
public class IntroOverlay {

    private final SpriteBatch batch;
    private final BitmapFont font; // Deberías usar una fuente grande
    private final Texture blackTexture; // Un simple pixel negro escalado
    private final GlyphLayout layout; // Para centrar el texto

    public IntroOverlay(SpriteBatch batch, BitmapFont font) {
        this.batch = batch;
        this.font = font;
        this.font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.font.getData().setScale(1f);

        this.layout = new GlyphLayout();

        // Creamos una textura negra de 1x1 pixel programáticamente
        // (Esto es más eficiente que cargar un archivo .png de 2MB de fondo negro)
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        this.blackTexture = new Texture(pixmap);
        pixmap.dispose(); // Liberamos memoria del pixmap
    }

    public void render(GameSnapshot snapshot, float remainingFreezeTime) {
        // Solo dibujamos si estamos congelados (Freeze Timer > 0)
        if (remainingFreezeTime <= 0) return;

        // 1. Buscamos el rol del jugador local
        PlayerView me = snapshot.getPlayers().stream()
            .filter(p -> p.getId().equals(snapshot.getLocalPlayerId()))
            .findFirst()
            .orElse(null);

        if (me == null) return;

        // 2. Iniciamos el Batch y dibujamos el fondo negro escalado a toda la pantalla
        batch.begin();
        // Nota: Gdx.graphics.getWidth/Height te dan el tamaño de la VENTANA, no del juego
        // Para que funcione bien en cualquier resolución, dibuja en coordenadas de pantalla.
        batch.setColor(1, 1, 1, 1); // Opaco total
        batch.draw(blackTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // 3. Configuramos el texto
        String text;
        Color textColor;

        if (me.getRole() == Role.IMPOSTOR) {
            text = "IMPOSTOR";
            textColor = Color.RED; // Letras rojas gigantes
        } else {
            text = "CREWMATE";
            textColor = Color.CYAN; // Letras cian gigantes
        }

        font.setColor(textColor);
        layout.setText(font, text, textColor, 0, Align.center, false);

        // 4. Calculamos el centro exacto de la pantalla
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = (Gdx.graphics.getHeight() / 2f) + (layout.height / 2f); // Sumamos layout.height/2 para el centrado vertical

        // 5. Dibujamos el texto centrado
        font.draw(batch, layout, centerX, centerY);

        batch.end();
    }

    public void dispose() {
        if (blackTexture != null) blackTexture.dispose();
        // La fuente no la liberamos aquí, porque es la misma del HUD del GameScreen
    }
}
