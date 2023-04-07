package org.wardrobeclothes;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import com.comphenix.protocol.ProtocolLibrary;

import java.util.Objects;


@Getter
public class WardrobeClothes extends JavaPlugin {
    private SkinManager skinManager;

    @Override
    public void onEnable() {
        getServer().getScheduler().runTask(this, () -> {
            skinManager = new SkinManager(this, ProtocolLibrary.getProtocolManager());
            skinManager.loadSkins();
            Objects.requireNonNull(getCommand("clothes")).setExecutor(new ClothesCommand(skinManager));
        });
    }
}
