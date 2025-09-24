package me.lssupportteam.ipdynamic.utils;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pagination state for players viewing IP history
 */
public class PaginationManager {

    private static final int ITEMS_PER_PAGE = 5; // IPs per page
    private final Map<UUID, PlayerPaginationState> playerStates = new ConcurrentHashMap<>();

    /**
     * Creates or updates pagination state for a player
     */
    public void createPaginationState(Player player, String targetPlayer, Map<String, Integer> ipHistory) {
        UUID playerUuid = player.getUniqueId();
        PlayerPaginationState state = new PlayerPaginationState(targetPlayer, ipHistory, ITEMS_PER_PAGE);
        playerStates.put(playerUuid, state);
    }

    /**
     * Gets the current page data for a player
     */
    public PaginationData getCurrentPage(UUID playerUuid) {
        PlayerPaginationState state = playerStates.get(playerUuid);
        if (state == null) {
            return null;
        }
        return state.getCurrentPageData();
    }

    /**
     * Moves to next page if available
     */
    public boolean nextPage(UUID playerUuid) {
        PlayerPaginationState state = playerStates.get(playerUuid);
        if (state == null) {
            return false;
        }
        return state.nextPage();
    }

    /**
     * Moves to previous page if available
     */
    public boolean previousPage(UUID playerUuid) {
        PlayerPaginationState state = playerStates.get(playerUuid);
        if (state == null) {
            return false;
        }
        return state.previousPage();
    }

    /**
     * Removes pagination state for a player
     */
    public void clearState(UUID playerUuid) {
        playerStates.remove(playerUuid);
    }

    /**
     * Checks if player has pagination state
     */
    public boolean hasState(UUID playerUuid) {
        return playerStates.containsKey(playerUuid);
    }

    /**
     * Gets the target player name for pagination state
     */
    public String getTargetPlayer(UUID playerUuid) {
        PlayerPaginationState state = playerStates.get(playerUuid);
        return state != null ? state.targetPlayer : null;
    }

    /**
     * Internal class to hold player pagination state
     */
    private static class PlayerPaginationState {
        private final String targetPlayer;
        private final String[] ipArray;
        private final int[] connectionCounts;
        private final int itemsPerPage;
        private final int totalPages;
        private int currentPage;

        public PlayerPaginationState(String targetPlayer, Map<String, Integer> ipHistory, int itemsPerPage) {
            this.targetPlayer = targetPlayer;
            this.itemsPerPage = itemsPerPage;
            this.ipArray = ipHistory.keySet().toArray(new String[0]);
            this.connectionCounts = new int[ipArray.length];

            // Fill connection counts array
            for (int i = 0; i < ipArray.length; i++) {
                this.connectionCounts[i] = ipHistory.get(ipArray[i]);
            }

            this.totalPages = (int) Math.ceil((double) ipArray.length / itemsPerPage);
            this.currentPage = 1;
        }

        public PaginationData getCurrentPageData() {
            int startIndex = (currentPage - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, ipArray.length);

            Map<String, Integer> pageData = new HashMap<>();
            for (int i = startIndex; i < endIndex; i++) {
                pageData.put(ipArray[i], connectionCounts[i]);
            }

            return new PaginationData(pageData, currentPage, totalPages, hasNextPage(), hasPreviousPage());
        }

        public boolean nextPage() {
            if (hasNextPage()) {
                currentPage++;
                return true;
            }
            return false;
        }

        public boolean previousPage() {
            if (hasPreviousPage()) {
                currentPage--;
                return true;
            }
            return false;
        }

        private boolean hasNextPage() {
            return currentPage < totalPages;
        }

        private boolean hasPreviousPage() {
            return currentPage > 1;
        }
    }

    /**
     * Data class for pagination information
     */
    public static class PaginationData {
        private final Map<String, Integer> pageIps;
        private final int currentPage;
        private final int totalPages;
        private final boolean hasNext;
        private final boolean hasPrevious;

        public PaginationData(Map<String, Integer> pageIps, int currentPage, int totalPages, boolean hasNext, boolean hasPrevious) {
            this.pageIps = pageIps;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }

        public Map<String, Integer> getPageIps() {
            return pageIps;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public boolean hasNext() {
            return hasNext;
        }

        public boolean hasPrevious() {
            return hasPrevious;
        }
    }
}