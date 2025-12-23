package games.polarbearbytes.walktheline.mixin.client;

import games.polarbearbytes.walktheline.component.WTLComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class ClientMovementMixin {
    @Inject(method = "move", at = @At("RETURN"))
    private void restrictMovement(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if(!(self instanceof PlayerEntity player) || !self.getEntityWorld().isClient() || !WTLComponents.playerState(player).isEnabled()) return;

        var data = WTLComponents.lockedAxisData(player);
        if(data == null) return;

        double x = self.getX();
        double z = self.getZ();
        double tolerance = WTLComponents.playerState(player).getCoordTolerance();

        if(data.axis() == Direction.Axis.X){
            if(x > data.coordinate() + tolerance) x = data.coordinate() + tolerance;
            if(x < data.coordinate() - tolerance) x = data.coordinate() - tolerance;
        } else {
            if(z > data.coordinate() + tolerance) z = data.coordinate() + tolerance;
            if(z < data.coordinate() - tolerance) z = data.coordinate() - tolerance;
        }
        self.setPos(x, self.getY(), z);
    }
}
