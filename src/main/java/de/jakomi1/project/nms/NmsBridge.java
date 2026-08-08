package de.jakomi1.project.nms;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Interne NMS-Bridge auf Basis von Reflection.
 *
 * Simuliert Server-Teams (Scoreboard + PlayerTeam) und sendet
 * {@code ClientboundSetPlayerTeamPacket}-Pakete direkt an Spieler, damit
 * Nametag-Präfixe über den Köpfen erscheinen. Baut wie in TheKingdoms auf
 * NMS-Klassen-Namen auf, ohne direkte Referenzen auf die Server-Jar.
 */
public final class NmsBridge {

    private final Logger logger;

    private boolean ready;
    private boolean broken;

    private Class<?> craftPlayerClass;
    private Class<?> serverPlayerClass;
    private Class<?> packetClass;
    private Class<?> scoreboardClass;
    private Class<?> playerTeamClass;
    private Class<?> teamColorClass;
    private Class<?> visibilityClass;
    private Class<?> collisionRuleClass;
    private Class<?> teamPacketClass;
    private Class<?> actionClass;
    private Class<?> connectionClass;
    private Class<?> paperAdventureClass;
    private Class<?> adventureComponentClass;

    private Method craftPlayerGetHandle;
    private Field serverPlayerConnection;
    private Method connectionSend;
    private Constructor<?> scoreboardConstructor;
    private Constructor<?> playerTeamConstructor;
    private Method scoreboardAddPlayerToTeam;
    private Method playerTeamSetDisplayName;
    private Method playerTeamSetPlayerPrefix;
    private Method playerTeamSetPlayerSuffix;
    private Method playerTeamSetColor;
    private Method playerTeamSetNameTagVisibility;
    private Method playerTeamSetCollisionRule;
    private Method playerTeamSetAllowFriendlyFire;
    private Method playerTeamSetSeeFriendlyInvisibles;
    private Method createAddOrModifyPacket;
    private Method createRemovePacket;
    private Method createPlayerPacket;
    private Method createMultiplePlayerPacket;
    private Method asVanillaComponent;

    private Object teamColorWhite;
    private Object visibilityAlways;
    private Object collisionRuleNever;
    private Object actionAdd;
    private Object actionRemove;

    public NmsBridge(Logger logger) {
        this.logger = logger;
    }

    public boolean isReady() {
        return ready;
    }

