package fr.uge.chatfusion.core.frame;

import fr.uge.chatfusion.core.reader.base.Reader;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

record FrameReaderPart(
    Reader<Integer> integer,
    Reader<String> string,
    Reader<List<String>> stringList,
    Reader<InetSocketAddress> address
) {
    public FrameReaderPart {
        Objects.requireNonNull(integer);
        Objects.requireNonNull(string);
        Objects.requireNonNull(stringList);
        Objects.requireNonNull(address);
    }
}
