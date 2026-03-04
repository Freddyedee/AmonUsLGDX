package com.amongus.core.view.voting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Carga y expone todas las TextureRegions del sprite sheet de votación.
 * Una sola responsabilidad: assets. No dibuja, no tiene estado.
 */
public class VotingAssets {

    public Texture panelTablet, panelCard, panelDead, panelInfo;
    public Texture btnCancel, btnConfirm, btnProceed;
    public Texture iconVoted, iconSkip, iconX, iconCheck;
    public Texture headLarge;

    public void load() {
        panelTablet = tex("ui/voting/panel_tablet.png");
        panelCard   = tex("ui/voting/panel_card.png");
        panelDead   = tex("ui/voting/panel_dead.png");
        panelInfo   = tex("ui/voting/panel_info.png");
        btnCancel   = tex("ui/voting/btn_cancel.png");
        btnConfirm  = tex("ui/voting/btn_confirm.png");
        btnProceed  = tex("ui/voting/btn_proceed.png");
        iconVoted   = tex("ui/voting/icon_voted.png");
        iconSkip    = tex("ui/voting/icon_skip.png");
        iconX       = tex("ui/voting/icon_x.png");
        iconCheck   = tex("ui/voting/icon_check.png");
        headLarge = tex("ui/voting/head_large.png");
    }

    private Texture tex(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    public void dispose() {
        panelTablet.dispose(); panelCard.dispose();
        panelDead.dispose();   panelInfo.dispose();
        btnCancel.dispose();   btnConfirm.dispose();
        btnProceed.dispose();  iconVoted.dispose();
        iconSkip.dispose();    iconX.dispose();
        iconCheck.dispose();
        headLarge.dispose();
    }
}


