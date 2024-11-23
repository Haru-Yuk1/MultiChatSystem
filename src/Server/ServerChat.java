package Server;



import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ServerChat extends JFrame {
    //服务器端口号
    private static int serverPORT;
    //使用static静态代码块初始化服务器端口号
    static {
        Properties prop=new Properties();
        try {
            prop.load(new FileReader("chat.properties"));
            serverPORT=Integer.parseInt(prop.getProperty("serverPort"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private JTextArea serverTa = new JTextArea(10, 20);
    private JTextArea clientList = new JTextArea(10,20);
    private JScrollPane jspSeverTa = new JScrollPane(serverTa);
    private JScrollPane jspClientList = new JScrollPane(clientList);

    private JLabel clientListLabel = new JLabel("在线用户列表");

    private JPanel clientListPanel = new JPanel();

    private JPanel btnTool = new JPanel();
    private JButton startBtn = new JButton("启动服务器");
    private JButton stopBtn = new JButton("停止服务器");
    private JButton checkFilesBtn = new JButton("查看文件");

    private ServerSocket serverSocket = null;

    private Socket socket = null;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;

    private Boolean isStart = false;

    private ArrayList<File> files=new ArrayList<>();

//    private SystemData systemData = SystemData.getInstance();

//    private ArrayList<ClientConn> ccList = systemData.getCcList();

    private ArrayList<ClientConn> ccList = new ArrayList<>();

    // 新增组管理逻辑
    private HashMap<String, ArrayList<ClientConn>> groups = new HashMap<>();

    public ServerChat() {
        this.setTitle("服务器端");
        this.add(jspSeverTa, "Center");

        clientListPanel.setLayout(new BorderLayout());
        clientListPanel.add(clientListLabel, BorderLayout.NORTH);
        clientListPanel.add(jspClientList, BorderLayout.CENTER);
        btnTool.add(startBtn);
        btnTool.add(stopBtn);
        btnTool.add(checkFilesBtn);
        this.add(btnTool, "South");
        this.add(btnTool, "South");
        this.add(clientListPanel, "East");

        this.setBounds(200, 200, 600, 600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //设置文本域不可编辑
        serverTa.setEditable(false);
        clientList.setEditable(false);
        //设置窗口可见
        this.setVisible(true);

        serverTa.append("服务器还没启动，请点击启动按钮\n");

        //文件列表初始化，将服务器端的files文件夹下的文件加入到files列表中
        File Directory = new File("files");
        if(Directory.exists()){
            File[] files = Directory.listFiles();
            for(File SecondDirectory:files) {
                for (File file : SecondDirectory.listFiles()) {
                    this.files.add(file);

                }
            }
        }
        //启动服务器按钮添加监听
        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    if (serverSocket == null) {
                        isStart = true;
                        serverSocket = new ServerSocket(serverPORT);

                        //启动服务器使用线程进行监听
                        new Thread(()->{
                            startServer();
                        }).start();
                        serverTa.append("服务器"+serverSocket.getLocalPort()+"启动成功\n");
                    }
                    else{
                        serverTa.append("服务器已经启动了\n");
                    }
                } catch (IOException e2) {
                    throw new RuntimeException(e2);
                }
            }
        });
        //停止服务器按钮添加监听
        stopBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if(serverSocket!=null){
                        isStart=false;
                        stopServer();
                        serverTa.append("服务器已停止\n");
                    }else{
                        serverTa.append("服务器还没启动，请点击启动按钮\n");
                    }

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        //查看文件按钮添加监听
        checkFilesBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFileList();
            }
        });


    }

    //服务器启动的方法
    public void startServer(){

        //等待客户端连接
        while(isStart){
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ccList.add(new ClientConn(socket));

        }
    }

    //服务器停止的方法
    public void stopServer() throws IOException {
        if (socket != null) {
            socket.close();
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
    }



    //服务器发送消息的方法
    public void sendMsg(String msg) throws IOException {
        if (socket != null) {
            socket.getOutputStream().write(msg.getBytes());
        }
    }

    //服务器接收消息的方法
    public void receiveMsg() {
        try {
            dis = new DataInputStream(socket.getInputStream());
            String msg = dis.readUTF();
            System.out.println(msg);
            serverTa.append(msg + "\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //服务器端的客户端连接线程
    public class ClientConn implements Runnable{
        Socket socket=null;
        String username=null;
        Thread th=null;
        DataInputStream dis=null;
        public ClientConn(Socket socket){
            this.socket=socket;
            this.th=(new Thread(this));
            th.start();
        }

        @Override
        public void run() {
            try{
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
                while (true) {
                    String msg = dis.readUTF();
                    if (msg.startsWith("CHECK_USERNAME#")) {
                        String usernameToCheck = msg.split("#")[1];
                        if (isUsernameTaken(usernameToCheck)) {
                            dos.writeUTF("USERNAME_TAKEN");
                        } else {
                            dos.writeUTF("USERNAME_OK");
                        }
                        dos.flush();
                    } else {
                        username = msg;
                        break;
                    }
                }
                System.out.println("username:"+username);
//                dis=new DataInputStream(socket.getInputStream());
//                username = dis.readUTF();  // 接收用户名

                //服务器端输出用户连接信息
                System.out.println(username+socket.getInetAddress() + ":" + socket.getPort() + "连接到服务器");
                serverTa.append(username+socket.getInetAddress() + ":" + socket.getPort() + "连接到服务器\n");
                //广播用户上线
                broadcast(username+"已上线");

                //显示在线用户列表
                showClientList();


                String str=dis.readUTF();

                while(!str.isEmpty()){
                    if(str.startsWith("PRIVATE#")){
                        String[] strs = str.split("#");
                        String toUser = strs[1];
                        String msg = strs[2];
                        //找到要发送的用户
                        for(ClientConn cc:ccList){
                            if(cc.username.equals(toUser)){
                                dos=new DataOutputStream(cc.socket.getOutputStream());
                                dos.writeUTF(username+"悄悄对你说："+msg);
                            }
                        }
                    }
                    else if(str.startsWith("CREATE_GROUP#")){
                        // 格式：CREATE_GROUP#groupName#用户列表
                        String[] parts=str.split("#",3);
                        String groupName=parts[1];
                        String[] groupMembers=parts[2].split(",");
                        System.out.println(parts[2]);
                        createGroup(groupName,groupMembers);

                    } else if (str.startsWith("GROUP_SERVER#")) {
                        // 格式：GROUP#groupName#msg
                        System.out.println("群聊消息：" + str);
                        String[] parts = str.split("#", 3);
                        String groupName = parts[1];
                        String msg = parts[2];
//                        System.out.println("群聊消息：" + msg);
                        broadcastGroup(groupName, "GROUP_CLIENT#"+groupName+"#"+msg);
                        broadcastGroup(groupName, "群聊"+groupName + ":" + msg);

                    } else if(str.equals("REQUEST_USER_LIST")){
                        System.out.println(ccList.size());
                        sendClientList();

                    }
                    else if (str.equals("REQUEST_FILE_LIST")){
                        System.out.println(files.size());
                        sendFileList();
                    }
                    else if (str.equals("SEND_FILE")){
                        String userName=dis.readUTF();
                        String fileName=dis.readUTF();
                        File Directory = new File("files");
                        if(!Directory.exists()){
                            Directory.mkdir();
                        }
                        File userDirectory = new File(Directory,userName);
                        if(!userDirectory.exists()){
                            userDirectory.mkdir();
                        }
                        File file = new File(userDirectory, fileName);
                        dis = new DataInputStream(socket.getInputStream());
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = dis.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                            System.out.println("文件接收成功");
                        }
                        System.out.println("收到"+userName+"传输的文件："+fileName);
                        serverTa.append("收到"+userName+"传输的文件："+fileName+"\n");
                        files.add(file);
                    } else if (str.startsWith("DOWNLOAD_FILE#")) {
                        String[] parts = str.split("#");

                        String userName = parts[1];
                        String fileName = parts[2];
                        File Directory = new File("files");
                        File userDirectory = new File(Directory, userName);
                        File file = new File(userDirectory, fileName);
                        System.out.println("文件路径："+file.getPath());
                        dos = new DataOutputStream(socket.getOutputStream());
                        dos.writeUTF("RESPONSE_DOWNLOAD_FILE");
                        if (file.exists()) {
                            byte[] buffer = new byte[1024];
                            try (FileInputStream fis = new FileInputStream(file)) {
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    dos.write(buffer, 0, length);
//                                    System.out.println("文件发送中");
//                                    if (length < 1024) {
//                                        break;
//                                    }
                                }
                                dos.flush();
                                System.out.println("文件发送成功");

                            }
                        } else {
                            dos.writeUTF("文件不存在");
                            dos.flush();
                        }

                    } else{
                        System.out.println(str);
                        serverTa.append(str+"\n");
                        broadcast(str);
                    }

                    str=dis.readUTF();

                }

            } catch (IOException e) {
                destroy();
                throw new RuntimeException(e);
            }
        }

        //销毁方法
        public void destroy(){
            try {
                dis=new DataInputStream(socket.getInputStream());
                //服务器端输出用户断开信息
                System.out.println(username+socket.getInetAddress() + ":" + socket.getPort() + "断开连接");
                serverTa.append(username+socket.getInetAddress() + ":" + socket.getPort() + "断开连接\n");

                //用户下线
                clientOffline(username);
                //广播用户下线
                broadcast(username+"已下线");
                //发送用户列表
                sendClientList();


                if (th!=null) {
                    th.interrupt();
                }
                if(socket!=null){
                    socket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getUsername() {
            return username;
        }
    }
    //服务器判断用户名是否重复
    private boolean isUsernameTaken(String username) {
        if (ccList.isEmpty()) {
            return false;
        }
        for (ClientConn cc : ccList) {
            if (cc.getUsername() == null) {
                continue;
            }
            if (cc.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    //服务器端创建群组的方法
    private void createGroup(String groupName, String[] groupMembers) {
        ArrayList<ClientConn> group = new ArrayList<>();
        for (String member : groupMembers) {
            for (ClientConn cc : ccList) {
                if (cc.username.equals(member)) {
//                    System.out.println("找到用户"+member);
                    group.add(cc);
                    break;
                }
            }
        }
        groups.put(groupName, group);


        //广播群组创建成功，从而让所有客户端新建窗口，显示群组聊天
        broadcastGroup(groupName, "RESPONSE_CREATE_GROUP");
        broadcastGroup(groupName, groupName);

        System.out.println(groupName+"群组创建成功");
        serverTa.append(groupName+"群组创建成功");
    }
    //群组消息广播
    private void broadcastGroup(String groupName, String msg) {
//        System.out.println("广播群聊log："+groupName+msg);
        ArrayList<ClientConn> group = groups.get(groupName);
        try {
            for (ClientConn cc : group) {

                dos = new DataOutputStream(cc.socket.getOutputStream());
                dos.writeUTF(msg);

//                System.out.println("发送消息成功");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        System.out.println(groupName+"群组消息广播成功");
    }

    //服务器端发送用户列表的方法
    public void sendClientList() throws IOException {
        StringBuilder userList = new StringBuilder();
        for (ClientConn cc : ccList) {
            userList.append(cc.getUsername()).append("\n");
        }
        //广播用户列表
        for (ClientConn cc : ccList) {
            DataOutputStream dos = new DataOutputStream(cc.socket.getOutputStream());
            dos.writeUTF("RESPONSE_USER_LIST");
            dos.flush();
            dos.writeUTF(userList.toString());
            dos.flush();
        }
    }
    //服务器端发送文件列表的方法
    public void sendFileList() throws IOException {
        StringBuilder fileList = new StringBuilder();
        for(File file:files){
            fileList.append(file.getParent().substring(6)+"/"+file.getName()+"\n");
        }
        System.out.println(fileList.toString());
        //广播文件列表
        for (ClientConn cc : ccList) {

            DataOutputStream dos = new DataOutputStream(cc.socket.getOutputStream());
            dos.writeUTF("RESPONSE_FILE_LIST");
            dos.flush();
            dos.writeUTF(fileList.toString());
            dos.flush();

        }
    }
    //服务器端广播消息的方法
    public void broadcast(String msg) throws IOException {
        for (ClientConn cc : ccList) {
            dos=new DataOutputStream(cc.socket.getOutputStream());
            dos.writeUTF(msg);
        }
    }

    //显示在线用户列表
    public void showClientList(){
        //清空在线用户列表
        clientList.setText("");
        //显示在线用户列表
        for(ClientConn cc:ccList){
            clientList.append(cc.username+"\n");
            //将所有用户广播出去

//            try {
//                broadcast(cc.username);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
        }
    }


    //服务器端中客户下线的方法
    public void clientOffline(String username){
        for(ClientConn cc:ccList){
            if(cc.username.equals(username)){
                ccList.remove(cc);
                break;
            }
        }
        showClientList();
    }

    //在新窗口显示文件列表
    public void showFileList(){
        JFrame fileFrame = new JFrame("文件列表");
        fileFrame.setLayout(new BorderLayout());
        JTextArea fileTa = new JTextArea(10, 20);
        JScrollPane fileJsp = new JScrollPane(fileTa);
        fileFrame.add(fileJsp, "Center");
        fileFrame.setBounds(600, 600, 250, 300);
        fileFrame.setVisible(true);
        fileTa.setEditable(false);
        StringBuilder sb = new StringBuilder();
        for(File file:files){
            sb.append(file.getParent().substring(6)+"/"+file.getName()+"\n");

        }
        fileTa.setText(sb.toString());
    }
    public static void main(String[] args) {
        ServerChat serverChat = new ServerChat();
    }
}
