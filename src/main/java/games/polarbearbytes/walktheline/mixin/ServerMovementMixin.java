package games.polarbearbytes.walktheline.mixin;

import games.polarbearbytes.walktheline.state.PlayerState;
import games.polarbearbytes.walktheline.util.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class ServerMovementMixin {
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void restrictMovement(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ServerPlayerEntity player && PlayerState.get().getEnabled(player)) {
            Utils.adjustmentPlayerPosition(player);
        }
    }
}
