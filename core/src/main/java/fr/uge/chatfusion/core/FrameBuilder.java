package fr.uge.chatfusion.core;

import fr.uge.chatfusion.core.frame.ServerInfo;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

public final class FrameBuilder {
    private ByteBuffer buffer = ByteBuffer.allocate(1_024);

    public FrameBuilder(byte opcode) {
        buffer.put(opcode);
    }

    public FrameBuilder addInt(int i) {
        buffer.putInt(i);
        return this;
    }

    public FrameBuilder addString(String str) {
        Objects.requireNonNull(str);
        var bb = BufferUtils.encodeString(str);
        bb.flip();
        if (bb.remaining() > buffer.remaining()) {
            grow();
        }
        buffer.put(bb);
        return this;
    }

    public FrameBuilder addAddress(InetSocketAddress address) {
        Objects.requireNonNull(address);
        var inet = address.getAddress().getAddress();
        if (inet.length != 4 && inet.length != 16) {
            throw new AssertionError("Impossible address length");
        }
        buffer.put((byte) inet.length);
        buffer.put(inet);
        buffer.putInt(address.getPort());
        return this;
    }

    public FrameBuilder addInfoList(List<ServerInfo> infos) {
        Objects.requireNonNull(infos);
        buffer.putInt(infos.size());
        infos.forEach(i -> {
            addString(i.name());
            addAddress(i.address());
        });
        return this;
    }

    public FrameBuilder addStringList(List<String> strings) {
        Objects.requireNonNull(strings);
        buffer.putInt(strings.size());
        strings.forEach(this::addString);
        return this;
    }

    public ByteBuffer build() {
        var res = ByteBuffer.allocate(buffer.position());
        buffer.flip();
        res.put(buffer);
        return res;
    }

    private void grow() {
        var bigger = ByteBuffer.allocate(buffer.capacity() * 2);
        buffer.flip();
        bigger.put(buffer);
        buffer = bigger;
    }
}
