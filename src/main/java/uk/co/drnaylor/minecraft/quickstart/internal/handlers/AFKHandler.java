package uk.co.drnaylor.minecraft.quickstart.internal.handlers;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Identifiable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class AFKHandler {
    private final HashMap<UUID, AFKData> data = new HashMap<>();

    public void login(UUID user) {
        getData(user);
    }

    /**
     * Updates activity.
     * @return <code>true</code> if the user has returned from AFK.
     */
    public boolean updateUserActivity(UUID user) {
        return getData(user).update();
    }

    public void setNoTrack(UUID user, boolean track) {
        getData(user).noTrack = track;
    }

    public void setAFK(UUID user, boolean afk) {
        AFKData a = getData(user);
        a.isAFK = !a.noTrack && afk;
    }

    public void purgeNotOnline() {
        // CMEs BE GONE!
        List<UUID> uuid = new ArrayList<>(Sponge.getServer().getOnlinePlayers()).stream().map(Identifiable::getUniqueId).collect(Collectors.toList());
        data.keySet().stream().filter(x -> !uuid.contains(x)).forEach(data::remove);
    }

    public AFKData getAFKData(Player pl) {
        return getData(pl.getUniqueId());
    }

    public List<UUID> checkForAfk(int amount) {
        Instant now = Instant.now();
        return data.entrySet().stream()
                .filter(x -> !x.getValue().noTrack && !x.getValue().isAFK && x.getValue().lastActivity.plus(amount, ChronoUnit.SECONDS).isBefore(now))
                .map(x -> {
                    x.getValue().isAFK = true;
                    return x.getKey();
                }).collect(Collectors.toList());
    }

    public List<UUID> checkForAfkKick(int amount) {
        Instant now = Instant.now();
        return data.entrySet().stream()
                .filter(x -> !x.getValue().noTrack && x.getValue().lastActivity.plus(amount, ChronoUnit.SECONDS).isBefore(now))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private AFKData getData(UUID user) {
        if (!data.containsKey(user)) {
            AFKData a = new AFKData();
            data.put(user, a);
            return a;
        }

        return data.get(user);
    }

    public static class AFKData {
        private boolean isAFK = false;
        private boolean noTrack = false;
        private Instant lastActivity = Instant.now();
        private final Object lockingObject = new Object();

        /**
         * Updates activity.
         * @return <code>true</code> if the user has returned from AFK.
         */
        private boolean update() {
            synchronized (lockingObject) {
                lastActivity = Instant.now();

                if (isAFK) {
                    isAFK = false;
                    return !noTrack;
                }

                return false;
            }
        }

        public boolean isAFK() {
            return isAFK;
        }

        public boolean notTracked() {
            return noTrack;
        }

        public Instant getLastActivity() {
            return lastActivity;
        }
    }
}
