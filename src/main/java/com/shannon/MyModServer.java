package com.shannon;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import com.shannon.network.packet.MessagePacket;
import com.shannon.network.packet.TaskTreeStatePacket;
import com.shannon.network.packet.TaskTreeState;

public class MyModServer {
    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                sendMessageToClient(player, "Welcome to the server, " + player.getName().getString() + "!");
            });
        });
    }

    public static void sendMessageToClient(ServerPlayerEntity player, String message) {
        ServerPlayNetworking.send(player, new MessagePacket(message));
    }

    public static void sendTaskTreeStateToClient(ServerPlayerEntity player, TaskTreeState state) {
        ServerPlayNetworking.send(player, new TaskTreeStatePacket(state));
    }
}
