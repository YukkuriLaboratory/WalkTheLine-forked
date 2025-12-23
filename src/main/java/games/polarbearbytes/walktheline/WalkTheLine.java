package games.polarbearbytes.walktheline;

import games.polarbearbytes.walktheline.axis.AxisLockManager;
import games.polarbearbytes.walktheline.command.WTLCommands;
import games.polarbearbytes.walktheline.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkTheLine implements ModInitializer {
	public static final String MOD_ID = "walk-the-line";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static MinecraftServer server = null;

	@Override
	public void onInitialize() {
		ConfigManager.loadConfig();
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> WalkTheLine.server = server);
        ServerWorldEvents.LOAD.register((server, world) -> WalkTheLine.server = server);
        AxisLockManager.register();
        WTLCommands.register();
	}
}