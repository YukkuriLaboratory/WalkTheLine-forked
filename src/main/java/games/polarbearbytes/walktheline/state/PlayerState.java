package games.polarbearbytes.walktheline.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import games.polarbearbytes.walktheline.WalkTheLine;
import games.polarbearbytes.walktheline.util.Utils;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.Direction.Axis;

import static games.polarbearbytes.walktheline.movement.AxisLockManager.determineDimensionLocks;
import static games.polarbearbytes.walktheline.movement.AxisLockManager.syncToClient;

/**
 * Player state class for saving the locked axis, coordinate data per world (dimension), per save, per player
 */
public class PlayerState extends PersistentState {
    public static final Codec<PlayerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Uuids.CODEC,
                    SavesData.CODEC
            ).fieldOf("players").forGetter(PlayerState::getRawMap),
            Codec.unboundedMap(
                    Codec.STRING,
                    Axis.CODEC
            ).optionalFieldOf("primaryAxisBySave", new HashMap<>()).forGetter(PlayerState::getPrimaryAxisBySaveMap)
    ).apply(instance, PlayerState::new));

    public static final PersistentStateType<PlayerState> TYPE = new PersistentStateType<>("walk_the_line_state",PlayerState::new, CODEC, DataFixTypes.PLAYER);

    private HashMap<UUID, SavesData> playersData = new HashMap<>();
    private ConcurrentHashMap<String, Axis> primaryAxisBySave = new ConcurrentHashMap<>();

    public PlayerState() {}

    public PlayerState(Map<UUID, SavesData> playersData, Map<String, Axis> primaryAxisBySave) {
        this.playersData = new HashMap<>();
        this.playersData.putAll(playersData);
        this.primaryAxisBySave = new ConcurrentHashMap<>();
        this.primaryAxisBySave.putAll(primaryAxisBySave);
    }

    public static PlayerState get() {
        return WalkTheLine.server.getOverworld().getPersistentStateManager().getOrCreate(TYPE);
    }

    /*
    Overridden functions to allow for default parameters or allow specific values
     */
    public SavesData getPlayerSaves(ServerPlayerEntity player){
        return playersData.computeIfAbsent(player.getUuid(),(uuid)-> new SavesData(new ConcurrentHashMap<>()));
    }
    public WorldsData getWorldsData(ServerPlayerEntity player, String saveName){
        return getPlayerSaves(player).savesData().computeIfAbsent(saveName,(savedName)-> new WorldsData(new ConcurrentHashMap<>(),false));
    }
    public LockedAxisData getLockedAxisData(ServerPlayerEntity player, String saveName, RegistryKey<World> worldKey){
        return getWorldsData(player,saveName).worldData().computeIfAbsent(worldKey,(worlds)-> determineDimensionLocks(player,worldKey));
    }

    public WorldsData getWorldsData(ServerPlayerEntity player){
        String saveName = Objects.requireNonNull(player.getEntityWorld().getServer()).getSaveProperties().getLevelName();
        return getWorldsData(player,saveName);
    }
    public LockedAxisData getLockedAxisData(ServerPlayerEntity player){
        String saveName = Objects.requireNonNull(player.getEntityWorld().getServer()).getSaveProperties().getLevelName();
        RegistryKey<World> worldKey = player.getEntityWorld().getRegistryKey();
        return getLockedAxisData(player,saveName,worldKey);
    }

    /**
     * Called from teh Command event to enable. Gets the current locked axis data and
     * sets the enabled flag
     *
     * @param player Server entity representing the player
     * @param enabled Flag for wither or not the mod is enabled
     */
    public void setEnabled(ServerPlayerEntity player, Boolean enabled){
        MinecraftServer server = player.getEntityWorld().getServer();
        String saveName = server.getSaveProperties().getLevelName();
        getPlayerSaves(player).savesData().compute(saveName,
                (savedName,worldsData)->{
                    if(worldsData == null){
                        return new WorldsData(new ConcurrentHashMap<>(),enabled);
                    }
                    return new WorldsData(worldsData.worldData(),enabled);
                });
        markDirty();
        RegistryKey<World> key = player.getEntityWorld().getRegistryKey();
        LockedAxisData data = PlayerState.get().getLockedAxisData(player);
        if(data == null){
            return;
        }

        // If enabling the mod and no primary axis is set yet, set this player's axis as primary
        if(enabled && !hasPrimaryAxis(saveName)){
            setPrimaryAxis(saveName, data.axis());
        }

        syncToClient(player,key,data,enabled);

        Utils.adjustmentPlayerPosition(player);
    }

    public boolean getEnabled(ServerPlayerEntity player){
        return getWorldsData(player).enabled();
    }

    private Map<UUID, SavesData> getRawMap() {
        return this.playersData;
    }

    private Map<String, Axis> getPrimaryAxisBySaveMap() {
        return new HashMap<>(this.primaryAxisBySave);
    }

    /**
     * Get the primary axis for a save (the axis used by the first player who enabled the mod)
     *
     * @param saveName The name of the save
     * @return The primary axis, or null if not set
     */
    public Axis getPrimaryAxis(String saveName) {
        return primaryAxisBySave.get(saveName);
    }

    /**
     * Set the primary axis for a save
     *
     * @param saveName The name of the save
     * @param axis The axis to set as primary
     */
    public void setPrimaryAxis(String saveName, Axis axis) {
        primaryAxisBySave.put(saveName, axis);
        markDirty();
    }

    /**
     * Check if a primary axis has been set for a save
     *
     * @param saveName The name of the save
     * @return True if primary axis is set
     */
    public boolean hasPrimaryAxis(String saveName) {
        return primaryAxisBySave.containsKey(saveName);
    }
}