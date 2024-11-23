import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        JList<String> list = new JList<>();
        DefaultListModel<String> model = new DefaultListModel<>();
        JScrollPane scrollPane = new JScrollPane(list);
        JButton button = new JButton("Add");
        JPanel panel = new JPanel();

        panel.add(scrollPane);
        panel.add(button);

        button.addActionListener(e -> {
            model.addElement("Element");
        });


        frame.add(panel, BorderLayout.CENTER);
        list.setModel(model);
        frame.setSize(300, 300);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }
}