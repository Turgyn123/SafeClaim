package net.safeclaim;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimManager {
    private final Map<UUID, ClaimSelection> selections = new HashMap<>();
    private final Map<UUID, Map<String, Claim>> claims = new HashMap<>();

    // Add a location to the player's selection
    public void addSelection(UUID playerId, Location location) {
        ClaimSelection selection = selections.getOrDefault(playerId, new ClaimSelection());
        selection.addLocation(location);
        selections.put(playerId, selection);
    }

    // Get the player's selection
    public ClaimSelection getSelection(UUID playerId) {
        return selections.get(playerId);
    }

    // Create a claim with the selected area
    public boolean createClaim(UUID playerId, String name, Location corner1, Location corner2) {
        // Check if the area is already claimed
        for (Map<String, Claim> playerClaims : claims.values()) {
            for (Claim claim : playerClaims.values()) {
                if (claim.overlaps(corner1, corner2)) {
                    return false;
                }
            }
        }

        Claim claim = new Claim(name, corner1, corner2);
        claims.computeIfAbsent(playerId, k -> new HashMap<>()).put(name, claim);
        selections.remove(playerId); // Clear the selection after creating the claim
        return true;
    }

    // Delete a claim
    public boolean deleteClaim(UUID playerId, String name) {
        Map<String, Claim> playerClaims = claims.get(playerId);
        if (playerClaims == null) {
            return false;
        }
        return playerClaims.remove(name) != null;
    }

    // Get a specific claim by name
    public Claim getClaim(UUID playerId, String name) {
        Map<String, Claim> playerClaims = claims.get(playerId);
        if (playerClaims == null) {
            return null;
        }
        return playerClaims.get(name);
    }

    // Get all claims for a player
    public Map<String, Claim> getPlayerClaims(UUID playerId) {
        return claims.get(playerId);
    }

    // Get all claims (for saving to file)
    public Map<UUID, Map<String, Claim>> getClaims() {
        return claims;
    }

    // Check if a location is inside any claim
    public boolean isInClaim(Location location) {
        for (Map<String, Claim> playerClaims : claims.values()) {
            for (Claim claim : playerClaims.values()) {
                if (claim.contains(location)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Inner class to represent a claim selection
    public static class ClaimSelection {
        private Location firstCorner;
        private Location secondCorner;

        // Add a location to the selection
        public void addLocation(Location location) {
            if (firstCorner == null) {
                firstCorner = location;
            } else {
                secondCorner = location;
            }
        }

        // Check if the selection is complete (both corners are set)
        public boolean isComplete() {
            return firstCorner != null && secondCorner != null;
        }

        // Get the first corner
        public Location getFirstCorner() {
            return firstCorner;
        }

        // Get the second corner
        public Location getSecondCorner() {
            return secondCorner;
        }
    }

    // Inner class to represent a claim
    public static class Claim {
        private final String name;
        private final Location firstCorner;
        private final Location secondCorner;
        private final int heightLimit = 320; // Maximum Y-level (height limit)
        private final int depthLimit = -64;  // Minimum Y-level (depth limit)

        public Claim(String name, Location firstCorner, Location secondCorner) {
            this.name = name;
            this.firstCorner = firstCorner;
            this.secondCorner = secondCorner;
        }

        // Check if a location is inside this claim
        public boolean contains(Location location) {
            if (!location.getWorld().equals(firstCorner.getWorld())) {
                return false;
            }

            double minX = Math.min(firstCorner.getX(), secondCorner.getX());
            double maxX = Math.max(firstCorner.getX(), secondCorner.getX());
            double minY = depthLimit; // Extend claim to the depth limit
            double maxY = heightLimit; // Extend claim to the height limit
            double minZ = Math.min(firstCorner.getZ(), secondCorner.getZ());
            double maxZ = Math.max(firstCorner.getZ(), secondCorner.getZ());

            return location.getX() >= minX && location.getX() <= maxX &&
                    location.getY() >= minY && location.getY() <= maxY &&
                    location.getZ() >= minZ && location.getZ() <= maxZ;
        }

        // Check if this claim overlaps with another area
        public boolean overlaps(Location corner1, Location corner2) {
            if (!corner1.getWorld().equals(firstCorner.getWorld())) {
                return false;
            }

            double thisMinX = Math.min(firstCorner.getX(), secondCorner.getX());
            double thisMaxX = Math.max(firstCorner.getX(), secondCorner.getX());
            double thisMinY = depthLimit; // Extend claim to the depth limit
            double thisMaxY = heightLimit; // Extend claim to the height limit
            double thisMinZ = Math.min(firstCorner.getZ(), secondCorner.getZ());
            double thisMaxZ = Math.max(firstCorner.getZ(), secondCorner.getZ());

            double otherMinX = Math.min(corner1.getX(), corner2.getX());
            double otherMaxX = Math.max(corner1.getX(), corner2.getX());
            double otherMinY = depthLimit; // Other claim also extends to the depth limit
            double otherMaxY = heightLimit; // Other claim also extends to the height limit
            double otherMinZ = Math.min(corner1.getZ(), corner2.getZ());
            double otherMaxZ = Math.max(corner1.getZ(), corner2.getZ());

            return thisMinX <= otherMaxX && thisMaxX >= otherMinX &&
                    thisMinY <= otherMaxY && thisMaxY >= otherMinY &&
                    thisMinZ <= otherMaxZ && thisMaxZ >= otherMinZ;
        }

        public Location getFirstCorner() {
            return firstCorner;
        }

        public Location getSecondCorner() {
            return secondCorner;
        }
    }
}