    public synchronized void init() {
        if (ready || broken) return;

        try {
            craftPlayerClass = ReflectionUtils.findClass(
                    "org.bukkit.craftbukkit.entity.CraftPlayer"
            );
            serverPlayerClass = ReflectionUtils.findClass(
                    "net.minecraft.server.level.ServerPlayer"
            );
            packetClass = ReflectionUtils.findClass(
                    "net.minecraft.network.protocol.Packet"
            );
            scoreboardClass = ReflectionUtils.findClass(
                    "net.minecraft.world.scores.Scoreboard"
            );
            playerTeamClass = ReflectionUtils.findClass(
                    "net.minecraft.world.scores.PlayerTeam"
            );
            teamColorClass = ReflectionUtils.findClass(
                    "net.minecraft.world.scores.TeamColor"
            );
            visibilityClass = ReflectionUtils.findClass(
                    "net.minecraft.world.scores.Team$Visibility"
            );
            collisionRuleClass = ReflectionUtils.findClass(
                    "net.minecraft.world.scores.Team$CollisionRule"
            );
            teamPacketClass = ReflectionUtils.findClass(
                    "net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket"
            );
            actionClass = ReflectionUtils.findClass(
                    "net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket$Action"
            );
            connectionClass = ReflectionUtils.findClass(
                    "net.minecraft.server.network.ServerCommonPacketListenerImpl"
            );
            paperAdventureClass = ReflectionUtils.findClass(
                    "io.papermc.paper.adventure.PaperAdventure"
            );
            adventureComponentClass = ReflectionUtils.findClass(
                    "net.kyori.adventure.text.Component"
            );

            if (anyClassMissing()) {
                broken = true;
                logger.severe("NmsBridge: NMS-Klassen wurden nicht gefunden, Nametags sind deaktiviert.");
                return;
            }

            craftPlayerGetHandle = ReflectionUtils.getMethod(craftPlayerClass, "getHandle");
            serverPlayerConnection = ReflectionUtils.getField(serverPlayerClass, "connection");
            connectionSend = ReflectionUtils.getMethod(connectionClass, "send", packetClass);
            scoreboardConstructor = ReflectionUtils.getConstructor(scoreboardClass);
            playerTeamConstructor = ReflectionUtils.getConstructor(playerTeamClass, scoreboardClass, String.class);
            scoreboardAddPlayerToTeam = ReflectionUtils.getMethod(
                    scoreboardClass, "addPlayerToTeam", String.class, playerTeamClass
            );
            playerTeamSetDisplayName = ReflectionUtils.getMethod(playerTeamClass, "setDisplayName", nmsComponentClass());
            playerTeamSetPlayerPrefix = ReflectionUtils.getMethod(playerTeamClass, "setPlayerPrefix", nmsComponentClass());
            playerTeamSetPlayerSuffix = ReflectionUtils.getMethod(playerTeamClass, "setPlayerSuffix", nmsComponentClass());
            playerTeamSetColor = ReflectionUtils.getMethod(playerTeamClass, "setColor", Optional.class);
            playerTeamSetNameTagVisibility = ReflectionUtils.getMethod(playerTeamClass, "setNameTagVisibility", visibilityClass);
            playerTeamSetCollisionRule = ReflectionUtils.getMethod(playerTeamClass, "setCollisionRule", collisionRuleClass);
            playerTeamSetAllowFriendlyFire = ReflectionUtils.getMethod(playerTeamClass, "setAllowFriendlyFire", boolean.class);
            playerTeamSetSeeFriendlyInvisibles = ReflectionUtils.getMethod(playerTeamClass, "setSeeFriendlyInvisibles", boolean.class);
            createAddOrModifyPacket = ReflectionUtils.getMethod(teamPacketClass, "createAddOrModifyPacket", playerTeamClass, boolean.class);
            createRemovePacket = ReflectionUtils.getMethod(teamPacketClass, "createRemovePacket", playerTeamClass);
            createPlayerPacket = ReflectionUtils.getMethod(
                    teamPacketClass, "createPlayerPacket", playerTeamClass, String.class, actionClass
            );
            createMultiplePlayerPacket = ReflectionUtils.getMethod(
                    teamPacketClass, "createMultiplePlayerPacket", playerTeamClass, Collection.class, actionClass
            );
            asVanillaComponent = ReflectionUtils.getMethod(paperAdventureClass, "asVanilla", adventureComponentClass);

            if (anyHandleMissing()) {
                broken = true;
                logger.severe("NmsBridge: NMS-Handler wurden nicht aufgelöst, Nametags sind deaktiviert.");
                return;
            }

            teamColorWhite = ReflectionUtils.enumConstant(teamColorClass, "WHITE");
            visibilityAlways = ReflectionUtils.enumConstant(visibilityClass, "ALWAYS");
            collisionRuleNever = ReflectionUtils.enumConstant(collisionRuleClass, "NEVER");
            actionAdd = ReflectionUtils.enumConstant(actionClass, "ADD");
            actionRemove = ReflectionUtils.enumConstant(actionClass, "REMOVE");

            if (teamColorWhite == null || visibilityAlways == null || collisionRuleNever == null
                    || actionAdd == null || actionRemove == null) {
                broken = true;
                logger.severe("NmsBridge: NMS-Konstanten wurden nicht gefunden, Nametags sind deaktiviert.");
                return;
            }

            ready = true;
        } catch (Throwable throwable) {
            broken = true;
            logger.severe("NmsBridge: Fehler beim Initialisieren, Nametags sind deaktiviert: "
                    + throwable.getMessage());
        }
    }

    @Nullable
    private static Class<?> nmsComponentClass() {
        return ReflectionUtils.findClass("net.minecraft.network.chat.Component");
    }

