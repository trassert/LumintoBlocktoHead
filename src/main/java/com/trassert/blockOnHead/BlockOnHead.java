package com.trassert.blockOnHead;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class BlockOnHead extends JavaPlugin implements Listener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Set<Material> allowedMaterials = new HashSet<>();
    private boolean useWhitelist = true;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAllowedMaterials();

        Bukkit.getPluginManager().registerEvents(this, this);
        registerCommand("onhead");
        registerCommand("onheadreload");

        getLogger().info("[BlockOnHead] Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("[BlockOnHead] Plugin disabled!");
    }

    private void registerCommand(@NotNull String name) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(this);
        } else {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml");
        }
    }

    private boolean isAllowedHelmet(@NotNull Material material) {
        if (!useWhitelist) {
            return true;
        }

        String name = material.name();
        if (name.endsWith("_HELMET") || name.endsWith("_HEAD") || name.endsWith("_SKULL")
                || name.equals("PUMPKIN") || name.equals("CARVED_PUMPKIN")) {
            return true;
        }

        return allowedMaterials.contains(material);
    }

    private void reloadAllowedMaterials() {
        FileConfiguration config = getConfig();
        useWhitelist = config.getBoolean("use-whitelist", true);

        Set<Material> materials = new HashSet<>();
        for (String itemName : config.getStringList("allowed-items")) {
            if (itemName == null) {
                continue;
            }

            Material material = Material.getMaterial(itemName.toUpperCase(Locale.ROOT));
            if (material != null && material.isItem()) {
                materials.add(material);
            }
        }

        allowedMaterials.clear();
        allowedMaterials.addAll(materials);
    }

    private @NotNull Component getMessage(@NotNull String key) {
        String message = getConfig().getString("messages." + key);
        if (message == null) {
            message = "<red>[!] <gray>Unknown message: " + key + "</gray></red>";
        }
        return MINI_MESSAGE.deserialize(message);
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() != 39 || event.getView().getType() != InventoryType.PLAYER) {
            return;
        }

        ItemStack cursor = event.getView().getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return;
        }

        if (!isAllowedHelmet(cursor.getType()) && !player.hasPermission("lumintohead.bypass")) {
            event.setCancelled(true);
            player.sendMessage(getMessage("not-allowed"));
            return;
        }

        event.setCancelled(true);

        ItemStack currentHelmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(cursor.clone());

        if (currentHelmet == null || currentHelmet.getType().isAir()) {
            event.getView().setCursor(null);
        } else {
            event.getView().setCursor(currentHelmet);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("onhead")) {
            return handleOnHead(sender);
        }

        if (command.getName().equalsIgnoreCase("onheadreload")) {
            return handleOnHeadReload(sender);
        }

        return false;
    }

    private boolean handleOnHead(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getMessage("players-only"));
            return true;
        }

        if (!player.hasPermission("lumintohead.use")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType().isAir()) {
            player.sendMessage(getMessage("no-item-in-hand"));
            return true;
        }

        if (!isAllowedHelmet(inHand.getType()) && !player.hasPermission("lumintohead.bypass")) {
            player.sendMessage(getMessage("not-allowed"));
            return true;
        }

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && !helmet.getType().isAir()) {
            player.sendMessage(getMessage("already-wearing"));
            return true;
        }

        player.getInventory().setHelmet(inHand);
        player.getInventory().setItemInMainHand(null);
        player.sendMessage(getMessage("success"));
        return true;
    }

    private boolean handleOnHeadReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("lumintohead.reload")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        reloadConfig();
        reloadAllowedMaterials();
        sender.sendMessage(getMessage("reloaded"));
        return true;
    }
}