package client.view;

import javax.swing.*;
import java.awt.*;

/**
 * 注册对话框 - 新用户注册界面
 * <p>
 * 【核心作用】
 * 1. 收集用户注册信息（昵称、密码、性别）
 * 2. 进行输入验证（非空、密码一致性）
 * 3. 返回用户填写的信息给调用方
 * <p>
 * 【界面组成】
 * - 昵称输入框
 * - 密码输入框
 * - 确认密码输入框
 * - 性别选择下拉框（男/女/保密）
 * - 注册/取消按钮
 * <p>
 * 【使用方式】
 * 
 * <pre>
 * RegisterDialog dialog = new RegisterDialog(parentFrame);
 * dialog.setVisible(true);
 * if (dialog.isConfirmed()) {
 *     String nickname = dialog.getNickname();
 *     String password = dialog.getPassword();
 *     String gender = dialog.getGender();
 * }
 * </pre>
 * 
 * @author ChatRoom Team
 */
public class RegisterDialog extends JDialog {

    private final JTextField nicknameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final JComboBox<String> genderCombo;
    private final JButton registerBtn;
    private final JButton cancelBtn;

    private boolean confirmed = false;

    public RegisterDialog(JFrame parent) {
        super(parent, "用户注册", true);
        setSize(320, 250);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 昵称
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("昵称:"), gbc);
        gbc.gridx = 1;
        nicknameField = new JTextField(15);
        panel.add(nicknameField, gbc);

        // 密码
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);

        // 确认密码
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("确认密码:"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(15);
        panel.add(confirmPasswordField, gbc);

        // 性别
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("性别:"), gbc);
        gbc.gridx = 1;
        genderCombo = new JComboBox<>(new String[] { "男", "女", "保密" });
        panel.add(genderCombo, gbc);

        // 按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        registerBtn = new JButton("注册");
        cancelBtn = new JButton("取消");
        btnPanel.add(registerBtn);
        btnPanel.add(cancelBtn);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        add(panel);

        // 按钮事件
        registerBtn.addActionListener(e -> {
            if (validateInput()) {
                confirmed = true;
                dispose();
            }
        });

        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
    }

    private boolean validateInput() {
        String nickname = nicknameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPwd = new String(confirmPasswordField.getPassword());

        if (nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "昵称不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "密码不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (!password.equals(confirmPwd)) {
            JOptionPane.showMessageDialog(this, "两次密码输入不一致", "提示", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getNickname() {
        return nicknameField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public String getGender() {
        return (String) genderCombo.getSelectedItem();
    }
}
