package fr.uge.chatfusion.core.reader;

import fr.uge.chatfusion.core.frame.*;
import fr.uge.chatfusion.core.reader.base.Reader;

import java.util.List;

public final class FrameReaders {
    private FrameReaders() {
        throw new AssertionError("No instances.");
    }

    public static Reader<Frame.AnonymousLogin> anonymousLoginReader() {
        return new FrameReader<>(c -> new Frame.AnonymousLogin(c.next()), FrameReader.ArgType.STRING);
    }

    public static Reader<Frame.PublicMessage> publicMessageReader() {
        return new FrameReader<>(
            c -> new Frame.PublicMessage(c.next(), c.next(), c.next()),
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.STRING
        );
    }

    public static Reader<Frame.FusionInit> fusionInitReader() {
        return new FrameReader<>(
            c -> new Frame.FusionInit(c.next(), c.next(), c.next()),
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.ADDRESS,
            FrameReader.ArgType.LIST,
            FrameReader.ArgType.SERVER_INFO
        );
    }

    public static Reader<Frame.FusionInitFwd> fusionInitFwdReader() {
        return new FrameReader<>(c -> new Frame.FusionInitFwd(c.next()), FrameReader.ArgType.ADDRESS);
    }

    public static Reader<Frame.LoginAccepted> loginAcceptedReader() {
        return new FrameReader<>(c -> new Frame.LoginAccepted(c.next()), FrameReader.ArgType.STRING);
    }

    public static Reader<Frame.LoginRefused> loginRefusedReader() {
        return new FrameReader<>(c -> new Frame.LoginRefused());
    }

    public static Reader<Frame.FusionInitKo> fusionInitKoReader() {
        return new FrameReader<>(c -> new Frame.FusionInitKo());
    }

    public static Reader<Frame.FusionMerge> fusionMergeReader() {
        return new FrameReader<>(c -> new Frame.FusionMerge(c.next()), FrameReader.ArgType.STRING);
    }

    public static Reader<Frame.FusionRequest> fusionRequestReader() {
        return new FrameReader<>(c -> new Frame.FusionRequest(c.next()), FrameReader.ArgType.ADDRESS);
    }

    public static Reader<Frame.FusionInitOk> fusionInitOkReader() {
        return new FrameReader<>(
            c -> new Frame.FusionInitOk(c.next(), c.next(), c.next()),
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.ADDRESS,
            FrameReader.ArgType.LIST,
            FrameReader.ArgType.SERVER_INFO
        );
    }

    public static Reader<Frame.FusionChangeLeader> fusionChangeLeaderReader() {
        return new FrameReader<>(
            c -> new Frame.FusionChangeLeader(c.next(), c.next()),
            FrameReader.ArgType.STRING,
            FrameReader.ArgType.ADDRESS
        );
    }
}
