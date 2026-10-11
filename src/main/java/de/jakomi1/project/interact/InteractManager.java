package de.jakomi1.project.interact;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;

/**
 * Schaltet Item-Frames per Werkzeug um: fest/frei und sichtbar/unsichtbar.
 *
 * <p>Standardmaessig wird mit einer Schere und geduckt rechtsgeklickt. Beide
 * Eigenschaften lassen sich abschalten, damit etwa nur die Sichtbarkeit oder
 * nur die Fixierung umgeschaltet wird. Welches Werkzeug noetig ist, laesst
 * sich ueber {@link #tool(Material)} aendern.
 *
 * <p>Der Manager ist opt-in ({@link #defaultState()} ist {@code false}):
 * Erst ein {@link #enable()} registriert den Listener.
 */
public final class InteractManager implements Manager {

    private final ProjectServer server;

    private Material tool = Material.SHEARS;
    private boolean requireSneaking = true;
    private boolean toggleFixed = true;
    private boolean toggleVisible = true;

    private boolean enabled;
    private InteractListener listener;

    public InteractManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public InteractManager enable() {
        if (enabled) return this;

        enabled = true;

        listener = new InteractListener(this);
        listener.register(server.plugin());

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;

        enabled = false;

        if (listener != null) {
            listener.unregister();
            listener = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean defaultState() {
        return false;
    }

    public ProjectServer server() {
        return server;
    }

    /** Werkzeug, mit dem getoggelt wird. Vorgabe ist die Schere. */
    public InteractManager tool(Material tool) {
        this.tool = tool == null ? Material.SHEARS : tool;
        return this;
    }

    public Material tool() {
        return tool;
    }

    /** Muss der Spieler beim Toggeln anschleichen? Vorgabe: ja. */
    public InteractManager requireSneaking(boolean requireSneaking) {
        this.requireSneaking = requireSneaking;
        return this;
    }

    public boolean requireSneaking() {
        return requireSneaking;
    }

    /** Soll der "fixiert"-Status umgeschaltet werden? Vorgabe: ja. */
    public InteractManager toggleFixed(boolean toggleFixed) {
        this.toggleFixed = toggleFixed;
        return this;
    }

    public boolean toggleFixed() {
        return toggleFixed;
    }

    /** Soll die Sichtbarkeit umgeschaltet werden? Vorgabe: ja. */
    public InteractManager toggleVisible(boolean toggleVisible) {
        this.toggleVisible = toggleVisible;
        return this;
    }

    public boolean toggleVisible() {
        return toggleVisible;
    }

    /**
     * Wendet die konfigurierten Umschaltungen auf einen Item Frame an.
     *
     * @return true, wenn mindestens ein Wert geaendert wurde
     */
    public boolean toggle(ItemFrame frame) {
        if (frame == null) return false;

        boolean changed = false;

        if (toggleFixed) {
            frame.setFixed(!frame.isFixed());
            changed = true;
        }

        if (toggleVisible) {
            frame.setVisible(!frame.isVisible());
            changed = true;
        }

        return changed;
    }
}
