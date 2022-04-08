package fr.uge.chatfusion.server.context;

import fr.uge.chatfusion.core.selection.SelectionKeyController;

public sealed interface Context extends SelectionKeyController
    permits LoggedClientContext, FusedServerContext, DefaultContext {

}
