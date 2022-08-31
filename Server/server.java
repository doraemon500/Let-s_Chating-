import java.net.*;
import java.io.*;
import java.util.*;


public class server {
    HashMap clients;
    HashMap client;
    HashMap InputStream_client;
    static String[] BackUpname;

    server() {
        clients = new HashMap();
        client = new HashMap();
        InputStream_client = new HashMap();
        Collections.synchronizedMap(clients);
        Collections.synchronizedMap(client);
        Collections.synchronizedMap(InputStream_client);
    }

    public static void main(String args[]) {
        new server().start();
    }

    public void start() {
        ServerSocket serverSocket = null;
        ServerSocket serverSocket1 = null;
        Socket socket = null;
        Socket socket1 = null;

        try {
            serverSocket = new ServerSocket(7777);
            serverSocket1 = new ServerSocket(7772);
            System.out.println("서버가 시작되었습니다");
            while (true) {
                socket = serverSocket.accept();
                socket1 = serverSocket1.accept();
                System.out.println("[" + socket.getInetAddress() + ":" + socket.getPort() + "]" + "에서 접속하셨습니다");
                ServerReceiver thread = new ServerReceiver(socket, socket1);
                thread.start();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ServerReceiver extends Thread {
        Socket socket;
        Socket socket1;
        DataInputStream in;
        DataOutputStream out;
        OutputStream out2;
        InputStream in2;


        ServerReceiver(Socket socket, Socket socket1) {
            this.socket = socket;
            this.socket1 = socket1;
            try {
                in = new DataInputStream((socket.getInputStream()));
                out = new DataOutputStream((socket.getOutputStream()));
                out2 = socket1.getOutputStream();
                in2 = socket1.getInputStream();

            } catch (IOException e) {
            }
        }

        public void run() {
            String name = "";

            try {
                name = in.readUTF();
                sendToAll("#" + name + "님이 들어오셨습니다");

                clients.put(name, out);
                client.put(name,out2);
                InputStream_client.put(name, in2);
                in2.mark(0);
                System.out.println("현재 서버접속자 수는" + clients.size() + "입니다.");
                synchronized (this) {
                    int cnt = 0;
                    while (in != null) {
                        String st = "";
                        try {
                            st = (String) in.readUTF();
                            String[] arr = st.split(",", 4);
                            if (arr[2].equals("5")) {
                                BackUpname = arr;
                                ServerFilereceiever thread = new ServerFilereceiever(socket1,in2,out2, cnt);
                                cnt++;
                                thread.start();
                                continue;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        sendToAll(st);
                    }
                }
            } catch (IOException e) {

            } finally {
                sendToAll("#" + name + "님이 나가셨습니다");
                clients.remove(name);
                System.out.println("[" + socket.getInetAddress() + ":" + socket.getPort() + "]" + "에서 접속을 종료하셨습니다");
                System.out.println("현재 접속자 수는" + clients.size() + "입니다.");
            }
        }


        class ServerFilereceiever extends Thread {
            Socket socket;
            Socket FileSocket;
            OutputStream out;
            InputStream in0;
            int cnt;


            ServerFilereceiever(Socket socket, InputStream in, OutputStream out, int cnt) {
                this.socket = socket;
                try {
                    this.in0 = in;
                    this.out = out;
                    this.cnt = cnt;

                } catch (Exception e) {
                }
            }

            public  void run() {
                Iterator it = InputStream_client.keySet().iterator();

                try {
                        String[] arr = BackUpname;

                        while(it.hasNext()) {
                            String key = (String) it.next();
                            if (key.equals(arr[3])) {
                                InputStream in = (InputStream) InputStream_client.get(key);
                                GetFile getFile = new GetFile(arr, in, out);
                                getFile.start();
                                int cnt = 0;
                                while (!getFile.isInterrupted()) {//isInterrupted()가 true 일 때 WAITING 상태
                                    if (cnt <= 10) {
                                        try {
                                            Thread.sleep(500);
                                        } catch (Exception e) {
                                        }
                                    } else {
                                        getFile.stop();
                                        Initialization init = new Initialization(in);
                                        init.start();
                                        break;
                                    }
                                    cnt++;
                                }
                            }
                        }
                      sendToOne(arr, out);
                }catch(Exception e){
                }
            }
        }


        class GetFile extends Thread {

            String[] arr;
            InputStream in;


            GetFile(String[] arr, InputStream in, OutputStream out) {
                this.arr = arr;
                try {
                    this.in = in;
                } catch (Exception e) {
                }
            }

            public void run() {
                try {
                   Iterator it = client.keySet().iterator();

                   while (it.hasNext()) {
                       String key = (String) it.next();
                        if (key.equals(arr[0])) {
                          OutputStream out = (OutputStream) client.get(key);
                            byte[] buffer = new byte[10000];
                            int readBytes;

                            for (int i = 0; i < 10; i++) {
                                if ((readBytes = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, readBytes);
                                    break;
                                }
                            }
                       }
                    }

                } catch (Exception e) {
                }
            }

        }

        class Initialization extends Thread {
            InputStream in;

            Initialization(InputStream in){
                this.in = in;
            }

            public void run() {
                try {
                    in.reset();
                } catch (IOException e) {}
            }
        }
        synchronized void sendToAll(String msg) {
            Iterator it = clients.keySet().iterator();

            while (it.hasNext()) {
                try {
                    DataOutputStream out = (DataOutputStream) clients.get(it.next());
                    out.writeUTF(msg);
                } catch (IOException e) {
                }
            }
        }

        void sendToOne(String[] arr, OutputStream out) {
            Iterator it = clients.keySet().iterator();

            while (it.hasNext()) {
                String key = (String) it.next();
                if (key.equals(arr[0])) {
                    String st = "";
                    try {
                        DataOutputStream outs = (DataOutputStream) clients.get(key);

                        st = arr[0] + "," + arr[1] + "," + arr[2];
                        outs.writeUTF(st);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }
    }
}
