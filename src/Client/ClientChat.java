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
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;

public class ClientChat extends JFrame {
    private JTextArea ta = new JTextArea(10, 20);  //创建文本域
    private JTextField tf = new JTextField(20);      //创建文本框
    private JPanel jp = new JPanel();                         //创建面板
    private JPanel jp2 = new JPanel();                         //创建面板
    private JButton sendBtn = new JButton("发送");          //创建发送按钮

    private JButton sendFileBtn = new JButton("发送文件");          //创建发送文件按钮
    private JButton receiveFileBtn = new JButton("接受文件");          //创建接受文件按钮
    private JButton checkListBtn = new JButton("查看在线用户");          //创建查看在线用户按钮
    private JButton myGroupBtn = new JButton("我的群聊");          //创建查看我的群聊按钮

    private JScrollPane jsp = new JScrollPane(ta);          //创建滚动条

    private static String HOST = "127.0.0.1";           //服务器地址
    private static int PORT = 8888;

    static {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("chat.properties"));
            HOST = properties.getProperty("serverIP");
            PORT = Integer.parseInt(properties.getProperty("clientPort"));
            System.out.println("HOST:" + HOST + " PORT:" + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Socket socket = null;

    private String username ; //用户名


    private DataOutputStream dos = null;

    //private SystemData systemData = SystemData.getInstance();

    private String userList = "";

    private String fileList = "";
    //线程
    private Thread receiveTh = null;
    private volatile boolean isRunning = true;

    // 私聊和群聊
    private JList<String> userListUI=new JList<>();
    private DefaultListModel<String> userlistModel=new DefaultListModel<>();
    private JScrollPane userListScrollPane=new JScrollPane(userListUI);

    private JList<String> myGroup=new JList<>();
    private DefaultListModel<String> myGroupModel=new DefaultListModel<>();
    private JScrollPane myGroupScrollPane=new JScrollPane(myGroup);

    private JTextArea groupChatTextArea = new JTextArea(10, 20);  //创建文本域

    //文件列表
    private JList<String> fileListUI=new JList<>();
    private DefaultListModel<String> filelistModel=new DefaultListModel<>();
    private JScrollPane fileListScrollPane=new JScrollPane(fileListUI);
    private String filePath;

    public ClientChat() throws HeadlessException {
        super();
        this.setTitle("客户端");   //设置窗口标题
        this.add(jsp, "Center");      //添加文本域


        jp.add(tf);                //添加文本框
        jp.add(sendBtn);            //添加发送按钮
        jp.add(myGroupBtn);            //添加我的群聊按钮


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
                String targetUser = userListUI.getSelectedValue();  //获取目标用户
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
        //我的群聊按钮添加监听
        myGroupBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //查看我的群聊
                showMyGroupWindow();
            }
        });

        try {
            socket = new Socket(HOST, PORT);   //创建套接字
            dos = new DataOutputStream(socket.getOutputStream());
            while (true) {
                username = "用户" + (int) (Math.random() * 100 + 1);  //随机生成用户名
                dos.writeUTF("CHECK_USERNAME#" + username);
                dos.flush();
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                String response = dis.readUTF();
                if (response.equals("USERNAME_OK")) {
                    break;
                }
            }
            dos.writeUTF(username);  // 发送用户名
            this.setTitle("客户端："+username);   //设置窗口标题
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
                    else if (str.equals("RESPONSE_DOWNLOAD_FILE")) {
                        File file = new File(filePath);
                        byte[] bytes = new byte[1024];
                        dis = new DataInputStream(socket.getInputStream());
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            int length;
                            while ((length = dis.read(bytes)) > 0) {
                                fos.write(bytes, 0, length);

                            }
                            System.out.println("文件下载成功");
                            ta.append("文件下载成功\n");
                        } catch (IOException e) {
                            ta.append("文件下载失败\n");
                            e.printStackTrace();
                        }

                    }
                    else if(str.equals("RESPONSE_CREATE_GROUP")){
                        String groupName=dis.readUTF();
                        System.out.println("创建群聊："+groupName);
                        updateMyGroup(groupName);
                        ta.append("'"+groupName+"'"+"群聊创建成功\n");
                    }
                    else if (str.startsWith("GROUP_CLIENT#")) {
                        System.out.println(str);
                        String[] split = str.split("#");
                        String groupName = split[1];
                        String msg = split[2];
                        if (msg.startsWith(username)) {
                            continue;
                        }
                        groupChatTextArea.append(msg + "\n");
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

    private void updateMyGroup(String s) {
        myGroupModel.addElement(s);
    }

    private void updateUserList(String userList) {
        this.userList = userList;
        userlistModel.clear();
        for (String user : userList.split("\n")) {
            userlistModel.addElement(user);
        }
    }
    private void updateFileList(String fileList){
        this.fileList=fileList;
        filelistModel.clear();
        for (String file : fileList.split("\n")) {
            filelistModel.addElement(file);
        }
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
                dos= new DataOutputStream(socket.getOutputStream());

                byte[] bytes = new byte[1024];
                try (FileInputStream fis = new FileInputStream(file);
                     OutputStream os = socket.getOutputStream()) {
                    int length;
                    while ((length = fis.read(bytes)) > 0) {
                        os.write(bytes, 0, length);
                    }
                }
                dos.flush();
                ta.append("文件发送成功\n");
                System.out.println("文件发送成功");
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
//        JTextArea onlineUsersTextArea = new JTextArea(10, 20);

        JButton refreshButton = new JButton("刷新");

        JButton privateChatBtn=new JButton("私聊");

        JButton groupChatBtn=new JButton("发起群聊");

        JPanel btn_jp = new JPanel();

        JScrollPane scrollPane = new JScrollPane(userListUI);
        userListUI.setModel(userlistModel);

        System.out.println("在线用户列表");
        System.out.println(userList);
        userlistModel.clear();
        for (String user : userList.split("\n")) {
            userlistModel.addElement(user);
        }

        btn_jp.add(refreshButton);
        btn_jp.add(privateChatBtn);
        btn_jp.add(groupChatBtn);

        onlineUsersFrame.add(scrollPane, BorderLayout.CENTER);
        onlineUsersFrame.add(btn_jp, BorderLayout.SOUTH);
        onlineUsersFrame.setSize(300, 200);
        onlineUsersFrame.setLocationRelativeTo(null);
        onlineUsersFrame.setVisible(true);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMsg("REQUEST_USER_LIST");

//                userlistModel.clear();
//                for (String user : userList.split("\n")) {
//                    userlistModel.addElement(user);
//                }
            }
        });
        groupChatBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> selectedUsers = (ArrayList<String>) userListUI.getSelectedValuesList();
                if(selectedUsers.size()>1){
                    String groupChatName = JOptionPane.showInputDialog("请输入群聊名称");
                    System.out.println("CREATE_GROUP#" + groupChatName + "#" + String.join(",", selectedUsers));
                    sendMsg("CREATE_GROUP#" + groupChatName + "#" + String.join(",", selectedUsers));

                    ta.append("创建群聊：" + groupChatName + "\n");

                }

            }
        });
        privateChatBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String targetUser = userListUI.getSelectedValue();

                if(Objects.equals(targetUser, username)){
                    System.out.println("不能选择自己");
                    JOptionPane.showMessageDialog(null,"不能选择自己");
                    return;
                }
                String msg = JOptionPane.showInputDialog("请输入私聊内容");
                if (msg != null) {
                    sendMsg("PRIVATE#" + targetUser + "#" + msg);
                    ta.append("你对" + targetUser+"悄悄说："+msg + "\n");

                }
            }
        });
    }
    //显示我的群聊窗口
    private void showMyGroupWindow(){
        JFrame myGroupFrame=new JFrame("我的群聊");
        JButton refreshButton = new JButton("刷新");
        JButton chatButton = new JButton("聊天");
        JPanel btn_jp = new JPanel();
        btn_jp.add(refreshButton);
        btn_jp.add(chatButton);
        JScrollPane scrollPane = new JScrollPane(myGroup);
        myGroup.setModel(myGroupModel);
        myGroupFrame.add(scrollPane, BorderLayout.CENTER);
        myGroupFrame.add(btn_jp, BorderLayout.SOUTH);
        myGroupFrame.setSize(300, 200);
        myGroupFrame.setLocationRelativeTo(null);
        myGroupFrame.setVisible(true);
//        refreshButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                sendMsg("REQUEST_USER_LIST");
//                myGroupModel.clear();
//                for (String user : userList.split("\n")) {
//                    myGroupModel.addElement(user);
//                }
//            }
//        });
        chatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String groupName = myGroup.getSelectedValue();
                showGroupChatWindow(groupName);
            }
        });
    }

    //群聊窗口
    private void showGroupChatWindow(String groupName){
        JFrame groupChatFrame=new JFrame(groupName+"群聊/"+username);
        JTextField groupChatTextField = new JTextField(20);
        JButton groupChatSendBtn = new JButton("发送");
        JPanel groupChatBtn_jp = new JPanel();
        groupChatTextArea.setEditable(false);
        JScrollPane groupChatScrollPane = new JScrollPane(groupChatTextArea);
        groupChatBtn_jp.add(groupChatTextField);
        groupChatBtn_jp.add(groupChatSendBtn);
        groupChatFrame.add(groupChatScrollPane, BorderLayout.CENTER);
        groupChatFrame.add(groupChatBtn_jp, BorderLayout.SOUTH);
        groupChatFrame.setSize(300, 200);
        groupChatFrame.setLocationRelativeTo(null);
        groupChatFrame.setVisible(true);

        groupChatTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = groupChatTextField.getText();    //获取文本框内容
                if (!str.isEmpty()) {
                    str = username + ":" + str;         //添加前缀
                    groupChatTextArea.append(str + "\n");        //添加到文本域
                    groupChatTextField.setText("");             //清空文本框
                    sendMsg("GROUP_SERVER#" + groupName + "#" + str);              //发送消息
                    System.out.println("GROUP_SERVER#" + groupName + "#" + str);
                }

            }
        });
        groupChatSendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String str = groupChatTextField.getText();    //获取文本框内容
                if (!str.isEmpty()) {
                    str = username + ":" + str;         //添加前缀
                    groupChatTextArea.append(str + "\n");        //添加到文本域
                    groupChatTextField.setText("");             //清空文本框
                    sendMsg("GROUP_SERVER#" + groupName + "#" + str);              //发送消息
                    System.out.println("GROUP_SERVER#" + groupName + "#" + str);
                }

            }
        });

    }
    //查看文件列表窗口
    public void showFileList() {
        JFrame fileListFrame=new JFrame("文件列表");

        JButton refreshButton = new JButton("刷新");
        JButton downloadButton = new JButton("下载");
        JPanel btn_jp = new JPanel();


        fileListUI.setModel(filelistModel);
        JScrollPane fileListScrollPane = new JScrollPane(fileListUI);
        System.out.println("文件列表");
        System.out.println(fileList);

        filelistModel.clear();
        for (String file : fileList.split("\n")) {
            System.out.println(file);
            filelistModel.addElement(file);
        }

        btn_jp.add(refreshButton);
        btn_jp.add(downloadButton);
        fileListFrame.add(fileListScrollPane, BorderLayout.CENTER);
        fileListFrame.add(btn_jp, BorderLayout.SOUTH);
        fileListFrame.setSize(300, 200);
        fileListFrame.setLocationRelativeTo(null);
        fileListFrame.setVisible(true);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMsg("REQUEST_FILE_LIST");
            }
        });
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String file = fileListUI.getSelectedValue();
                String fileName = file.split("/")[1];
                String Dir=file.split("/")[0];
                if (file != null) {
                    //选择保存路径
                    JFileChooser jfc = new JFileChooser();
                    jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    jfc.showOpenDialog(null);
                    File fileDir = jfc.getSelectedFile();
                    if (fileDir != null) {
                        filePath = fileDir.getAbsolutePath() + "\\" + fileName;
                        System.out.println(filePath);
                        sendMsg("DOWNLOAD_FILE#" + Dir+"#"+fileName);
                    }

                }
            }
        });
    }
    public static void main(String[] args) {
        ClientChat clientChat = new ClientChat();


    }
}
