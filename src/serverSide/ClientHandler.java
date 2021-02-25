package serverSide;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean authenticated;
    private boolean sendingTimeout = true;
    private String name;

    public String getName() {
        return name;
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.name = "";

            new Thread(() -> {
                try {
                    new Thread(new Runnable() {
                        public void run() {
                            while (true)
                                try {

                                    Thread.sleep(120000);
                                    if (authenticated == false) {
                                        sendMsg("Клиент отключен от сервера по таймауту ");
                                        closeConnection();
                                    }
                                    ;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                        }
                    }).start();


                    authentication();


                    new Thread(new Runnable() {
                        public void run() {
                            while(true){
                            try {

                                Thread.sleep(180000);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (sendingTimeout == true) {
                                sendMsg("пользователь отключен за бездействие ");
                                closeConnection();

                            }
                                sendingTimeout = true;


                        }}

                    }).start();

                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента");
        }
    }

    public void authentication() throws IOException {
        while (true) {

            String str = in.readUTF();
            if (str.startsWith("/auth")) {
                String[] parts = str.split("\\s");
                String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                if (nick != null) {
                    if (!myServer.isNickBusy(nick)) {
                        sendMsg("/authok " + nick);
                        name = nick;
                        myServer.broadcastMsg(name + " зашел в чат");
                        myServer.subscribe(this);
                        authenticated = true;
                        System.out.println(authenticated);
                        System.out.println(this.authenticated);
                        return;
                    } else {
                        sendMsg("Учетная запись уже используется");
                    }
                } else {
                    sendMsg("Неверные логин/пароль");
                }
            }
        }
    }

    synchronized public void readMessages() throws IOException {

        while (true) {
            String str = in.readUTF();
            sendingTimeout = false;

            System.out.println(sendingTimeout);
            if (str.startsWith("/")) {
                if (str.equals("/end")) {
                    break;
                }
                if (str.startsWith("/w ")) {
                    String[] tokens = str.split("\\s");
                    String nick = tokens[1];
                    String msg = str.substring(4 + nick.length());
                    myServer.sendMsgToClient(this, nick, msg);
                }
                continue;
            }
            myServer.broadcastMsg(name + ": " + str);
        }


    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMsg(name + " вышел из чата");
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

