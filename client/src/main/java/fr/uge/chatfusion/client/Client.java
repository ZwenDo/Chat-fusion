package fr.uge.chatfusion.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;


public class Client {


    static public class Context {
        private final Client client;
        private final SelectionKey key;
        private final SocketChannel sc;
        private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
        private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
        private final ArrayDeque<ByteBuffer> queue = new ArrayDeque<>();
        private boolean closed = false;

        private Context(SelectionKey key, Client client) {
            this.client = client;
            Objects.requireNonNull(key);
            this.key = key;
            this.sc = (SocketChannel) key.channel();
        }

        /**
         * Process the content of bufferIn
         *
         * The convention is that bufferIn is in write-mode before the call to process
         * and after the call
         *
         */
        private void processIn() {
            // TODO
        }

        /**
         * Add a message to the message queue, tries to fill bufferOut and updateInterestOps
         *
         * @param bb
         */
        private void queueMessage(ByteBuffer bb) {
            if(queue.size() < 50) {
                queue.add(bb);
                processOut();
                updateInterestOps();
            }
        }

        /**
         * Try to fill bufferOut from the message queue
         *
         */
        private void processOut() {
            while (!queue.isEmpty()){
                var bb = queue.peek();
                if (bb.remaining()<=bufferOut.remaining()){
                    bufferOut.put(bb);
                    queue.remove();
                }else {
                    break;
                }
            }
        }

        /**
         * Update the interestOps of the key looking only at values of the boolean
         * closed and of both ByteBuffers.
         *
         * The convention is that both buffers are in write-mode before the call to
         * updateInterestOps and after the call. Also it is assumed that process has
         * been be called just before updateInterestOps.
         */

        private void updateInterestOps() {
            var interesOps=0;
            if(closed) {
                System.out.println("Connection iterupted");
                client.console.interrupt();
                return;
            }
            if (!closed && bufferIn.hasRemaining()){
                interesOps |= SelectionKey.OP_READ;
            }
            if (bufferIn.position()!=0){
                interesOps|=SelectionKey.OP_WRITE;
            }
            if (interesOps==0){
                silentlyClose();
                return;
            }
            key.interestOps(interesOps);
        }

        private void silentlyClose() {
            try {
                sc.close();
            } catch (IOException e) {
                // ignore exception
            }
        }

        /**
         * Performs the read action on sc
         *
         * The convention is that both buffers are in write-mode before the call to
         * doRead and after the call
         *
         * @throws IOException
         */
        private void doRead() throws IOException {
            if (sc.read(bufferIn)==-1) {
                closed=true;
            }
            processIn();
            updateInterestOps();
        }

        /**
         * Performs the write action on sc
         *
         * The convention is that both buffers are in write-mode before the call to
         * doWrite and after the call
         *
         * @throws IOException
         */

        private void doWrite() throws IOException {
            bufferOut.flip();
            sc.write(bufferOut);
            bufferOut.compact();
            processOut();
            updateInterestOps();
        }

        public void doConnect() throws IOException {
            if (!sc.finishConnect())
                return; // the selector gave a bad hint
            key.interestOps(SelectionKey.OP_READ);
        }
    }


    static private final int BUFFER_SIZE = 1024;
    static private final Logger logger = Logger.getLogger(Client.class.getName());
    static private final String PUBLIC = "ALL", PRIVE = "DM", CONNEXION_PRIVE = "/@", OK_REQUEST = "ACC", KO_REQUEST = "REF";

    private final Charset charset = StandardCharsets.UTF_8;
    private final ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private SocketChannel sc;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final String login;
    private final Thread console;
    private final Object lock  = new Object();
    private Context uniqueContext;
    public  boolean isPublic;
    public  boolean isFile;
    private final ArrayList<String> requesters = new ArrayList<String>();
    private final ArrayBlockingQueue<String[]> commandQueue = new ArrayBlockingQueue<>(10);


