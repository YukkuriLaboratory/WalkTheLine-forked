package games.polarbearbytes.walktheline.component;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class PlayerStateComponent implements Component, AutoSyncedComponent {
    private final PlayerEntity player;
    private boolean enabled = false;

    public PlayerStateComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void readData(ReadView readView) {
        enabled = readView.getBoolean("enabled", false);
    }

    @Override
    public void writeData(WriteView writeView) {
        writeView.putBoolean("enabled", enabled);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        WTLComponents.PLAYER_STATE.sync(player);
    }
}
