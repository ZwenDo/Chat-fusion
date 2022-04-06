package fr.uge.chatfusion.server.processor;


import fr.uge.chatfusion.core.frame.FrameVisitor;

public interface FrameProcessorClient extends FrameVisitor<Void> {

    FrameProcessorClient nextProcessor();
}