    public Client(String login, String host, int port) throws IOException {
        this.serverAddress = new InetSocketAddress(host, port);
        this.login = login;
        this.sc = SocketChannel.open();
        this.selector = Selector.open();
        this.console = new Thread(this::consoleRun);
    }

    private void consoleRun() {
        try {
            try(var scan = new Scanner(System.in)){
                while (scan.hasNextLine()) {
                    var ll = scan.nextLine();
                    if(ll.equals("exit")) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    else if(!ll.startsWith("/") && !ll.startsWith("@")) {
                        isPublic = true;
                        isFile = false;
                    }
                    else if (ll.startsWith("@")){
                        isPublic = false;
                        isFile = false;
                    }
                    else if (ll.startsWith("/")){
                        isFile = true;
                        isPublic = true;
                    }
                    var msg = ll.split("[ :]");
                    sendCommand(msg);


                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            logger.info("Console thread stopping");
            selector.wakeup();
        }
    }


    /**
     * Send a command to the selector via commandQueue and wake it up
     *
     * @param l
     * @throws InterruptedException
     */
    private void sendCommand(String[] l) throws InterruptedException {
        Objects.requireNonNull(l);
        commandQueue.add(l);
        selector.wakeup();
    }


    private void processCommands(){
        buff.clear();
        var log = charset.encode(login);
        while (!commandQueue.isEmpty()){
            var msg = commandQueue.poll();
            if (isFile){
                buff.putInt(Commande.FILE_PRIVATE.ordinal());
                buff.putInt(serverAddress.getHostName().length());
                buff.put(charset.encode(serverAddress.getHostName()));
                buff.putInt(login.length());
                buff.put(charset.encode(login));
                buff.putInt(msg[1].length()); // serveur dest
                buff.put(charset.encode(msg[1]));
                buff.putInt(msg[0].length()); // login dest
                buff.put(charset.encode(msg[0]));

            }
            else if (!isPublic){
                buff.putInt(Commande.MESSAGE_PRIVATE.ordinal());
                buff.putInt(serverAddress.getHostName().length());
                buff.put(charset.encode(serverAddress.getHostName()));
                buff.putInt(login.length());
                buff.put(charset.encode(login));
                buff.putInt(msg[1].length()); // serveur dest
                buff.put(charset.encode(msg[1]));
                buff.putInt(msg[0].length()); // login dest
                buff.putInt(msg[2].length());
                buff.put(charset.encode(msg[2]));
            }
            else if (isPublic){
                buff.putInt(Commande.MESSAGE.ordinal());
                buff.putInt(serverAddress.getHostName().length());
                buff.put(charset.encode(serverAddress.getHostName()));
                buff.putInt(msg[2].length());
                buff.put(charset.encode(msg[2]));

            }
            uniqueContext.queueMessage(buff.flip());
        }
    }


    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isConnectable()) {
                uniqueContext.doConnect();
                ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);

                var log = charset.encode(login);
                bb.putInt(Commande.LOGIN_ANONYMOUS.ordinal());
                bb.putInt(log.limit());
                bb.put(log);
                synchronized(lock) {
                    uniqueContext.queueMessage(bb.flip());
                }
                selector.wakeup();
            }
            if (key.isValid() && key.isWritable()) {
                uniqueContext.doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                uniqueContext.doRead();
            }
        } catch(IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
    }

    public void launch() throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
        uniqueContext = new Client.Context(key, this);
        key.attach(uniqueContext);
        sc.connect(serverAddress);
        console.start();

        while(!Thread.interrupted()) {
            try {
                if(console.isInterrupted()) {
                    break;
                }
                selector.select(this::treatKey);
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
        System.out.println("End of task.\nBye !");

    }

    private static void usage(){
        System.out.println("Usage : Client --chat/--tcp login hostname port");
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length!=3){
            usage();
            return;
        }
        new Client(args[0],args[1],Integer.parseInt(args[2]));

    }
}
