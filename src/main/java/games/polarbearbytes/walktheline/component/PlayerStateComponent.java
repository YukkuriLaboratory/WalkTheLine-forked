package games.polarbearbytes.walktheline.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Formatting;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class PlayerStateComponent implements Component, AutoSyncedComponent {
    private final PlayerEntity player;
    private Formatting lineColor = Formatting.RED;
    private boolean enabled = false;
    private double coordTolerance = 0.5;

    public PlayerStateComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void readData(ReadView readView) {
        enabled = readView.getBoolean("enabled", false);
        coordTolerance = readView.getDouble("coord_to_lerance", 0.5);
        lineColor = readView.read("line_color", Formatting.CODEC).orElse(Formatting.RED);
    }

    @Override
    public void writeData(WriteView writeView) {
        writeView.putBoolean("enabled", enabled);
        writeView.putDouble("coord_to_lerance", coordTolerance);
        writeView.put("line_color", Formatting.CODEC, lineColor);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isEnabledWithWorld() {
        return this.enabled; // I know this is so bad. Sorry, I have no time to get better.
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        WTLComponents.PLAYER_STATE.sync(player);
    }

    public double getCoordTolerance() {
        return this.coordTolerance;
    }

    public void setCoordTolerance(double coordTolerance) {
        this.coordTolerance = coordTolerance;
        WTLComponents.PLAYER_STATE.sync(player);
    }

    public Formatting getLineColor() {
        return lineColor;
    }

    public void setLineColor(Formatting lineColor) {
        this.lineColor = lineColor;
        WTLComponents.PLAYER_STATE.sync(player);
    }
}
