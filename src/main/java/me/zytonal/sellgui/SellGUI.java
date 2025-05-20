package me.zytonal.sellgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SellGUI extends JavaPlugin implements Listener {
    private Economy economy;
    private FileConfiguration config;
    private final Map<Material, Double> itemPrices = new HashMap<>();
    private final Set<UUID> inSellGUI = new HashSet<>();
    private Component guiTitle;
    private Component prefix;
    private final LegacyComponentSerializer legacyAmpersand = LegacyComponentSerializer.legacy('&');

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        
        if (!setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SellGUI has been enabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void loadConfig() {
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        
        // Load messages
        String prefixText = config.getString("messages.prefix", "&7[&eSellGUI&7] &r");
        String titleText = config.getString("gui.title", "&eSell Your Items");
        
        // Parse color codes using legacy formatter
        prefix = legacyAmpersand.deserialize(prefixText);
        guiTitle = legacyAmpersand.deserialize(titleText);
        
        // Debug output
        getLogger().info("Prefix: " + prefixText);
        getLogger().info("Title: " + titleText);
        
        // Load item prices
        itemPrices.clear();
        ConfigurationSection generalItems = config.getConfigurationSection("category.general-item.item-price");
        if (generalItems != null) {
            for (String itemKey : generalItems.getKeys(false)) {
                try {
                    Material material = Material.matchMaterial(itemKey);
                    if (material == null) continue;
                    
                    double price = generalItems.getDouble(itemKey + ".price-per-unit", 0);
                    if (price > 0) {
                        itemPrices.put(material, price);
                    }
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid material in config: " + itemKey);
                }
            }
        }
        getLogger().info("Loaded " + itemPrices.size() + " sellable items");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix.append(Component.text("This command can only be used by players!")
                    .color(NamedTextColor.RED)));
            return true;
        }

        Player player = (Player) sender;
        openSellGUI(player);
        return true;
    }

    private void openSellGUI(Player player) {
        int size = 54; // 6 rows (6 * 9 = 54 slots)
        Inventory gui = Bukkit.createInventory(null, size, guiTitle);
        player.openInventory(gui);
        inSellGUI.add(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        
        if (!inSellGUI.remove(player.getUniqueId())) {
            return; // Not our GUI
        }

        Inventory inventory = event.getInventory();
        double totalEarned = 0;
        int itemsSold = 0;
        Map<ItemStack, Integer> returnItems = new HashMap<>();

        // Process all items in the GUI
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            double pricePerUnit = itemPrices.getOrDefault(item.getType(), 0.0);
            if (pricePerUnit > 0) {
                int amount = item.getAmount();
                totalEarned += pricePerUnit * amount;
                itemsSold += amount;
            } else {
                // Return items that can't be sold
                returnItems.merge(item, item.getAmount(), Integer::sum);
            }
        }

        // Deposit money if any items were sold
        if (totalEarned > 0) {
            economy.depositPlayer(player, totalEarned);
            String message = config.getString("messages.sold-items", "&aYou sold &e%amount% &aitems for &e$%price%&a!")
                    .replace("%amount%", String.valueOf(itemsSold))
                    .replace("%price%", String.format("%.2f", totalEarned));
            player.sendMessage(legacyAmpersand.deserialize(message));
        } else if (itemsSold == 0) {
            String message = config.getString("messages.no-items-sold", "&cNo items were sold.");
            player.sendMessage(legacyAmpersand.deserialize(message));
        }

        // Return items that couldn't be sold
        if (!returnItems.isEmpty()) {
            player.getInventory().addItem(returnItems.entrySet().stream()
                    .map(entry -> {
                        ItemStack item = entry.getKey().clone();
                        item.setAmount(entry.getValue());
                        return item;
                    })
                    .toArray(ItemStack[]::new));
            
            player.sendMessage(prefix.append(legacyAmpersand.deserialize("&eSome items were returned as they cannot be sold.")));
        }
    }
}
