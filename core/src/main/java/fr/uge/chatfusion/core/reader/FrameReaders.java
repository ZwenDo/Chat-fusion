package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.frame.*;

public final class FrameReaders {
    private FrameReaders() {
        throw new AssertionError("No instances.");
    }

    public static Reader<AnonymousLogin> anonymousLoginReader() {
        return new FrameReader<>(c -> new AnonymousLogin(c.nextString()), FrameReader.ArgType.STRING);
    }

    public static Reader<PublicMessage> publicMessageReader() {
        return new FrameReader<>(
            c -> new PublicMessage(c.nextString(), c.nextString(), c.nextString()),
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.STRING
        );
    }

    public static Reader<FusionInit> fusionInitReader() {
        return new FrameReader<>(
            c -> new FusionInit(c.nextString(), c.nextAddress(), c.nextList(ServerInfo.class)),
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.ADDRESS,
            FrameReader.ArgType.LIST,
            FrameReader.ArgType.SERVER_INFO
        );
    }

    public static Reader<FusionInitFwd> fusionInitFwdReader() {
        return new FrameReader<>(c -> new FusionInitFwd(c.nextAddress()), FrameReader.ArgType.ADDRESS);
    }

    public static Reader<LoginAccepted> loginAcceptedReader() {
        return new FrameReader<>(c -> new LoginAccepted(c.nextString()), FrameReader.ArgType.STRING);
    }

    public static Reader<LoginRefused> loginRefusedReader() {
        return new FrameReader<>(c -> new LoginRefused());
    }

    public static Reader<FusionInitKo> fusionInitKoReader() {
        return new FrameReader<>(c -> new FusionInitKo());
    }

    public static Reader<FusionMerge> fusionMergeReader() {
        return new FrameReader<>(c -> new FusionMerge(c.nextString()), FrameReader.ArgType.STRING);
    }

    public static Reader<FusionRequest> fusionRequestReader() {
        return new FrameReader<>(c -> new FusionRequest(c.nextAddress()), FrameReader.ArgType.ADDRESS);
    }

    public static Reader<FusionInitOk> fusionInitOkReader() {
        return new FrameReader<>(
            c -> new FusionInitOk(c.nextString(), c.nextAddress(), c.nextList(ServerInfo.class)),
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.ADDRESS,
            FrameReader.ArgType.LIST,
            FrameReader.ArgType.SERVER_INFO
        );
    }

    public static Reader<FusionChangeLeader> fusionChangeLeaderReader() {
        return new FrameReader<>(
            c -> new FusionChangeLeader(c.nextString(), c.nextAddress()),
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.ADDRESS
        );
    }
}