    private boolean anyClassMissing() {
        return craftPlayerClass == null
                || serverPlayerClass == null
                || packetClass == null
                || scoreboardClass == null
                || playerTeamClass == null
                || teamColorClass == null
                || visibilityClass == null
                || collisionRuleClass == null
                || teamPacketClass == null
                || actionClass == null
                || connectionClass == null
                || paperAdventureClass == null
                || adventureComponentClass == null
                || nmsComponentClass() == null;
    }

    private boolean anyHandleMissing() {
        return craftPlayerGetHandle == null
                || serverPlayerConnection == null
                || connectionSend == null
                || scoreboardConstructor == null
                || playerTeamConstructor == null
                || scoreboardAddPlayerToTeam == null
                || playerTeamSetDisplayName == null
                || playerTeamSetPlayerPrefix == null
                || playerTeamSetPlayerSuffix == null
                || playerTeamSetColor == null
                || playerTeamSetNameTagVisibility == null
                || playerTeamSetCollisionRule == null
                || playerTeamSetAllowFriendlyFire == null
                || playerTeamSetSeeFriendlyInvisibles == null
                || createAddOrModifyPacket == null
                || createRemovePacket == null
                || createPlayerPacket == null
                || createMultiplePlayerPacket == null
                || asVanillaComponent == null;
    }

    @Nullable
    public Object newScoreboard() {
        return ReflectionUtils.construct(scoreboardConstructor);
    }

    @Nullable
    public Object newTeam(Object scoreboard, String name) {
        return ReflectionUtils.construct(playerTeamConstructor, scoreboard, name);
    }

    public void addPlayerToTeam(Object scoreboard, Object team, String playerName) {
        ReflectionUtils.invoke(scoreboardAddPlayerToTeam, scoreboard, playerName, team);
    }

    public void configureTeam(Object team, Component prefix, Component suffix) {
        if (team == null || !ready) return;

        Object nmsPrefix = asVanilla(prefix);
        if (nmsPrefix != null) {
            ReflectionUtils.invoke(playerTeamSetDisplayName, team, nmsPrefix);
            ReflectionUtils.invoke(playerTeamSetPlayerPrefix, team, nmsPrefix);
        }

        Object nmsSuffix = asVanilla(suffix);
        if (nmsSuffix != null) {
            ReflectionUtils.invoke(playerTeamSetPlayerSuffix, team, nmsSuffix);
        }

        ReflectionUtils.invoke(playerTeamSetColor, team, Optional.of(teamColorWhite));
        ReflectionUtils.invoke(playerTeamSetNameTagVisibility, team, visibilityAlways);
        ReflectionUtils.invoke(playerTeamSetCollisionRule, team, collisionRuleNever);
        ReflectionUtils.invoke(playerTeamSetAllowFriendlyFire, team, false);
        ReflectionUtils.invoke(playerTeamSetSeeFriendlyInvisibles, team, false);
    }

    @Nullable
    public Object addOrModifyPacket(Object team, boolean withPlayers) {
        return ReflectionUtils.invokeStatic(createAddOrModifyPacket, team, withPlayers);
    }

    @Nullable
    public Object removePacket(Object team) {
        return ReflectionUtils.invokeStatic(createRemovePacket, team);
    }

    @Nullable
    public Object multiplePlayerPacket(Object team, Collection<String> players, boolean add) {
        return ReflectionUtils.invokeStatic(createMultiplePlayerPacket, team, players, add ? actionAdd : actionRemove);
    }

    @Nullable
    public Object playerPacket(Object team, String playerName, boolean add) {
        return ReflectionUtils.invokeStatic(createPlayerPacket, team, playerName, add ? actionAdd : actionRemove);
    }

    @Nullable
    private Object asVanilla(Component component) {
        return ReflectionUtils.invokeStatic(asVanillaComponent, component);
    }

    public void send(Player player, Object packet) {
        if (!ready || packet == null || player == null || !player.isOnline()) return;

        try {
            Object handle = craftPlayerGetHandle.invoke(player);
            if (handle == null) return;

            Object connection = serverPlayerConnection.get(handle);
            if (connection == null) return;

            connectionSend.invoke(connection, packet);
        } catch (Throwable ignored) {
        }
    }
}
