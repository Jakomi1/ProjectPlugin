package de.jakomi1.project.scoreboard;

import de.jakomi1.project.AutoManager;
import de.jakomi1.project.ProjectServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardManager implements AutoManager {

    private final ProjectServer server;
    private final TabNumberListener listener;

    private final Map<UUID, Integer> values = new ConcurrentHashMap<>();
    private final Set<UUID> initializedViewers = ConcurrentHashMap.newKeySet();

    private String objectiveName = "project_tab";

    private Scoreboard scoreboard;
    private Objective templateObjective;
    private ClientboundSetObjectivePacket addObjectivePacket;
    private ClientboundSetObjectivePacket removeObjectivePacket;
    private ClientboundSetDisplayObjectivePacket displayPacket;

    private boolean enabled;
    private boolean auto = true;

    public ScoreboardManager(ProjectServer server) {
        this.server = server;
        this.listener = new TabNumberListener(this);
        rebuildTemplates();
    }

    public ScoreboardManager objectiveName(String objectiveName) {
        if (objectiveName == null || objectiveName.isBlank()) return this;

        this.objectiveName = objectiveName;
        rebuildTemplates();
        return this;
    }

    public String objectiveName() {
        return objectiveName;
    }

    @Override
    public ScoreboardManager enable() {
        if (enabled) return this;
        enabled = true;

        listener.register(server.plugin());
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        listener.unregister();
        resetAll();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean auto() {
        return auto;
    }

    @Override
    public ScoreboardManager auto(boolean auto) {
        this.auto = auto;
        return this;
    }

    public ScoreboardManager setTabNumber(Player player, int number) {
        if (player == null) return this;

        values.put(player.getUniqueId(), number);
        sendScoreToAllViewers(player, number);
        return this;
    }

    public ScoreboardManager removeTabNumber(Player player) {
        if (player == null) return this;

        values.remove(player.getUniqueId());

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendObjectiveIfNeeded(viewer);

            ((CraftPlayer) viewer).getHandle().connection.send(
                    new ClientboundSetScorePacket(
                            player.getName(),
                            objectiveName,
                            0,
                            Optional.empty(),
                            Optional.empty()
                    )
            );
        }
        return this;
    }

    public ScoreboardManager clear(Player player) {
        return removeTabNumber(player);
    }

    public int getTabNumber(Player player) {
        if (player == null) return 0;

        Integer value = values.get(player.getUniqueId());
        return value != null ? value : 0;
    }

    public void resetAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            var connection = ((CraftPlayer) viewer).getHandle().connection;
            if (connection != null) {
                connection.send(removeObjectivePacket);
            }
        }
        initializedViewers.clear();
    }

    public void clearViewer(Player viewer) {
        if (viewer == null) return;

        initializedViewers.remove(viewer.getUniqueId());
    }

    public void sendFullSync(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;

        sendObjectiveIfNeeded(viewer);

        for (Map.Entry<UUID, Integer> entry : values.entrySet()) {
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target != null && target.isOnline()) {
                ((CraftPlayer) viewer).getHandle().connection.send(
                        new ClientboundSetScorePacket(
                                target.getName(),
                                objectiveName,
                                entry.getValue(),
                                Optional.empty(),
                                Optional.empty()
                        )
                );
            }
        }
    }

    private void sendScoreToAllViewers(Player target, int number) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendObjectiveIfNeeded(viewer);

            ((CraftPlayer) viewer).getHandle().connection.send(
                    new ClientboundSetScorePacket(
                            target.getName(),
                            objectiveName,
                            number,
                            Optional.empty(),
                            Optional.empty()
                    )
            );
        }
    }

    private void sendObjectiveIfNeeded(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;

        UUID viewerId = viewer.getUniqueId();

        if (initializedViewers.add(viewerId)) {
            var connection = ((CraftPlayer) viewer).getHandle().connection;

            connection.send(removeObjectivePacket);
            connection.send(addObjectivePacket);
            connection.send(displayPacket);
        }
    }

    private void rebuildTemplates() {
        this.scoreboard = new Scoreboard();
        this.templateObjective = scoreboard.addObjective(
                objectiveName,
                ObjectiveCriteria.DUMMY,
                Component.literal("Tab"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null
        );

        this.addObjectivePacket = new ClientboundSetObjectivePacket(
                templateObjective,
                ClientboundSetObjectivePacket.METHOD_ADD
        );

        this.removeObjectivePacket = new ClientboundSetObjectivePacket(
                templateObjective,
                ClientboundSetObjectivePacket.METHOD_REMOVE
        );

        this.displayPacket = new ClientboundSetDisplayObjectivePacket(
                DisplaySlot.LIST,
                templateObjective
        );
    }

    public ProjectServer server() {
        return server;
    }
}
