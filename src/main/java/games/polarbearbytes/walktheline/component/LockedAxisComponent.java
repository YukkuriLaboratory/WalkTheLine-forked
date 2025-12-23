package games.polarbearbytes.walktheline.component;

import com.mojang.serialization.Codec;
import games.polarbearbytes.walktheline.state.LockedAxisData;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashMap;
import java.util.UUID;

public class LockedAxisComponent implements Component, AutoSyncedComponent {
    private final World world;
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

    public void setLockedAxisData(UUID playerUuid, LockedAxisData data) {
        axisMap.put(playerUuid, data);
        WTLComponents.LOCKED_AXIS.sync(world);
    }
}
