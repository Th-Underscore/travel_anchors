package de.castcrafter.travelanchors.network;

import de.castcrafter.travelanchors.TeleportHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import org.moddingx.libx.network.PacketHandler;
import org.moddingx.libx.network.PacketSerializer;

import java.util.function.Supplier;

public record ShortTeleportPacket(InteractionHand hand, boolean keepVelocity) {

    public static class Serializer implements PacketSerializer<ShortTeleportPacket> {
        @Override
        public Class<ShortTeleportPacket> messageClass() {
            return ShortTeleportPacket.class;
        }

        @Override
        public void encode(ShortTeleportPacket msg, FriendlyByteBuf buffer) {
            buffer.writeEnum(msg.hand());
            buffer.writeBoolean(msg.keepVelocity());
        }

        @Override
        public ShortTeleportPacket decode(FriendlyByteBuf buffer) {
            return new ShortTeleportPacket(buffer.readEnum(InteractionHand.class), buffer.readBoolean());
        }
    }

    public static class Handler implements PacketHandler<ShortTeleportPacket> {
        @Override
        public Target target() {
            return Target.MAIN_THREAD;
        }

        @Override
        public boolean handle(ShortTeleportPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return true;
            }
            TeleportHandler.performShortTeleportOnServer(player.level(), player, msg.hand(), msg.keepVelocity());
            return true;
        }
    }
}
