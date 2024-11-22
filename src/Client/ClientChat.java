package Client;

import javax.imageio.IIOException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class ClientChat extends JFrame {
    private JTextArea ta = new JTextArea(10, 20);  //创建文本域
    private JTextField tf = new JTextField(20);      //创建文本框
    private JPanel jp = new JPanel();                         //创建面板
    private JPanel jp2 = new JPanel();                         //创建面板
    private JButton sendBtn = new JButton("发送");          //创建发送按钮

    private JButton sendFileBtn = new JButton("发送文件");          //创建发送文件按钮
    private JButton receiveFileBtn = new JButton("接受文件");          //创建接受文件按钮
    private JButton checkListBtn = new JButton("查看在线用户");          //创建查看在线用户按钮

    private JScrollPane jsp = new JScrollPane(ta);          //创建滚动条

    private static final String HOST = "127.0.0.1";           //服务器地址
    private static final int PORT = 8888;
    private Socket socket = null;

    private String username = "用户" + (int) (Math.random() * 100);  //随机生成用户名

    private DataOutputStream dos = null;

    //private SystemData systemData = SystemData.getInstance();

    private String userList = "";

    private String fileList = "";
    //线程
    private Thread receiveTh = null;
    private volatile boolean isRunning = true;

    public ClientChat() throws HeadlessException {
        super();
        this.setTitle(username);   //设置窗口标题
        this.add(jsp, "Center");      //添加文本域


        jp.add(tf);                //添加文本框
        jp.add(sendBtn);            //添加发送按钮

        jp2.add(sendFileBtn);            //添加文件按钮
        jp2.add(receiveFileBtn);            //添加查看文件按钮
        jp2.add(checkListBtn);            //添加查看在线用户按钮

        this.add(jp2, "North");      //添加面板
        this.add(jp, "South");      //添加面板

        this.setBounds(300, 300, 400, 500);    //设置窗口大小
//        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    //关闭窗口时退出程序


        ta.setEditable(false);    //设置文本域不可编辑
        tf.requestFocus();      //光标聚集
        this.setVisible(true);  //设置窗口可见

        //关闭窗口
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                closeClient();
            }
        });
        //发送按钮事件
        sendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = tf.getText();    //获取文本框内容
                if (!str.isEmpty()) {
                    str = username + ":" + str;         //添加前缀
                    ta.append(str + "\n");        //添加到文本域
                    tf.setText("");             //清空文本框
                    sendMsg(str);              //发送消息
                }

            }
        });
        //发送文件按钮添加监听
        sendFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.showOpenDialog(null);
                String filePath = jfc.getSelectedFile().getAbsolutePath();
                System.out.println(filePath);
                sendFile(filePath);
            }
        });
        //接受文件按钮添加监听
        receiveFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMsg("REQUEST_FILE_LIST");
                showFileList();
                //sendFile(filePath);
            }
        });

        //文本框添加监听
        tf.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = tf.getText();    //获取文本框内容
                if (!str.isEmpty()) {
                    str = username + ":" + str;         //添加前缀
                    ta.append(str + "\n");        //添加到文本域
                    tf.setText("");             //清空文本框
                    sendMsg(str);              //发送消息
                }

            }
        });
        //查看在线用户按钮添加监听
        checkListBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //发送获取请求
                sendMsg("REQUEST_USER_LIST");
                showOnlineUsers();

            }
        });

        try {
            socket = new Socket(HOST, PORT);   //创建套接字
            dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF(username);  // 发送用户名F
            dos.flush();
        } catch (java.net.ConnectException e) {
            ta.append("服务器未启动或无法连接到服务器\n");
            System.out.println("服务器未启动或无法连接到服务器");

        } catch (Exception e) {
            e.printStackTrace();
        }
        receiveMsg();
    }

    //发送消息的方法
    public void sendMsg(String msg) {
        if (socket != null) {
            try {
                dos = new DataOutputStream(socket.getOutputStream());  //创建输出流
                dos.writeUTF(msg);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    //接收消息的方法
    public void receiveMsg() {
        Thread receiveTh = new Thread(() -> {
            while (isRunning) {
                try {
                    DataInputStream dis = new DataInputStream(socket.getInputStream());

                    String str = dis.readUTF();
                    if(str.equals("RESPONSE_USER_LIST")){
                        str=dis.readUTF();
                        updateUserList(str);
                    }
                    else if(str.equals("RESPONSE_FILE_LIST")){
                        str=dis.readUTF();
                        updateFileList(str);
                    }
                    else if (!str.startsWith(username)) {
                        ta.append(str + "\n");
                    }

                } catch (IOException e) {
                    if (isRunning) {
                        throw new RuntimeException(e);
                    }

                }
            }
        });
        receiveTh.start();
    }

    private void updateUserList(String userList) {
        this.userList = userList;
    }
    private void updateFileList(String fileList){
        this.fileList=fileList;
    }

    public void sendFile(String filePath) {
        if (socket != null) {
            try {
                File file = new File(filePath);
                dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("SEND_FILE");
                dos.flush();
                dos.writeUTF(username);
                dos.flush();
                dos.writeUTF(file.getName());
                dos.flush();

                System.out.println(file.getName());
                ta.append("正在发送文件：" + file.getName() + "\n");

                byte[] bytes = new byte[1024];
                try (FileInputStream fis = new FileInputStream(file)) {
                    int length;
                    while ((length = fis.read(bytes)) > 0) {
                        dos.write(bytes, 0, length);
                    }
                }
                dos.flush();
                ta.append("文件发送成功\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void closeClient() {
        isRunning = false;
        try {
            if (receiveTh != null) {
                receiveTh.interrupt();
            }
            if (socket != null) {
                socket.close();
            }
            System.exit(0);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    // 显示在线用户列表的新窗口
    public void showOnlineUsers() {
        JFrame onlineUsersFrame=new JFrame("在线用户列表");
        JTextArea onlineUsersTextArea = new JTextArea(10, 20);
        JButton refreshButton = new JButton("刷新");
        JPanel btn_jp = new JPanel();
        onlineUsersTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(onlineUsersTextArea);
        System.out.println("在线用户列表");
        System.out.println(userList);
        onlineUsersTextArea.setText(userList);
        //问题：获取到的ccList为空，导致无法显示在线用户列表
        /*
        原因：
        由于 SystemData 实例在客户端和服务器端是独立的，
        客户端无法直接访问服务器端的 SystemData 实例。
        因此，客户端获取到的 ccList 为空。为了实现客户端和服务器共享数据，
        可以通过服务器向客户端发送在线用户列表。
        */
//        SystemData systemData=SystemData.getInstance();
//        System.out.println(systemData.getCcList().size());

//        for(ServerChat.ClientConn cc: systemData.getCcList()){
//            System.out.println(cc.getUsername());
//            onlineUsersTextArea.append(cc.getUsername()+"\n");
//        }
        btn_jp.add(refreshButton);
        onlineUsersFrame.add(scrollPane, BorderLayout.CENTER);
        onlineUsersFrame.add(btn_jp, BorderLayout.SOUTH);
        onlineUsersFrame.setSize(300, 200);
        onlineUsersFrame.setLocationRelativeTo(null);
        onlineUsersFrame.setVisible(true);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMsg("REQUEST_USER_LIST");
                onlineUsersTextArea.setText(userList);
            }
        });
    }
    //查看文件列表窗口
    public void showFileList() {
        JFrame fileListFrame=new JFrame("文件列表");
        JTextArea fileListTextArea = new JTextArea(10, 20);
        JButton refreshButton = new JButton("刷新");
        JPanel btn_jp = new JPanel();
        fileListTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(fileListTextArea);

        System.out.println("文件列表");
        System.out.println(fileList);
        fileListTextArea.setText(fileList);
        btn_jp.add(refreshButton);
        fileListFrame.add(scrollPane, BorderLayout.CENTER);
        fileListFrame.add(btn_jp, BorderLayout.SOUTH);
        fileListFrame.setSize(300, 200);
        fileListFrame.setLocationRelativeTo(null);
        fileListFrame.setVisible(true);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMsg("REQUEST_USER_LIST");
                fileListTextArea.setText(userList);
            }
        });
    }
    public static void main(String[] args) {
        ClientChat clientChat = new ClientChat();


    }
}
