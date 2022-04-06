package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.frame.AnonymousLogin;
import fr.uge.chatfusion.core.frame.PublicMessage;

public final class FrameReaders {
    private FrameReaders() {
        throw new AssertionError("No instances.");
    }

    public static Reader<AnonymousLogin> anonymousLoginReader() {
        return new FrameReader<>(c -> new AnonymousLogin(c.nextString()), FrameReader.ArgType.STRING);
    }

    public static Reader<PublicMessage> publicMessageReader() {
        return new FrameReader<>(c -> new PublicMessage(c.nextString()), FrameReader.ArgType.STRING);
    }
}
