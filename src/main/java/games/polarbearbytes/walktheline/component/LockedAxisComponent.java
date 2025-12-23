package games.polarbearbytes.walktheline.component;

import com.mojang.serialization.Codec;
import games.polarbearbytes.walktheline.axis.LockedAxisData;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashMap;
import java.util.UUID;

public class LockedAxisComponent implements Component, AutoSyncedComponent {
    private final World world;
    @Nullable private Vec3i crossPoint = null;
    private HashMap<UUID, LockedAxisData> axisMap = new HashMap<>();

    public LockedAxisComponent(World world) {
        this.world = world;
    }

    @Override
    public void readData(ReadView readView) {
        axisMap = new HashMap<>(readView.read("axis_map", Codec.unboundedMap(Uuids.CODEC, LockedAxisData.CODEC)).orElseGet(HashMap::new));
    }

    @Override
    public void writeData(WriteView writeView) {
        writeView.put("axis_map", Codec.unboundedMap(Uuids.CODEC, LockedAxisData.CODEC), axisMap);
    }

    @Nullable
    public LockedAxisData getLockedAxisData(UUID playerUuid) {
        return axisMap.get(playerUuid);
    }

    public void removeLocketAxisData(UUID playerUuid) {
        axisMap.remove(playerUuid);
        WTLComponents.LOCKED_AXIS.sync(world);
    }

    public void setLockedAxisData(UUID playerUuid, LockedAxisData data) {
        axisMap.put(playerUuid, data);
        WTLComponents.LOCKED_AXIS.sync(world);
    }

    @Nullable
    public Vec3i getCrossPoint() {
        return this.crossPoint;
    }

    public void setCrossPoint(@NonNull Vec3i crossPoint) {
        this.crossPoint = crossPoint;
        WTLComponents.LOCKED_AXIS.sync(world);
    }
}
