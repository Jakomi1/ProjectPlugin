package de.jakomi1.project.combat;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class CombatEndEvent extends Event implements Cancellable {

    public enum Reason {
        EXPIRED,
        REMOVED,
        DISABLED
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Reason reason;
    private boolean cancelled;

    public CombatEndEvent(Player player, Reason reason) {
        this.player = player;
        this.reason = reason;
    }

    public Player player() {
        return player;
    }

    public Reason reason() {
        return reason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
