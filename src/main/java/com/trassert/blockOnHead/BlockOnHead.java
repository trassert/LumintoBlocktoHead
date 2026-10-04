package com.trassert.blockOnHead;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
    private boolean useWhitelist;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAllowedMaterials();

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("onhead") != null)
            getCommand("onhead").setExecutor(this);

        if (getCommand("onheadreload") != null)
            getCommand("onheadreload").setExecutor(this);

        getLogger().info("BlockOnHead enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BlockOnHead disabled!");
    }

    private void reloadAllowedMaterials() {
        useWhitelist = getConfig().getBoolean("use-whitelist", true);
        allowedMaterials.clear();

        for (String name : getConfig().getStringList("allowed-items")) {
            Material material = Material.getMaterial(name.toUpperCase(Locale.ROOT));

            if (material != null && material.isItem()) {
                allowedMaterials.add(material);
            }
        }
    }

    private boolean isAllowed(@NotNull Material material) {
        if (!useWhitelist) {
            return true;
        }

        String name = material.name();

        return name.endsWith("_HELMET")
                || name.endsWith("_HEAD")
                || name.endsWith("_SKULL")
                || material == Material.PUMPKIN
                || material == Material.CARVED_PUMPKIN
                || allowedMaterials.contains(material);
    }

    private @NotNull Component message(@NotNull String key) {
        return MINI_MESSAGE.deserialize(
                getConfig().getString(
                        "messages." + key,
                        "<red>[!] <gray>Unknown message: " + key + "</gray></red>"
                )
        );
    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() != 39
                || event.getView().getType() != InventoryType.PLAYER) {
            return;
        }

        ItemStack cursor = event.getView().getCursor();

        if (cursor.getType().isAir()) {
            return;
        }

        if (!isAllowed(cursor.getType())
                && !player.hasPermission("lumintohead.bypass")) {

            event.setCancelled(true);
            player.sendMessage(message("not-allowed"));
            return;
        }

        event.setCancelled(true);

        ItemStack currentHelmet = player.getInventory().getHelmet();

        player.getInventory().setHelmet(cursor.clone());

        event.getView().setCursor(
                currentHelmet == null || currentHelmet.getType().isAir()
                        ? null
                        : currentHelmet
        );
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "onhead" -> handleOnHead(sender);
            case "onheadreload" -> handleReload(sender);
            default -> false;
        };
    }

    private boolean handleOnHead(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message("players-only"));
            return true;
        }

        if (!player.hasPermission("lumintohead.use")) {
            player.sendMessage(message("no-permission"));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            player.sendMessage(message("no-item-in-hand"));
            return true;
        }

        if (!isAllowed(item.getType())
                && !player.hasPermission("lumintohead.bypass")) {

            player.sendMessage(message("not-allowed"));
            return true;
        }

        ItemStack helmet = player.getInventory().getHelmet();

        if (helmet != null && !helmet.getType().isAir()) {
            player.sendMessage(message("already-wearing"));
            return true;
        }

        player.getInventory().setHelmet(item);
        player.getInventory().setItemInMainHand(null);
        player.sendMessage(message("success"));

        return true;
    }

    private boolean handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("lumintohead.reload")) {
            sender.sendMessage(message("no-permission"));
            return true;
        }

        reloadConfig();
        reloadAllowedMaterials();

        sender.sendMessage(message("reloaded"));
        return true;
    }
}