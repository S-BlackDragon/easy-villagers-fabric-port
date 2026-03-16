package de.maxhenkel.easyvillagers.network;

import de.maxhenkel.easyvillagers.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRerollPacket(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<C2SRerollPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Main.MODID, "reroll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRerollPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> buf.writeBlockPos(pkt.pos()),
            buf -> new C2SRerollPacket(buf.readBlockPos())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
