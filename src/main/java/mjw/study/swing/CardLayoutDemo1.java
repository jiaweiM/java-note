package mjw.study.swing;

import javax.swing.*;
import java.awt.*;

/**
 * @author JiaweiMao
 * @version 1.0.0
 * @since 15 Nov 2021, 10:11 PM
 */
public class CardLayoutDemo1 extends JFrame
{
    private JPanel pane; // 主要的JPanel，该JPanel的布局管理将被设置成CardLayout
    private JPanel p; // 放按钮的JPanel
    private CardLayout card; // CardLayout布局管理器
    private JButton button_1; // 上一步
    private JButton button_2; // 下一步
    private JButton b_1, b_2, b_3; // 三个可直接翻转到JPanel组件的按钮
    private JPanel p_1, p_2, p_3; // 要切换的三个JPanel

    public CardLayoutDemo1()
    {
        super("CardLayout Test");
        try {
            // 将LookAndFeel设置成Windows样式
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        card = new CardLayout(5, 5); //创建一个具有指定的水平和垂直间隙的新卡片布局
        pane = new JPanel(card); // JPanel的布局管理将被设置成CardLayout
        p = new JPanel(); // 构造放按钮的JPanel

        button_1 = new JButton("< 上一步");
        button_2 = new JButton("下一步 >");

        b_1 = new JButton("1");
        b_2 = new JButton("2");
        b_3 = new JButton("3");
        b_1.setMargin(new Insets(2, 2, 2, 2));
        b_2.setMargin(new Insets(2, 2, 2, 2));
        b_3.setMargin(new Insets(2, 2, 2, 2));
        p.add(button_1);
        p.add(b_1);
        p.add(b_2);
        p.add(b_3);
        p.add(button_2);

        p_1 = new JPanel();
        p_2 = new JPanel();
        p_3 = new JPanel();
        p_1.setBackground(Color.RED);
        p_2.setBackground(Color.BLUE);
        p_3.setBackground(Color.GREEN);
        p_1.add(new JLabel("JPanel_1"));
        p_2.add(new JLabel("JPanel_2"));
        p_3.add(new JLabel("JPanel_3"));
        pane.add(p_1, "p1");
        pane.add(p_2, "p2");
        pane.add(p_3, "p3");
        //下面是翻转到卡片布局的某个组件的动作事件处理，当单击某个普通按钮组件，就会触发出现下一个组件
        /// 上一步的按钮动作
        button_1.addActionListener(e -> card.previous(pane));
        // 下一步的按钮动作
        button_2.addActionListener(e -> card.next(pane));
        // 直接翻转到p_1
        b_1.addActionListener(e -> card.show(pane, "p1"));
        // 直接翻转到p_2
        b_2.addActionListener(e -> card.show(pane, "p2"));
        // 直接翻转到p_3
        b_3.addActionListener(e -> card.show(pane, "p3"));

        this.getContentPane().add(pane);
        this.getContentPane().add(p, BorderLayout.SOUTH);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(300, 200);
        this.setVisible(true);
    }

    public static void main(String[] args)
    {
        new CardLayoutDemo1();
    }
}
