package de.jakomi1.project.betterban;

import de.jakomi1.project.ProjectPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Optionale SimpleVoiceChat-Integration.
 *
 * <p>Die Voicechat-API ist bewusst keine Compile-Abhaengigkeit von
 * ProjectPlugin. Deshalb wird hier komplett ueber Reflection und einen
 * dynamischen Proxy gearbeitet. Ist voicechat nicht installiert oder nicht
 * aktiv, liefert {@link #install(BetterBanManager, ProjectPlugin)} einfach
 * {@code null} und der Rest von betterban funktioniert unveraendert.</p>
 */
public final class VoiceChatHook {

    private static final String PACKAGE = "de.maxhenkel.voicechat.api";
    private static final String EVENT_PACKAGE = PACKAGE + ".events";

    private static final String[] PACKET_EVENTS = {
            "MicrophonePacketEvent",
            "StaticSoundPacketEvent",
            "LocationalSoundPacketEvent",
            "EntitySoundPacketEvent",
            "JoinGroupEvent"
    };

    private final BetterBanManager manager;
    private final ProjectPlugin plugin;
    private final ClassLoader voicechatLoader;

    private Object serverApi;
    private Class<?> serverApiClass;
    private Method getConnectionOf;
    private Method setDisabled;
    private Method connectionGetPlayer;
    private Method playerGetUuid;
    private Method playerGetBukkitPlayer;
    private Method cancel;

    private boolean available;
    private boolean packetEventsRegistered;

    private VoiceChatHook(BetterBanManager manager, ProjectPlugin plugin, ClassLoader voicechatLoader) {
        this.manager = manager;
        this.plugin = plugin;
        this.voicechatLoader = voicechatLoader;
    }

    /**
     * Registriert betterban bei SimpleVoiceChat, falls das Plugin laeuft.
     *
     * <p>Die API-Klassen werden ueber den Klassenlader von voicechat geladen.
     * Damit funktioniert die Integration auch dann, wenn das Spielplugin
     * voicechat nicht als (soft)depend deklariert hat.</p>
     *
     * @return Hook oder {@code null}, wenn voicechat nicht verfuegbar ist
     */
    public static VoiceChatHook install(BetterBanManager manager, ProjectPlugin plugin) {
        if (manager == null || plugin == null) {
            return null;
        }

        Plugin voicechat = Bukkit.getPluginManager().getPlugin("voicechat");

        if (voicechat == null || !voicechat.isEnabled()) {
            return null;
        }

        try {
            ClassLoader loader = voicechat.getClass().getClassLoader();
            Class<?> serviceClass = Class.forName(PACKAGE + ".BukkitVoicechatService", true, loader);
            Object service = Bukkit.getServicesManager().load(serviceClass);

            if (service == null) {
                return null;
            }

            VoiceChatHook hook = new VoiceChatHook(manager, plugin, loader);
            hook.register(service, serviceClass);
            return hook;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Voicechat-Integration konnte nicht geladen werden: " + throwable.getMessage());
            return null;
        }
    }

    public boolean available() {
        return available;
    }

    private void register(Object service, Class<?> serviceClass) throws Exception {
        Class<?> pluginInterface = loadClass(PACKAGE + ".VoicechatPlugin");

        Object proxy = Proxy.newProxyInstance(
                pluginInterface.getClassLoader(),
                new Class<?>[]{pluginInterface},
                this::invoke
        );

        Method registerPlugin = serviceClass.getMethod("registerPlugin", pluginInterface);
        registerPlugin.invoke(service, proxy);
    }

    private Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "getPluginId":
                return "betterban";

            case "initialize":
                if (args != null && args.length == 1) {
                    this.serverApi = args[0];
                    resolveApi();

                    if (available && serverApiClass != null && serverApiClass.isInstance(serverApi)) {
                        this.manager.onVoiceChatReady();
                    } else {
                        this.serverApi = null;
                        this.available = false;
                    }
                }
                return null;

            case "registerEvents":
                if (args != null && args.length == 1) {
                    registerPacketEvents(args[0]);
                }
                return null;

            case "equals":
                return args != null && args.length == 1 && proxy == args[0];

            case "hashCode":
                return System.identityHashCode(proxy);

            case "toString":
                return "BetterBanVoiceChatHook";

            default:
                return null;
        }
    }

    private Class<?> loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name, true, voicechatLoader);
    }

    private void resolveApi() {
        try {
            Class<?> apiClass = loadClass(PACKAGE + ".VoicechatServerApi");
            Class<?> connectionClass = loadClass(PACKAGE + ".VoicechatConnection");
            Class<?> playerClass = loadClass(PACKAGE + ".Player");
            Class<?> eventClass = loadClass(EVENT_PACKAGE + ".Event");

            this.serverApiClass = apiClass;
            this.getConnectionOf = apiClass.getMethod("getConnectionOf", UUID.class);
            this.setDisabled = connectionClass.getMethod("setDisabled", boolean.class);
            this.connectionGetPlayer = connectionClass.getMethod("getPlayer");
            this.playerGetUuid = playerClass.getMethod("getUuid");
            this.playerGetBukkitPlayer = playerClass.getMethod("getPlayer");
            this.cancel = eventClass.getMethod("cancel");
            this.available = true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Voicechat-API konnte nicht aufgeloest werden: " + throwable.getMessage());
        }
    }

    private void registerPacketEvents(Object registration) {
        if (packetEventsRegistered) {
            return;
        }

        packetEventsRegistered = true;

        try {
            Class<?> registrationClass = loadClass(EVENT_PACKAGE + ".EventRegistration");
            Method registerEvent = registrationClass.getMethod(
                    "registerEvent",
                    Class.class,
                    Consumer.class,
                    int.class
            );

            for (String eventName : PACKET_EVENTS) {
                Class<?> eventClass = loadClass(EVENT_PACKAGE + "." + eventName);
                Consumer<Object> consumer = event -> onPacketEvent(event, eventName);
                registerEvent.invoke(registration, eventClass, consumer, 0);
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Voicechat-Events konnten nicht registriert werden: " + throwable.getMessage());
        }
    }

    private void onPacketEvent(Object event, String eventName) {
        if (event == null || !available) {
            return;
        }

        try {
            Object connection = connectionOf(event, connectionGetter(eventName));

            if (connection == null) {
                return;
            }

            Object serverPlayer = connectionGetPlayer.invoke(connection);

            if (serverPlayer == null) {
                return;
            }

            UUID uuid = (UUID) playerGetUuid.invoke(serverPlayer);

            if (uuid == null || !manager.isVoiceBanned(uuid)) {
                return;
            }

            cancel.invoke(event);

            Object bukkitPlayer = playerGetBukkitPlayer.invoke(serverPlayer);

            if (bukkitPlayer instanceof Player player) {
                player.sendActionBar(manager.messages().voiceBanMessage(manager.voiceBanEntry(uuid)));
            }
        } catch (Throwable ignored) {
            // Voicechat-Events duerfen den Server nicht beeintraechtigen.
        }
    }

    private Object connectionOf(Object event, String getter) {
        if (getter == null) {
            return null;
        }

        try {
            return event.getClass().getMethod(getter).invoke(event);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Wie im Original: Mikrofon-Pakete werden am Sender geprueft, Sound-Pakete
     * am Empfaenger und Gruppen-Events an der betroffenen Verbindung.
     */
    private static String connectionGetter(String eventName) {
        if ("MicrophonePacketEvent".equals(eventName)) {
            return "getSenderConnection";
        }

        if ("JoinGroupEvent".equals(eventName)) {
            return "getConnection";
        }

        return "getReceiverConnection";
    }

    /** Schaltet die Voicechat-Verbindung eines Spielers stumm oder wieder frei. */
    public void setDisabled(UUID uuid, boolean disabled) {
        if (!available || serverApi == null || uuid == null) {
            return;
        }

        try {
            Object connection = getConnectionOf.invoke(serverApi, uuid);

            if (connection != null) {
                setDisabled.invoke(connection, disabled);
            }
        } catch (Throwable ignored) {
        }
    }
}
