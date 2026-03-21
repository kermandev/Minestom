package net.minestom.server.listener;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.BoatMeta;
import net.minestom.server.network.packet.client.play.ClientSteerBoatPacket;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;

public class PlayerVehicleListener {

    public static void vehicleMoveListener(ClientVehicleMovePacket packet, Player player) {
        final Entity vehicle = player.getVehicle();
        if (vehicle == null)
            return;

        vehicle.refreshPosition(packet.position());
    }

    public static void boatSteerListener(ClientSteerBoatPacket packet, Player player) {
        final Entity vehicle = player.getVehicle();
        /* The packet may have been received after already exiting the vehicle. */
        if (vehicle == null) return;
        if (!(vehicle.getEntityMeta() instanceof BoatMeta boat)) return;
        // Only send the metadata packet if there are changes
        if (boat.isLeftPaddleTurning() != packet.leftPaddleTurning()) {
            boat.setLeftPaddleTurning(packet.leftPaddleTurning());
        }
        if (boat.isRightPaddleTurning() != packet.rightPaddleTurning()) {
            boat.setRightPaddleTurning(packet.rightPaddleTurning());
        }
    }
}