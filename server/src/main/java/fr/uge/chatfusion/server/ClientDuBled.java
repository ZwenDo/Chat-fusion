package fr.uge.chatfusion.server;

import fr.uge.chatfusion.core.Opcodes;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ClientDuBled {
    private static final Charset CS = StandardCharsets.UTF_8;

    private final SocketChannel sc;
    private final InetSocketAddress serverAddress;

    public ClientDuBled(InetSocketAddress serverAddress) throws IOException {
        Objects.requireNonNull(serverAddress);
        this.serverAddress = serverAddress;
        this.sc = SocketChannel.open();
    }

    public void launch() throws IOException {
        sc.connect(serverAddress);
        var login = CS.encode("ZwenDo");
        var message = CS.encode("flop");
        var buffer = ByteBuffer.allocate(
            2 * Byte.BYTES
            + 2 * Integer.BYTES
            + login.remaining()
            + message.remaining()
        );

        buffer.put(Opcodes.ANONYMOUS_LOGIN.value())
            .putInt(login.remaining())
            .put(login)
            .put(Opcodes.PUBLIC_MESSAGE.value())
            .putInt(message.remaining())
            .put(message)
            .flip();

        sc.write(buffer);
        System.out.println("sent");
        while(true);
    }


    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length != 1) {
            usage();
            return;
        }
        new ClientDuBled(new InetSocketAddress(Integer.parseInt(args[0]))).launch();
    }

    private static void usage() {
        System.out.println("Usage : ClientDuBled port");
    }
}
