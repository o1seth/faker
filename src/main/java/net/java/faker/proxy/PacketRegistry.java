/*
 * This file is part of faker - https://github.com/o1seth/faker
 * Copyright (C) 2024 o1seth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.java.faker.proxy;

import net.java.faker.proxy.packet.*;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.constants.MCVersion;
import net.raphimc.netminecraft.packet.registry.DefaultPacketRegistry;

public class PacketRegistry extends DefaultPacketRegistry {
    public PacketRegistry(boolean isClientside, int protocolVersion) {
        super(isClientside, protocolVersion);
        registerPacket(MCPackets.C2S_MOVE_PLAYER_STATUS_ONLY, C2SMovePlayer.Status::new);
        registerPacket(MCPackets.C2S_MOVE_PLAYER_ROT, C2SMovePlayer.Rot::new);
        registerPacket(MCPackets.C2S_MOVE_PLAYER_POS, C2SMovePlayer.Pos::new);
        registerPacket(MCPackets.C2S_MOVE_PLAYER_POS_ROT, C2SMovePlayer.PosRot::new);
        registerPacket(MCPackets.C2S_SET_CARRIED_ITEM, C2SSetCarriedItem::new);
        registerPacket(MCPackets.C2S_PLAYER_COMMAND, C2SPlayerCommand::new);
        registerPacket(MCPackets.S2C_SET_ENTITY_MOTION, S2CSetEntityMotion::new);

        if (protocolVersion < MCVersion.v1_17) {
            registerPacket(MCPackets.S2C_DESTROY_ENTITIES, S2CDestroyEntities::new);
        } else if (protocolVersion == MCVersion.v1_17) {
            registerPacket(MCPackets.S2C_REMOVE_ENTITY, S2CRemoveEntity::new);
        } else {
            registerPacket(MCPackets.S2C_REMOVE_ENTITIES, S2CDestroyEntities::new);
        }

        if (protocolVersion < MCVersion.v1_9) {
            //also exists on 1.9+, but in proxy only needed for mount vehicle
            registerPacket(MCPackets.S2C_SET_ENTITY_LINK, S2CEntityAttach::new);
        }
        if (protocolVersion >= MCVersion.v1_17) {
            registerPacket(MCPackets.C2S_PONG, C2SPong::new);
            registerPacket(MCPackets.S2C_PING, S2CPing::new);
        } else {
            registerPacket(MCPackets.C2S_WINDOW_CONFIRMATION, C2SWindowConfirmation::new);
        }

        if (protocolVersion >= MCVersion.v1_21_2) {
            registerPacket(MCPackets.S2C_ENTITY_POSITION_SYNC, S2CEntityPositionSync::new);
        } else {
            registerPacket(MCPackets.S2C_TELEPORT_ENTITY, S2CEntityPositionSync::new);
        }
        if (protocolVersion >= MCVersion.v1_9) {
            registerPacket(MCPackets.C2S_MOVE_VEHICLE, C2SMoveVehicle::new);
            registerPacket(MCPackets.S2C_SET_PASSENGERS, S2CSetPassengers::new);
        }
        if (protocolVersion >= MCVersion.v1_21_2) {
            registerPacket(MCPackets.S2C_SET_HELD_SLOT, S2CSetCarriedItem::new);
        } else {
            registerPacket(MCPackets.S2C_SET_CARRIED_ITEM, S2CSetCarriedItem::new);
        }
        registerPacket(MCPackets.C2S_CONTAINER_CLOSE, C2SContainerClose::new);
        registerPacket(MCPackets.S2C_CONTAINER_CLOSE, S2CContainerClose::new);
        if (protocolVersion >= MCVersion.v1_21_2) {
            registerPacket(MCPackets.S2C_PLAYER_POSITION, S2CPlayerPosition.v1_21_2::new);
            registerPacket(MCPackets.S2C_PLAYER_ROTATION, S2CPlayerRotation::new);
        } else if (protocolVersion >= MCVersion.v1_19_4) {
            registerPacket(MCPackets.S2C_PLAYER_POSITION, S2CPlayerPosition.v1_19_4::new);
        } else if (protocolVersion >= MCVersion.v1_17) {
            registerPacket(MCPackets.S2C_PLAYER_POSITION, S2CPlayerPosition.v1_17::new);
        } else if (protocolVersion >= MCVersion.v1_9) {
            registerPacket(MCPackets.S2C_PLAYER_POSITION, S2CPlayerPosition.v1_9::new);
        } else if (protocolVersion >= MCVersion.v1_7_2) {
            registerPacket(MCPackets.S2C_PLAYER_POSITION, S2CPlayerPosition.v1_7_2::new);
        }
    }
}
