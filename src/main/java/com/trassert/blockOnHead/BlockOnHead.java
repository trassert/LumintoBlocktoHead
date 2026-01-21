package com.trassert.blockOnHead;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class BlockOnHead extends JavaPlugin implements Listener {

    private Set<Material> allowedMaterials = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAllowedMaterials();

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("onhead").setExecutor(this);
        getCommand("onheadreload").setExecutor(this);

        getLogger().info("[BlockOnHead] Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("[BlockOnHead] Plugin disabled!");
    }

    private boolean useWhitelist = true;

    private boolean isAllowedHelmet(Material material) {
        if (!useWhitelist)
            return true;

        if (material.isArmor()) {
            ItemStack test = new ItemStack(material);
            return test.getEquipmentSlot() == org.bukkit.inventory.EquipmentSlot.HEAD;
        }

        if (material.name().endsWith("_HEAD") || material.name().endsWith("_SKULL") ||
                material == Material.PUMPKIN || material == Material.CARVED_PUMPKIN) {
            return true;
        }

        return allowedMaterials.contains(material);
    }

    private void reloadAllowedMaterials() {
        FileConfiguration config = getConfig();
        useWhitelist = config.getBoolean("use-whitelist", true);
        allowedMaterials = config.getStringList("allowed-items")
                .stream()
                .map(String::toUpperCase)
                .map(Material::getMaterial)
                .filter(m -> m != null && m.isItem())
                .collect(Collectors.toSet());
    }

    private String colorize(String msg) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        if (event.getSlot() == 39) {
            ItemStack clickedItem = event.getCursor();
            if (clickedItem == null || clickedItem.getType().isAir())
                return;

            if (!isAllowedHelmet(clickedItem.getType())) {
                event.setCancelled(true);
                player.sendMessage(colorize(getMessage("not-allowed")));
                return;
            }

            ItemStack helmetSlot = player.getInventory().getHelmet();
            event.setCancelled(true);
            player.getInventory().setHelmet(clickedItem);
            event.setCursor(helmetSlot != null ? helmetSlot : null);
        }
    }

    private String getMessage(String key) {
        return getConfig().getString("messages." + key, "&c[!] &7Unknown message: " + key);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("onhead")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(colorize(getMessage("players-only")));
                return true;
            }

            if (!player.hasPermission("lumintohead.use")) {
                player.sendMessage(colorize(getMessage("no-permission")));
                return true;
            }

            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType().isAir()) {
                player.sendMessage(colorize(getMessage("no-item-in-hand")));
                return true;
            }

            if (!isAllowedHelmet(clickedItem.getType())) {
                player.sendMessage(colorize(getMessage("not-allowed")));
                return true;
            }

            if (player.getInventory().getHelmet() != null && !player.getInventory().getHelmet().getType().isAir()) {
                player.sendMessage(colorize(getMessage("already-wearing")));
                return true;
            }

            player.getInventory().setHelmet(inHand);
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(colorize(getMessage("success")));
            return true;
        }

        if (command.getName().equalsIgnoreCase("onheadreload")) {
            if (!sender.hasPermission("lumintohead.reload")) {
                sender.sendMessage(colorize(getMessage("no-permission")));
                return true;
            }

            reloadConfig();
            reloadAllowedMaterials();
            sender.sendMessage(colorize(getMessage("reloaded")));
            return true;
        }

        return false;
    }
}