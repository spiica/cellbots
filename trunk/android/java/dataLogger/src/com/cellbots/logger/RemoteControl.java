
package com.cellbots.logger;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedList;

public class RemoteControl {
    private static final String TAG = "RemoteControl";

    private final Context mContext;
    private RemoteThread mThread = null;

    public interface CommandListener {
        boolean onCommandReceived(Command c) throws Exception;
    }

    @SuppressWarnings("serial")
    private static class ListenerList extends LinkedList<CommandListener> {
    }

    @SuppressWarnings("serial")
    private static class CommandMap extends HashMap<String, ListenerList> {
        public void addCommand(String command, CommandListener listener) {
            if (!containsKey(command)) {
                this.put(command, new ListenerList());
            }
            this.get(command).add(listener);
        }

        public void removeCommand(String command, CommandListener listener) {
            if (!containsKey(command))
                return;
            this.get(command).remove(listener);
        }

        public ListenerList getListeners(String command) {
            if (!containsKey(command))
                return null;
            return this.get(command);
        }
    }

    public static final class Command {
        public String command;
        private final SocketChannel client;

        public Command(String command, SocketChannel client) {
            this.command = command;
            this.client = client;
        }

        public void sendResponse(String response) {
            try {
                ByteBuffer tmp = ByteBuffer.wrap(response.getBytes());
                tmp.position(0);
                tmp.limit(tmp.capacity());
                client.write(tmp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final int MESSAGE_COMMAND = 1;
    private Handler mHandler = new Handler() {
            @Override
        public void handleMessage(Message m) {
            if (m.what != MESSAGE_COMMAND)
                return;

            Command c = (Command) m.obj;
            m.obj = null;

            if (!mCommandMap.containsKey(c.command)) {
                StringBuilder b = new StringBuilder(64);
                b.append("Unknown Command: ").append(c.command).append('\n');
                c.sendResponse(b.toString());
            }

            runListeners(c);
        }
    };

    private CommandMap mCommandMap = new CommandMap();

    public void registerCommandListener(String command, CommandListener listener) {
        mCommandMap.addCommand(command, listener);
    }

    public void unregisterCommandListener(String command, CommandListener listener) {
        mCommandMap.removeCommand(command, listener);
    }

    public void runListeners(Command c) {
        ListenerList listeners = mCommandMap.getListeners(c.command);
        if (listeners == null)
            return;
        for (CommandListener l : listeners) {
            try {
                if (l.onCommandReceived(c))
                    break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return;
    }

    public void broadcastMessage(String message) {
        if (mThread == null)
            return;
        mThread.broadcastMessage(message);
    }

    public RemoteControl(Context context) {
        mContext = context;
    }

    public void start() {
        if (mThread != null)
            return;

        int port = mContext.getResources().getInteger(R.integer.network_remote_port);
        mThread = new RemoteThread(port, mHandler);
        mThread.start();
    }

    public void shutdown() {
        if (mThread != null) {
            mThread.shutdown();
            mThread = null;
        }
    }

    private static final class RemoteThread extends Thread {
        private Selector mSelector = null;

        private final Handler mHandler;
        private final int mPort;

        private boolean mShutdown = false;

        private static final class Tag {
            public boolean client = false;
            public ByteBuffer buf = null;
        }

        public RemoteThread(int port, Handler handler) {
            super("RemoteThread");
            mPort = port;
            mHandler = handler;
        }

        private ByteBuffer mOutBuf = ByteBuffer.allocateDirect(1024);

        public void broadcastMessage(String msg) {
            if (mSelector == null)
                return;

            synchronized (mOutBuf) {
                mOutBuf.clear();
                mOutBuf.put(msg.getBytes());
            }

            mSelector.wakeup();
        }

        public void shutdown() {
            mShutdown = true;
            mSelector.wakeup();
        }

        @Override
        public void run() {
            try {
                mSelector = createSelector();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            int tries = 0;

            Log.i(TAG, "RemoteControl started and ready on port " + mPort);
            Tag t;

            while (!mShutdown) {
                if (tries > 3)
                    return;

                try {
                    Log.i(TAG, "Selecting...");
                    mSelector.select(2500);
                } catch (ClosedSelectorException e) {
                    Log.e(TAG, "Somehow the selector got closed.");
                    return;
                } catch (IOException e) {
                    Log.e(TAG, "Unable to select");
                    e.printStackTrace();
                    tries++;
                    continue;
                }

                if (mShutdown)
                    break;

                for (SelectionKey k : mSelector.selectedKeys()) {
                    try {
                        if (k.isAcceptable()) {
                            acceptConnection(k);
                        }

                        if (k.isReadable()) {
                            if (!readClient(k)) {
                                k.cancel();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        k.cancel();
                    } finally {
                        mSelector.selectedKeys().remove(k);
                    }

                    if (mShutdown)
                        break;
                }

                if (mShutdown)
                    break;

                synchronized (mOutBuf) {
                    if (mOutBuf.position() == 0)
                        continue;

                    for (SelectionKey k : mSelector.keys()) {
                        t = (Tag) k.attachment();
                        if (t == null)
                            continue;
                        if (t.client == false)
                            continue;

                        try {
                            SocketChannel chan = (SocketChannel) k.channel();
                            mOutBuf.flip();
                            chan.write(mOutBuf);
                        } catch (IOException e) {
                            Log.e(TAG, "Error broadcasting message");
                            e.printStackTrace();
                            continue;
                        } finally {
                            mOutBuf.rewind();
                        }

                        if (mShutdown)
                            break;
                    }
                }
            }

            try {
                for (SelectionKey k : mSelector.keys()) {
                    k.channel().close();
                    k.attach(null);
                    k.cancel();
                }
                mSelector.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        private boolean readClient(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel) key.channel();
            Tag tag = (Tag) key.attachment();
            ByteBuffer buf = tag.buf;

            int bytesRead = channel.read(buf);
            if (bytesRead == -1) {
                Log.i(TAG, "Client Disconnected: "
                        + channel.socket().getRemoteSocketAddress().toString());
                // Client disconnected
                tag = null;
                key.attach(null);
                channel.close();
                return false;
            }

            Log.d(TAG, "Received: " + bytesRead + " (" + buf.position() + " )");
            if (buf.position() > 0 &&
                    (buf.get(buf.position() - 1) != '\n') && (buf.get(buf.position() - 1) != '\r'))
            {
                if (buf.position() > 0)
                    Log.d(TAG, "Last char: " + buf.get(buf.position() - 1));
                return true;
            }

            StringBuilder b = new StringBuilder(buf.position());
            for (int i = 0; i < buf.position() - 1; i++)
                b.append((char) buf.get(i));
            buf.put(buf.position() - 1, (byte) 0);

            int pos = b.indexOf("\r");
            while (pos != -1) {
                b.deleteCharAt(pos);
                pos = b.indexOf("\r");
            }

            Log.d(TAG, "Received: " + b.toString());

            Message m = mHandler.obtainMessage();
            m.what = RemoteControl.MESSAGE_COMMAND;
            m.obj = new RemoteControl.Command(b.toString(), channel);
            m.sendToTarget();

            buf.clear();
            return true;
        }

        private void acceptConnection(SelectionKey key) throws IOException {
            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
            SocketChannel client = channel.accept();
            client.configureBlocking(false);

            Tag t = new Tag();
            t.buf = ByteBuffer.allocateDirect(512);
            t.client = true;

            SelectionKey clientKey = client.register(mSelector, SelectionKey.OP_READ);
            clientKey.attach(t);
            Log.i(TAG, "Client connected: " + client.socket().getRemoteSocketAddress());
        }

        private Selector createSelector() throws IOException {
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.socket().setReuseAddress(true);
            serverSocket.socket().bind(new InetSocketAddress(mPort));
            serverSocket.configureBlocking(false);

            Selector selector = Selector.open();
            SelectionKey k = serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            Tag t = new Tag();
            t.client = false;
            k.attach(t);

            return selector;
        }
    }
}
