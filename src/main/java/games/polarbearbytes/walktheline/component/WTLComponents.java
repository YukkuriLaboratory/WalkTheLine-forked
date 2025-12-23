package games.polarbearbytes.walktheline.component;

import games.polarbearbytes.walktheline.WalkTheLine;
import games.polarbearbytes.walktheline.state.LockedAxisData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

public final class WTLComponents implements WorldComponentInitializer, EntityComponentInitializer {
    public static final ComponentKey<LockedAxisComponent> LOCKED_AXIS =
            ComponentRegistry.getOrCreate(Identifier.of(WalkTheLine.MOD_ID, "locked_axis"), LockedAxisComponent.class);

    public static final ComponentKey<PlayerStateComponent> PLAYER_STATE =
            ComponentRegistry.getOrCreate(Identifier.of(WalkTheLine.MOD_ID, "player_state"), PlayerStateComponent.class);

    public static LockedAxisComponent lockedAxis(World provider) {
        return LOCKED_AXIS.get(provider);
    }

    public static LockedAxisData lockedAxisData(PlayerEntity player) {
        return LOCKED_AXIS.get(player.getEntityWorld()).getLockedAxisData(player.getUuid());
    }

    public static PlayerStateComponent playerState(PlayerEntity player) {
        return PLAYER_STATE.get(player);
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
        registry.register(LOCKED_AXIS, LockedAxisComponent::new);
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(PLAYER_STATE, PlayerStateComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }
}
