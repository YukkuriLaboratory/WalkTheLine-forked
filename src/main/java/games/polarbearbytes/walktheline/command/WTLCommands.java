package games.polarbearbytes.walktheline.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import games.polarbearbytes.walktheline.WalkTheLine;
import games.polarbearbytes.walktheline.axis.AxisLockManager;
import games.polarbearbytes.walktheline.component.WTLComponents;
import games.polarbearbytes.walktheline.world.PlayerPosAdjust;
import games.polarbearbytes.walktheline.world.StrongholdLocator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WTLCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Register only for server-side (both dedicated + integrated worlds)
            if (environment.integrated || environment.dedicated) {
                dispatcher.register(WALKTHELINE);
            }
        });
    }

    private static final LiteralArgumentBuilder<ServerCommandSource> WALKTHELINE = literal("walktheline")
            .then(literal("enable").then(argument("isprimary", BoolArgumentType.bool())
                            .executes(ctx -> {
                                WalkTheLine.LOGGER.info("Processing enable command...");
                                var player = ctx.getSource().getPlayer();
                                if (player == null) {
                                    WalkTheLine.LOGGER.warn("Command source player is null!");
                                    return 0;
                                }
                                var isPrimary = BoolArgumentType.getBool(ctx, "isprimary");
                                var playerState = WTLComponents.playerState(player);
                                playerState.setEnabled(true);

                                var world = player.getEntityWorld();
                                var newData = AxisLockManager.determineDimensionLocks(player, world.getRegistryKey(), isPrimary);
                                WTLComponents.lockedAxis(world).setLockedAxisData(player.getUuid(), newData);
                                PlayerPosAdjust.apply(player);

                                // feedback
                                ctx.getSource().sendFeedback(() -> Text.literal("Walk the Line enabled."), false);
                                return 1;
                            }))
            )
            .then(literal("disable")
                    .executes(ctx -> {
                        WTLComponents.playerState(ctx.getSource().getPlayer()).setEnabled(false);
                        ctx.getSource().sendFeedback(() -> Text.literal("Walk the Line disabled."), false);
                        return 1;
                    })
            )
            .then(literal("teleport-cross").executes(ctx -> {
                var lockedAxis = WTLComponents.lockedAxis(ctx.getSource().getWorld());
                var crossPoint = lockedAxis.getCrossPoint();
                if(crossPoint == null) {
                    ctx.getSource().sendFeedback(() -> Text.literal("Failed to teleport"), false);
                    return 1;
                }
                Vec3d pos = new Vec3d(crossPoint.getX(), crossPoint.getY(), crossPoint.getZ());
                var safeY = StrongholdLocator.WorldUtil.findSafeYAbove(ctx.getSource().getPlayer().getEntityWorld(), pos);
                ctx.getSource().getServer().getPlayerManager().getPlayerList().forEach(p -> {
                    p.requestTeleport(pos.x, safeY, pos.z);
                });
                ctx.getSource().sendFeedback(() -> Text.literal("Teleported! -> " + crossPoint.toShortString()), false);
                return 1;
            }))
            .then(literal("setcolor")
                    .then(argument("target", EntityArgumentType.player()).then(argument("color", ColorArgumentType.color())
                            .executes(ctx -> {
                                Formatting color = ColorArgumentType.getColor(ctx, "color");
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "target");
                                if(player == null) {
                                    ctx.getSource().sendFeedback(() -> Text.literal("Server Console wasn't supported."), false);
                                    return 1;
                                }

                                var playerUuid = ctx.getSource().getPlayer().getUuid();
                                var lockedAxis = WTLComponents.lockedAxis(ctx.getSource().getWorld());
                                var data = lockedAxis.getLockedAxisData(playerUuid);
                                if(data == null) return 1;
                                data.setColor(color);
                                lockedAxis.setLockedAxisData(playerUuid, data);
                                ctx.getSource().sendFeedback(() -> Text.translatable("walktheline.cmd.walktheline.setcolor.changed", ctx.getSource().getPlayer().getStringifiedName(), color.getName()), false);
                                return 1;
                            })))
            );
}
