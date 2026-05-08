package com.ChatLite.client;
import java.io.IOException; import java.net.InetSocketAddress; import java.nio.ByteBuffer; import java.nio.channels.SocketChannel; import java.nio.charset.StandardCharsets; import java.util.function.Consumer;
public class TcpClient {
    private final String host;
    private final int port;
    private final Consumer<String> logger;
    private SocketChannel ch;
    private final ByteBuffer buf = ByteBuffer.allocate(8192);
    private String username = "unknown";
    private Consumer<String> onListItem = s -> {};
    private Consumer<String> onBody = s -> {};
    private boolean collectingBody = false;
    private final StringBuilder bodyBuf = new StringBuilder();

    public TcpClient(String host, int port, Consumer<String> logger) {
        this.host = host;
        this.port = port;
        this.logger = logger;

    }
    public void setOnListItem(Consumer<String> cb){
        this.onListItem = cb;
    }
    public void setOnBody(Consumer<String> cb){
        this.onBody = cb;
    }
    public void connect() throws IOException {
        ch = SocketChannel.open();
        ch.configureBlocking(false);
        ch.connect(new InetSocketAddress(host, port));
        while (!ch.finishConnect()) {

        } new Thread(this::readLoop, "tcp-read").start();
    }
    public void sendLine(String line) {
        try {
            if (line.startsWith("HELO ")) {
                String[] toks = line.split(" ");
                if (toks.length >= 2) username = toks[1];
            } ch.write(StandardCharsets.US_ASCII.encode(line));
            logger.accept("C> " + line.trim());
        } catch (Exception e) {
            logger.accept("send ERR " + e.getMessage());
        }
    }
    private void readLoop() {
        try {
        while (ch.isOpen()) {
            int r = ch.read(buf);
            if (r == -1) break;
            if (r == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {

                } continue;
            }
            buf.flip();
            while (true) {
                int nl = -1;
                for (int i = buf.position(); i < buf.limit(); i++) if (buf.get(i) == '\n') {
                    nl = i; break;
                } if (nl == -1)
                    break;
                int len = nl - buf.position() + 1;
                byte[] line = new byte[len];
                buf.get(line);
                String s = new String(line, StandardCharsets.US_ASCII).trim();
                logger.accept("S< " + s);
                if (s.startsWith("213 ")) {
                    onListItem.accept(s.substring(4));
                } else if (s.equals("214 BODY")) {
                    collectingBody = true;
                    bodyBuf.setLength(0);
                }
                else if (s.equals("214 END")) {
                    collectingBody = false;
                    onBody.accept(bodyBuf.toString());
                    bodyBuf.setLength(0);
                }
                else if (collectingBody) {
                    bodyBuf.append(s).append("\n");
                }
            } buf.compact();
        }
    } catch (Exception e) {
        logger.accept("read ERR " + e.getMessage());
    }
    }
    public String getUsername() {
        return username;
    }
    public void close() throws IOException {
        if (ch != null) ch.close();
    }
}