package games.polarbearbytes.walktheline;

import games.polarbearbytes.walktheline.movement.AxisLockManager;
import games.polarbearbytes.walktheline.network.OtherPlayerSyncPacket;
import games.polarbearbytes.walktheline.network.SyncPacket;
import games.polarbearbytes.walktheline.state.PlayerState;
import games.polarbearbytes.walktheline.util.PosUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

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
        PayloadTypeRegistry.playS2C().register(OtherPlayerSyncPacket.PAYLOAD_ID, OtherPlayerSyncPacket.PACKET_CODEC);

        /*
        Our command for enabling / disabling the mod for a save
        Probably change to a keybinding maybe, also locking the mod
        to enabled when the mod is first enabled to prevent players
        from being able to disable mod, do stuff and then re-enable
        */

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
                                            var playerState = PlayerState.get();
                                            playerState.setEnabled(player, true, false);
                                            var data = playerState.getLockedAxisData(player); // might be null
                                            server.getPlayerManager()
                                                    .getPlayerList()
                                                    .stream()
                                                    .filter((p) -> p != player && !PlayerState.get().getEnabled(p))
                                                    .forEach((otherPlayer) -> {
                                                        PlayerState.get().setEnabled(otherPlayer, true, false);
                                                    });

                                            // get other player's data
                                            var optionalOtherPlayer = server.getPlayerManager().getPlayerList().stream().filter(p -> p!=player).findFirst();
                                            if(optionalOtherPlayer.isEmpty()) {
                                                WalkTheLine.LOGGER.warn("Other player wasn't found!");
                                                return 0;
                                            }
                                            var otherData = PlayerState.get().getLockedAxisData(optionalOtherPlayer.get());

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
                                            PlayerState.get().setEnabled(ctx.getSource().getPlayer(), false);
                                            ctx.getSource().sendFeedback(() -> Text.literal("Walk the Line disabled."), false);
                                            return 1;
                                        })
                                )
                );
            }
        });
    }
}
