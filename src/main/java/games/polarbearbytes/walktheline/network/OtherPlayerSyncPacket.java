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
import net.minecraft.world.World;

import static games.polarbearbytes.walktheline.WalkTheLine.MOD_ID;

public record OtherPlayerSyncPacket(RegistryKey<World> worldKey, LockedAxisData data, Boolean enabled) implements CustomPayload {
    public static final Identifier ID = Identifier.of(MOD_ID, "sync_other_locked_axis");
    public static final CustomPayload.Id<OtherPlayerSyncPacket> PAYLOAD_ID = new Id<>(ID);

    public static final PacketCodec<PacketByteBuf, OtherPlayerSyncPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.codec(RegistryKey.createCodec(RegistryKeys.WORLD)), OtherPlayerSyncPacket::worldKey,
            PacketCodecs.codec(LockedAxisData.CODEC), OtherPlayerSyncPacket::data,
            PacketCodecs.codec(Codec.BOOL), OtherPlayerSyncPacket::enabled,
            OtherPlayerSyncPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
