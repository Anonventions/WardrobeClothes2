package org.wardrobeclothes;


import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.wardrobeclothes.WardrobeClothes;

import java.awt.image.RenderedImage;
import java.io.FileReader;
import java.io.FileWriter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Getter
@RequiredArgsConstructor
public class SkinManager {
    private final WardrobeClothes plugin;
    private final ProtocolManager protocolManager;
    private final Map<String, String> skins = new ConcurrentHashMap<>();
    private File playerSkinFile;
    private File playerSkinFolder;
    private RenderedImage skinImage;

    public void applySkin(Player player, String skinName) {
        if (!skins.containsKey(skinName)) {
            plugin.getLogger().log(Level.WARNING, "Skin not found: " + skinName);
            player.sendMessage("Skin not found.");
            return;
        }

        String currentPlayerSkinFileName = loadPlayerSkinData(player);
        if (currentPlayerSkinFileName != null) {
            playerSkinFile = new File(playerSkinFolder, currentPlayerSkinFileName);
        }
        String skinValue = skins.get(skinName);
        plugin.getLogger().log(Level.INFO, "Applying skin: " + skinName + " to player " + player.getName());

        if (protocolManager == null) {
            plugin.getLogger().log(Level.SEVERE, "ProtocolManager is null!");
            return;
        }

        String mergedSkinFileName = UUID.randomUUID().toString() + ".png";
        playerSkinFile = new File(playerSkinFolder, mergedSkinFileName);
        try {
            ImageIO.write(skinImage, "png", playerSkinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        savePlayerSkinData(player, mergedSkinFileName);

        // Added: Load the player's existing skin and merge with the new one
        File playerSkinFolder = new File(plugin.getDataFolder() + File.separator + "player_skins");
        if (!playerSkinFolder.exists()) {
            boolean mkdirs = playerSkinFolder.mkdirs();
        }

        File playerSkinFile = new File(playerSkinFolder, player.getUniqueId() + ".png");
        BufferedImage skinImage = null;

        try {
            skinImage = ImageIO.read(playerSkinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedImage newSkinImage = null;
        try {
            newSkinImage = ImageIO.read(new File(plugin.getDataFolder() + File.separator + "skins", skinName + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (skinImage != null && newSkinImage != null) {
            skinImage = mergeSkinImages(skinImage, newSkinImage);
        }


        try {
            ImageIO.write(skinImage, "png", playerSkinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(skinImage, "png", outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        skinValue = Base64.getEncoder().encodeToString(outputStream.toByteArray());

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

        WrappedGameProfile originalProfile = WrappedGameProfile.fromPlayer(player);
        WrappedGameProfile newProfile = new WrappedGameProfile(originalProfile.getUUID(), originalProfile.getName());
        newProfile.getProperties().putAll(originalProfile.getProperties());
        newProfile.getProperties().put("textures", new WrappedSignedProperty("textures", skinValue, ""));

        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(new PlayerInfoData(newProfile, 1, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(player.getName()))));
        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            try {
                protocolManager.sendServerPacket(onlinePlayer, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private BufferedImage mergeSkinImages(BufferedImage baseImage, BufferedImage overlayImage) {
        int width = baseImage.getWidth();
        int height = baseImage.getHeight();


        BufferedImage mergedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);


        Graphics g = mergedImage.getGraphics();
        g.drawImage(baseImage, 0, 0, null);


        g.drawImage(overlayImage, 0, 0, null);
        g.dispose();

        return mergedImage;
    }

    private void savePlayerSkinData(Player player, String skinFileName) {
        
        File playerDataFolder = new File(plugin.getDataFolder() + File.separator + "player_data");
        if (!playerDataFolder.exists()) {
            boolean mkdirs = playerDataFolder.mkdirs();
        }

        File playerDataFile = new File(playerDataFolder, player.getUniqueId() + ".json");

        JsonObject playerData = new JsonObject();
        playerData.addProperty("skinFileName", skinFileName);

        try (FileWriter writer = new FileWriter(playerDataFile)) {
            new Gson().toJson(playerData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadPlayerSkinData(Player player) {
        File playerDataFolder = new File(plugin.getDataFolder() + File.separator + "player_data");
        File playerDataFile = new File(playerDataFolder, player.getUniqueId() + ".json");

        if (!playerDataFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(playerDataFile)) {
            JsonObject playerData = new Gson().fromJson(reader, JsonObject.class);
            return playerData.get("skinFileName").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }



    public void loadSkins() {
        File skinFolder = new File(plugin.getDataFolder(), "skins");
        if (!skinFolder.exists()) {
            boolean mkdirs = skinFolder.mkdirs();
        }


        for (File file : Objects.requireNonNull(skinFolder.listFiles())) {
            try {
                String skinName = file.getName().replace(".png", "").replace(".jpg", "");
                plugin.getLogger().log(Level.INFO, "Loading skin: " + skinName);
                BufferedImage skinImage = ImageIO.read(file);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(skinImage, "png", outputStream);
                String skinValue = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                skins.put(skinName, skinValue);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading skin file: " + file.getName(), e);
            }
        }
    }
}

