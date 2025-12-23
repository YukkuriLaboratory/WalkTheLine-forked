package games.polarbearbytes.walktheline;

import games.polarbearbytes.walktheline.component.WTLComponents;
import games.polarbearbytes.walktheline.movement.AxisLockManager;
import games.polarbearbytes.walktheline.network.SyncPacket;
import games.polarbearbytes.walktheline.util.PosUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * One stop shop for our events for events that are not class specific
 */
public class ServerEvents {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> WalkTheLine.server = server);
        ServerWorldEvents.LOAD.register((server, world) -> WalkTheLine.server = server);
        AxisLockManager.register();
        PayloadTypeRegistry.playS2C().register(SyncPacket.PAYLOAD_ID, SyncPacket.PACKET_CODEC);

        /*
        Our command for enabling / disabling the mod for a save
        Probably change to a keybinding maybe, also locking the mod
        to enabled when the mod is first enabled to prevent players
        from being able to disable mod, do stuff and then re-enable
        */

        // Todo: add change color system
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Register only for server-side (both dedicated + integrated worlds)
            if (environment.integrated || environment.dedicated) {
                dispatcher.register(
                        literal("walktheline")
                                .then(literal("enable")
                                        .executes(ctx -> {
                                            WalkTheLine.LOGGER.info("Processing enable command...");
                                            var player = ctx.getSource().getPlayer();
                                            if (player == null) {
                                                WalkTheLine.LOGGER.warn("Command source player is null!");
                                                return 0;
                                            }
                                            var server = ctx.getSource().getServer();
                                            var playerState = WTLComponents.playerState(player);
                                            playerState.setEnabled(true); // Todo: don't forget to apply false adjustment
//                                            playerState.setEnabled(player, true, false);
                                            var data = WTLComponents.lockedAxis(ctx.getSource().getWorld()).getLockedAxisData(player.getUuid());
                                            server.getPlayerManager()
                                                    .getPlayerList()
                                                    .stream()
                                                    .filter((p) -> p != player && !WTLComponents.playerState(p).isEnabled())
                                                    .forEach((otherPlayer) -> {
                                                        WTLComponents.playerState(otherPlayer).setEnabled(true);
                                                    });

                                            // get other player's data
                                            var optionalOtherPlayer = server.getPlayerManager().getPlayerList().stream().filter(p -> p!=player).findFirst();
                                            if(optionalOtherPlayer.isEmpty()) {
                                                WalkTheLine.LOGGER.warn("Other player wasn't found!");
                                                return 0;
                                            }
                                            var otherData = WTLComponents.lockedAxis(ctx.getSource().getWorld()).getLockedAxisData(optionalOtherPlayer.get().getUuid());

                                            // get safe y of cross point
                                            var crossX = data.coordinate();
                                            var crossZ = otherData.coordinate();
                                            var crossVec = new Vec3d(crossX, 65, crossZ);
                                            var crossY = PosUtil.findSafeYAbove(player, crossVec);

                                            // teleport all players to cross point
                                            var tpVec = new Vec3d(crossX, crossY, crossZ);
                                            WalkTheLine.LOGGER.info("Teleport all players to {}", tpVec);
                                            server.getPlayerManager().getPlayerList().forEach(p -> {
                                                p.teleport(crossX, crossY, crossZ, true);
                                            });

                                            // feedback
                                            ctx.getSource().sendFeedback(() -> Text.literal("Walk the Line enabled."), false);
                                            return 1;
                                        })
                                )
                                .then(literal("disable")
                                        .executes(ctx -> {
                                            WTLComponents.playerState(ctx.getSource().getPlayer()).setEnabled(false);
                                            ctx.getSource().sendFeedback(() -> Text.literal("Walk the Line disabled."), false);
                                            return 1;
                                        })
                                )
                                .then(literal("setcolor")
                                        .then(argument("target", EntityArgumentType.player()).then(argument("color", ColorArgumentType.color())
                                        .executes(ctx -> {
                                            Formatting color = ColorArgumentType.getColor(ctx, "color");
                                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                                            if(player == null) {
                                                ctx.getSource().sendFeedback(() -> Text.literal("Server Console wasn't supported."), false);
                                                return 1;
                                            }

                                            var playerUuid = ctx.getSource().getPlayer().getUuid();
                                            var lockedAxis = WTLComponents.lockedAxis(ctx.getSource().getWorld());
                                            var data = lockedAxis.getLockedAxisData(playerUuid);
                                            ctx.getSource().sendFeedback(() -> Text.translatable("walktheline.cmd.walktheline.setcolor.changed"), false);
                                            return 1;
                                        })))
                                )
                );
            }
        });
    }
}
