package games.polarbearbytes.walktheline.network;

import com.mojang.serialization.Codec;
import games.polarbearbytes.walktheline.state.LockedAxisData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;

import java.util.UUID;

import static games.polarbearbytes.walktheline.WalkTheLine.MOD_ID;

/**
 * Network Packet for syncing locked data and mod enabled flag
 *
 * @param worldKey The registry key for the world (dimension)
 * @param data The locked axis, coordinate data
 * @param enabled Is the mod enabled
 */
public record SyncPacket(UUID playerUuid, RegistryKey<World> worldKey, LockedAxisData data, Double coordTolerance, Boolean enabled)
        implements CustomPayload {
    public static final Identifier ID = Identifier.of(MOD_ID, "sync_locked_axis");
    public static final CustomPayload.Id<SyncPacket> PAYLOAD_ID = new CustomPayload.Id<>(ID);

    //Codec for serialization, deserialization our data for the client sync
    public static final PacketCodec<PacketByteBuf,SyncPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.codec(Uuids.CODEC), SyncPacket::playerUuid,
            PacketCodecs.codec(RegistryKey.createCodec(RegistryKeys.WORLD)), SyncPacket::worldKey,
            PacketCodecs.codec(LockedAxisData.CODEC), SyncPacket::data,
            PacketCodecs.DOUBLE, SyncPacket::coordTolerance,
            PacketCodecs.codec(Codec.BOOL), SyncPacket::enabled,
            SyncPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}