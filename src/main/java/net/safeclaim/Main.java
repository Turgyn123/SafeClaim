package net.safeclaim;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin implements Listener {

    private ClaimManager claimManager;
    private File claimsFile;

    @Override
    public void onEnable() {
        claimManager = new ClaimManager();
        claimsFile = new File(getDataFolder(), "claims.yml");

        // Load claims from file
        loadClaims();

        Bukkit.getPluginManager().registerEvents(new ClaimListener(this, claimManager), this);
        getLogger().info("SafeClaim has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save claims to file
        saveClaims();
        getLogger().info("SafeClaim has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("safeclaim")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;

            // Check if the player has OP status
            if (!player.isOp()) {
                player.sendMessage("§4§lYou do not have permission to use this command!");
                return true;
            }

            if (args.length == 0) {
                // Give the player the Golden Shovel
                player.getInventory().addItem(createClaimTool());
                player.sendMessage(ChatColor.GREEN + "You have been given the claim tool! Right-click two blocks to select the area.");
                return true;
            }

            if (args[0].equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /safeclaim delete <name>");
                    return true;
                }

                String name = args[1];
                if (claimManager.deleteClaim(player.getUniqueId(), name)) {
                    player.sendMessage(ChatColor.GREEN + "Claim '" + name + "' has been deleted.");
                } else {
                    player.sendMessage(ChatColor.RED + "Could not find a claim with the name '" + name + "'.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                Map<String, ClaimManager.Claim> playerClaims = claimManager.getPlayerClaims(player.getUniqueId());
                if (playerClaims == null || playerClaims.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You have no claims.");
                    return true;
                }

                player.sendMessage(ChatColor.GOLD + "Your Claims:");
                for (Map.Entry<String, ClaimManager.Claim> entry : playerClaims.entrySet()) {
                    String claimName = entry.getKey();
                    ClaimManager.Claim claim = entry.getValue();
                    Location corner1 = claim.getFirstCorner();
                    Location corner2 = claim.getSecondCorner();
                    player.sendMessage(ChatColor.GREEN + "- " + claimName + ": " +
                            "(" + corner1.getBlockX() + ", " + corner1.getBlockY() + ", " + corner1.getBlockZ() + ") to " +
                            "(" + corner2.getBlockX() + ", " + corner2.getBlockY() + ", " + corner2.getBlockZ() + ")");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("info")) {
                player.sendMessage(ChatColor.GOLD + "=== SafeClaim Info ===");
                player.sendMessage(ChatColor.GREEN + "1. Use /safeclaim to get the claim tool.");
                player.sendMessage(ChatColor.GREEN + "2. Right-click two blocks to select the corners of your claim.");
                player.sendMessage(ChatColor.GREEN + "3. Use /safeclaim name <name> to create the claim.");
                player.sendMessage(ChatColor.GREEN + "4. Use /safeclaim delete <name> to delete a claim.");
                player.sendMessage(ChatColor.GREEN + "5. Use /safeclaim list to view your claims.");
                player.sendMessage(ChatColor.GREEN + "6. Inside a claim, players cannot break blocks, damage entities, or be damaged.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /safeclaim name <name>");
                return true;
            }

            String name = args[1];
            ClaimManager.ClaimSelection selection = claimManager.getSelection(player.getUniqueId());
            if (selection == null || !selection.isComplete()) {
                player.sendMessage(ChatColor.RED + "You must select an area first using the claim tool.");
                return true;
            }

            if (claimManager.createClaim(player.getUniqueId(), name, selection.getFirstCorner(), selection.getSecondCorner())) {
                player.sendMessage(ChatColor.GREEN + "Claim '" + name + "' has been created!");
                visualizeClaim(player, claimManager.getClaim(player.getUniqueId(), name));
            } else {
                player.sendMessage(ChatColor.RED + "The selected area overlaps with an existing claim.");
            }
            return true;
        }
        return false;
    }

    // Visualize the claim using particles
    private void visualizeClaim(Player player, ClaimManager.Claim claim) {
        if (claim == null) return;

        Location corner1 = claim.getFirstCorner();
        Location corner2 = claim.getSecondCorner();

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        for (double x = minX; x <= maxX; x += 0.5) {
            for (double z = minZ; z <= maxZ; z += 0.5) {
                player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(corner1.getWorld(), x, minY, z), 1);
                player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(corner1.getWorld(), x, maxY, z), 1);
            }
        }

        for (double y = minY; y <= maxY; y += 0.5) {
            for (double x = minX; x <= maxX; x += 0.5) {
                player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(corner1.getWorld(), x, y, minZ), 1);
                player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(corner1.getWorld(), x, y, maxZ), 1);
            }
            for (double z = minZ; z <= maxZ; z += 0.5) {
                player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(corner1.getWorld(), minX, y, z), 1);
                player.spawnParticle(Particle.VILLAGER_HAPPY, new Location(corner1.getWorld(), maxX, y, z), 1);
            }
        }
    }

    // Static method to create the claim tool
    public static ItemStack createClaimTool() {
        ItemStack tool = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta meta = tool.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Claim Tool");
            tool.setItemMeta(meta);
        }
        return tool;
    }

    // Save claims to a YAML file
    private void saveClaims() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, ClaimManager.Claim>> entry : claimManager.getClaims().entrySet()) {
            UUID playerId = entry.getKey();
            Map<String, ClaimManager.Claim> playerClaims = entry.getValue();
            for (Map.Entry<String, ClaimManager.Claim> claimEntry : playerClaims.entrySet()) {
                String claimName = claimEntry.getKey();
                ClaimManager.Claim claim = claimEntry.getValue();
                String path = playerId.toString() + "." + claimName + ".";
                yaml.set(path + "world", claim.getFirstCorner().getWorld().getName());
                yaml.set(path + "x1", claim.getFirstCorner().getX());
                yaml.set(path + "y1", claim.getFirstCorner().getY());
                yaml.set(path + "z1", claim.getFirstCorner().getZ());
                yaml.set(path + "x2", claim.getSecondCorner().getX());
                yaml.set(path + "y2", claim.getSecondCorner().getY());
                yaml.set(path + "z2", claim.getSecondCorner().getZ());
            }
        }

        try {
            yaml.save(claimsFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save claims to file: " + e.getMessage());
        }
    }

    // Load claims from a YAML file
    private void loadClaims() {
        if (!claimsFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(claimsFile);
        for (String playerIdStr : yaml.getKeys(false)) {
            UUID playerId = UUID.fromString(playerIdStr);
            for (String claimName : yaml.getConfigurationSection(playerIdStr).getKeys(false)) {
                String path = playerIdStr + "." + claimName + ".";
                String worldName = yaml.getString(path + "world");
                double x1 = yaml.getDouble(path + "x1");
                double y1 = yaml.getDouble(path + "y1");
                double z1 = yaml.getDouble(path + "z1");
                double x2 = yaml.getDouble(path + "x2");
                double y2 = yaml.getDouble(path + "y2");
                double z2 = yaml.getDouble(path + "z2");

                Location corner1 = new Location(Bukkit.getWorld(worldName), x1, y1, z1);
                Location corner2 = new Location(Bukkit.getWorld(worldName), x2, y2, z2);
                claimManager.createClaim(playerId, claimName, corner1, corner2);
            }
        }
    }
}
