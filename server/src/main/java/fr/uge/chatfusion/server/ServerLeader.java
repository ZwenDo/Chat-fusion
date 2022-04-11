package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.selection.SelectionKeyController;
import fr.uge.chatfusion.server.visitor.IdentifiedRemoteInfo;

import java.util.Objects;

record ServerLeader(SelectionKeyController controller, IdentifiedRemoteInfo infos) {
    public ServerLeader {
        Objects.requireNonNull(controller);
        Objects.requireNonNull(infos);
    }
}
