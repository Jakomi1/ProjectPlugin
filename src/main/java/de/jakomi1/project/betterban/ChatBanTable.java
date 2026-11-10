package de.jakomi1.project.betterban;

/** Speichert permanente und temporaere Chat-Bans. */
public final class ChatBanTable extends AbstractBanTable {

    public ChatBanTable() {
        super("chat_bans");
    }
}
