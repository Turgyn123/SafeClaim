package net.safeclaim;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ClaimListener implements Listener {

    private final Main plugin;
    private final ClaimManager claimManager;

    public ClaimListener(Main plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the item is the claim tool
        if (item != null && item.isSimilar(Main.createClaimTool())) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Location location = event.getClickedBlock().getLocation();
                claimManager.addSelection(player.getUniqueId(), location);
                player.sendMessage(ChatColor.GREEN + "Position set!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (claimManager.isInClaim(event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks in a safe claim!");
        }
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (claimManager.isInClaim(event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot damage blocks in a safe claim!");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();
        Location entityLocation = entity.getLocation();

        // Prevent entities from damaging players or other entities in a claim
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (claimManager.isInClaim(entityLocation)) {
                event.setCancelled(true);
            }
        } else if (damager instanceof Player) {
            Player player = (Player) damager;
            if (claimManager.isInClaim(player.getLocation())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot damage entities in a safe claim!");
            }
        } else if (damager instanceof Projectile) {
            // Prevent projectiles (e.g., arrows, fireballs) from damaging entities in a claim
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                if (claimManager.isInClaim(shooter.getLocation())) {
                    event.setCancelled(true);
                    shooter.sendMessage(ChatColor.RED + "You cannot damage entities in a safe claim!");
                }
            } else if (claimManager.isInClaim(entityLocation)) {
                event.setCancelled(true);
            }
        } else {
            // Prevent entities from damaging other entities in a claim
            if (claimManager.isInClaim(entityLocation)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        Location entityLocation = entity.getLocation();

        // Prevent entities from being damaged in a claim
        if (claimManager.isInClaim(entityLocation)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        // Prevent explosions (e.g., fireballs, TNT) from damaging blocks in a claim
        event.blockList().removeIf(block -> claimManager.isInClaim(block.getLocation()));
    }
}
