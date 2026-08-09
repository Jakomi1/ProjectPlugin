package de.jakomi1.project.sidebar;

import de.jakomi1.project.AutoManager;
import de.jakomi1.project.ProjectServer;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
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

/**
 * Anzeige einer Sidebar (Scoreboard rechts) für alle Spieler.
 * <p>
 * Nutzt direkt die NMS-Pakete (wie {@code ScoreboardManager}), damit keine
 * Konflikte mit anderen Plugins entstehen, die das Bukkit-Scoreboard
 * (z.B. für Nametags/Teams) verwenden.
 */
public final class SidebarManager implements AutoManager {

    private final ProjectServer server;
    private final SidebarListener listener;

    private final Map<Integer, Component> lines = new ConcurrentHashMap<>();
    private final Set<UUID> initializedViewers = ConcurrentHashMap.newKeySet();

    private String objectiveName = "project_sidebar";
    private Component title = Component.empty();

    private Scoreboard scoreboard;
    private Objective templateObjective;
    private ClientboundSetObjectivePacket addObjectivePacket;
    private ClientboundSetObjectivePacket removeObjectivePacket;
    private ClientboundSetDisplayObjectivePacket displayPacket;

    private boolean enabled;
    private boolean auto = true;

    public SidebarManager(ProjectServer server) {
        this.server = server;
        this.listener = new SidebarListener(this);
        rebuildTemplates();
    }

    @Override
    public SidebarManager enable() {
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
        clearAll();
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
    public SidebarManager auto(boolean auto) {
        this.auto = auto;
        return this;
    }

    public ProjectServer server() {
        return server;
    }

    public SidebarManager objectiveName(String objectiveName) {
        if (objectiveName == null || objectiveName.isBlank()) return this;

        this.objectiveName = objectiveName;
        rebuildTemplates();
        return this;
    }

    public String objectiveName() {
        return objectiveName;
    }

    public SidebarManager title(Component title) {
        this.title = title != null ? title : Component.empty();
        if (!enabled) return this;

        rebuildTemplates();
        clearAll();
        syncAll();
        return this;
    }

    public Component title() {
        return title;
    }

    /**
     * Setzt eine Zeile. Der Score bestimmt die Position: höhere Scores stehen
     * weiter oben.
     */
    public SidebarManager setLine(int score, Component line) {
        if (score < 0) return this;

        if (line == null) {
            lines.remove(score);
            sendRemoveToAll(score);
            return this;
        }

        lines.put(score, line);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendLine(viewer, score, line);
        }
        return this;
    }

    /**
     * Setzt eine Zeile von oben nach unten ({@code row} 1 = oberste Zeile).
     */
    public SidebarManager line(int row, Component line) {
        return setLine(row, line);
    }

    public SidebarManager removeLine(int score) {
        return setLine(score, null);
    }

    public SidebarManager clearLines() {
        for (Integer score : lines.keySet()) {
            removeLine(score);
        }
        return this;
    }

    public Component getLine(int score) {
        return lines.get(score);
    }

    public Set<Integer> scores() {
        return Set.copyOf(lines.keySet());
    }

    public int size() {
        return lines.size();
    }

    /**
     * Sendet die komplette Sidebar an einen Spieler.
     */
    public SidebarManager sync(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return this;

        ensureObjective(viewer);

        var connection = ((CraftPlayer) viewer).getHandle().connection;
        if (connection == null) return this;

        for (Map.Entry<Integer, Component> entry : lines.entrySet()) {
            connection.send(scorePacket(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    public SidebarManager syncAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sync(viewer);
        }
        return this;
    }

    /**
     * Entfernt die Sidebar beim Spieler (vermeidet, dass ein anderer
     * Scoreboard-Anbieter die Pakete mehrfach sendet).
     */
    public SidebarManager clear(Player viewer) {
        if (viewer == null) return this;

        UUID id = viewer.getUniqueId();
        if (!initializedViewers.remove(id)) return this;

        if (viewer.isOnline()) {
            var connection = ((CraftPlayer) viewer).getHandle().connection;
            if (connection != null) {
                connection.send(removeObjectivePacket);
            }
        }
        return this;
    }

    public SidebarManager clearViewer(Player viewer) {
        return clear(viewer);
    }

    public SidebarManager clearAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            clear(viewer);
        }
        initializedViewers.clear();
        return this;
    }

    private void ensureObjective(Player viewer) {
        UUID id = viewer.getUniqueId();

        if (!initializedViewers.add(id)) return;

        var connection = ((CraftPlayer) viewer).getHandle().connection;
        if (connection == null) return;

        connection.send(removeObjectivePacket);
        connection.send(addObjectivePacket);
        connection.send(displayPacket);
    }

    private void sendLine(Player viewer, int score, Component line) {
        if (viewer == null || !viewer.isOnline()) return;

        ensureObjective(viewer);

        var connection = ((CraftPlayer) viewer).getHandle().connection;
        if (connection != null) {
            connection.send(scorePacket(score, line));
        }
    }

    private void sendRemoveToAll(int score) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!initializedViewers.contains(viewer.getUniqueId())) continue;

            var connection = ((CraftPlayer) viewer).getHandle().connection;
            if (connection != null) {
                connection.send(removeScorePacket(score));
            }
        }
    }

    private ClientboundSetScorePacket scorePacket(int score, Component line) {
        Object nms = PaperAdventure.asVanilla(line);
        Optional<net.minecraft.network.chat.Component> display =
                nms instanceof net.minecraft.network.chat.Component component
                        ? Optional.of(component)
                        : Optional.empty();

        return new ClientboundSetScorePacket(
                ownerKey(score),
                objectiveName,
                score,
                display,
                Optional.empty()
        );
    }

    private ClientboundSetScorePacket removeScorePacket(int score) {
        return new ClientboundSetScorePacket(
                ownerKey(score),
                objectiveName,
                0,
                Optional.empty(),
                Optional.empty()
        );
    }

    private String ownerKey(int score) {
        return objectiveName + ":" + score;
    }

    private void rebuildTemplates() {
        Object nmsTitle = PaperAdventure.asVanilla(title);
        net.minecraft.network.chat.Component displayName =
                nmsTitle instanceof net.minecraft.network.chat.Component component
                        ? component
                        : net.minecraft.network.chat.Component.literal("Sidebar");

        this.scoreboard = new Scoreboard();
        this.templateObjective = scoreboard.addObjective(
                objectiveName,
                ObjectiveCriteria.DUMMY,
                displayName,
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
                DisplaySlot.SIDEBAR,
                templateObjective
        );
    }
}
