import org.jnetpcap.PcapIf;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class UILayer extends BaseLayer {

    private static final LayerManager LAYER_MANAGER = new LayerManager();

    private final Dimension preferredDimension = new Dimension(80, 24);

    private JTextArea chatTextArea;
    private JScrollPane chatTextPane;
    private JTextField chatInputField;
    private JButton chatSendButton;
    private JTextField filePathField;
    private JButton fileSelectButton;
    private JButton fileSendButton;
    private JProgressBar fileProgressBar;
    private JComboBox<String> networkInterfaceComboBox;
    private JTextField sourceAddressField;
    private JTextField destinationAddressField;
    private JButton settingButton;

    private String filePath = null;

    public UILayer(String name) {
        super(name);

        // UI Frame
        JFrame frame = new JFrame();
        frame.add(createUIPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        LAYER_MANAGER.addLayer(new NILayer("NI"));
        LAYER_MANAGER.addLayer(new EthernetLayer("Ethernet"));
        LAYER_MANAGER.addLayer(new ChatAppLayer("ChatApp"));
        LAYER_MANAGER.addLayer(new UILayer("GUI"));
        LAYER_MANAGER.connectLayers(" NI ( *Ethernet ( *ChatApp ( *GUI ) ) )");
    }

    private JPanel createUIPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        JPanel interactivePanel = new JPanel();
        interactivePanel.setLayout(new BoxLayout(interactivePanel, BoxLayout.Y_AXIS));

        interactivePanel.add(createChatPanel());
        interactivePanel.add(createFilePanel());

        mainPanel.add(interactivePanel, BorderLayout.CENTER);
        mainPanel.add(createSettingPanel(), BorderLayout.EAST);

        return mainPanel;
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(0, 0));
        chatTextArea = new JTextArea(12, 40);
        chatTextPane = new JScrollPane(chatTextArea);
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInputField = new JTextField();
        chatSendButton = new JButton("Send");

        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatTextPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        chatTextPane.setMinimumSize(preferredDimension);
        chatTextPane.setAutoscrolls(true);
        chatTextArea.setEditable(false);
        chatTextArea.setBorder(UIManager.getBorder("TitledBorder.border"));
        chatInputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    chatSendButton.doClick();
            }
        });
        chatSendButton.setPreferredSize(preferredDimension);
        chatSendButton.addActionListener(actionEvent -> {
            if (settingButton.getText().equals("Reset")) {
                String inputString = chatInputField.getText();
                chatInputField.setText("");
                chatTextArea.append("[SEND] : " + inputString + "\n");

                byte[] data = inputString.getBytes();
                LAYER_MANAGER.getLayer("ChatApp").send(data, data.length);

                JScrollBar chatTextPaneVerticalScrollBar = chatTextPane.getVerticalScrollBar();
                chatTextPaneVerticalScrollBar.setValue(chatTextPaneVerticalScrollBar.getMaximum());
            }
        });

        chatInputPanel.add(chatInputField, BorderLayout.CENTER);
        chatInputPanel.add(chatSendButton, BorderLayout.EAST);

        chatPanel.add(chatTextPane, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
        return chatPanel;
    }

    private JPanel createFilePanel() {
        JPanel filePanel = new JPanel(new BorderLayout(0, 0));
        filePathField = new JTextField();
        JPanel fileButtonPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        fileSelectButton = new JButton("Select");
        fileSendButton = new JButton("Send");
        fileProgressBar = new JProgressBar();

        filePanel.setBorder(BorderFactory.createTitledBorder("File Transfer"));
        filePathField.setPreferredSize(preferredDimension);
        filePathField.setEditable(false);
        fileSelectButton.setPreferredSize(preferredDimension);
        fileSelectButton.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            int fileChooserReturnValue = fileChooser.showOpenDialog(filePanel);
            if (fileChooserReturnValue == JFileChooser.APPROVE_OPTION) {
                filePathField.setEditable(false);
                filePathField.setText(fileChooser.getSelectedFile().getName());
                filePath = fileChooser.getSelectedFile().toString();
                fileSendButton.setEnabled(true);
            } else if (fileChooserReturnValue == JFileChooser.CANCEL_OPTION) {
                filePathField.setText("");
                filePath = null;
                filePathField.setEditable(true);
                fileSendButton.setEnabled(false);
            }
        });
        fileSendButton.setPreferredSize(preferredDimension);
        fileSendButton.setEnabled(false);
        fileSendButton.addActionListener(actionEvent -> {
            // TODO Implement File Transfer
//            File file = new File(filePath);
//            if (file.canRead()) {
//                chatTextArea.append(
//                        "[FILE] : File Name - " + file.getName() + "\n" + "[FILE] : Waiting for opponent to accept\n");
//                LAYER_MANAGER.getLayer("FileApp").send(filePath);
//            }
        });
        fileProgressBar.setPreferredSize(preferredDimension);

        fileButtonPanel.add(fileSelectButton);
        fileButtonPanel.add(fileSendButton);

        filePanel.add(filePathField, BorderLayout.CENTER);
        filePanel.add(fileButtonPanel, BorderLayout.EAST);
        filePanel.add(fileProgressBar, BorderLayout.SOUTH);
        return filePanel;
    }

    private JPanel createSettingPanel() {
        JPanel settingPanel = new JPanel(new GridLayout(10, 1, 0, 0));
        JLabel networkInterfaceLabel = new JLabel("NIC Select");
        networkInterfaceComboBox = new JComboBox<>();
        JLabel sourceAddressLabel = new JLabel("Source Address");
        sourceAddressField = new JTextField();
        JLabel destinationAddressLabel = new JLabel("Destination Address");
        destinationAddressField = new JTextField();
        settingButton = new JButton("Setting");

        Dimension settingDefaultDimension = new Dimension(140, 24);

        settingPanel.setBorder(BorderFactory.createTitledBorder("Setting"));
        networkInterfaceLabel.setPreferredSize(settingDefaultDimension);
        networkInterfaceComboBox.setPreferredSize(settingDefaultDimension);
        {
            NILayer niLayer = (NILayer) LAYER_MANAGER.getLayer("NI");
            for (int index = 0; index < niLayer.getInterfaceList().size(); index++) {
                PcapIf pcapIf = niLayer.getInterfaceList().get(index);
                networkInterfaceComboBox.addItem(pcapIf.getName());
            }
        }
        networkInterfaceComboBox.addActionListener(actionEvent -> {
            if (networkInterfaceComboBox.getSelectedItem() != null)
                sourceAddressField.setText(((NILayer) LAYER_MANAGER.getLayer("NI"))
                        .getInterfaceMACAddress(networkInterfaceComboBox.getSelectedItem().toString()));
        });
        sourceAddressLabel.setPreferredSize(settingDefaultDimension);
        sourceAddressField.setPreferredSize(settingDefaultDimension);
        sourceAddressField.setEditable(false);
        destinationAddressLabel.setPreferredSize(settingDefaultDimension);
        destinationAddressField.setPreferredSize(settingDefaultDimension);
        settingButton.setPreferredSize(settingDefaultDimension);
        settingButton.addActionListener(actionEvent -> {
            if (settingButton.getText().equals("Reset")) {
                sourceAddressField.setText("");
                destinationAddressField.setText("");
                settingButton.setText("Setting");
                sourceAddressField.setEnabled(true);
                destinationAddressField.setEnabled(true);
            } else {
                byte[] srcByteAddr = new byte[6];
                byte[] dstByteAddr = new byte[6];
                String[] srcAddr = sourceAddressField.getText().split("-");
                String[] dstAddr = destinationAddressField.getText().split("-");
                for (int index = 0; index < 6; index++) {
                    srcByteAddr[index] = (byte) Integer.parseInt(srcAddr[index], 16);
                    dstByteAddr[index] = (byte) Integer.parseInt(dstAddr[index], 16);
                }
                ((EthernetLayer) LAYER_MANAGER.getLayer("Ethernet")).setSourceAddress(srcByteAddr);
                ((EthernetLayer) LAYER_MANAGER.getLayer("Ethernet")).setDestinationAddress(dstByteAddr);
                if (networkInterfaceComboBox.getSelectedItem() != null)
                    ((NILayer) LAYER_MANAGER.getLayer("NI"))
                            .setInterface(networkInterfaceComboBox.getSelectedItem().toString());

                settingButton.setText("Reset");
                sourceAddressField.setEnabled(false);
                destinationAddressField.setEnabled(false);
            }
        });

        settingPanel.add(networkInterfaceLabel);
        settingPanel.add(networkInterfaceComboBox);
        settingPanel.add(sourceAddressLabel);
        settingPanel.add(sourceAddressField);
        settingPanel.add(destinationAddressLabel);
        settingPanel.add(destinationAddressField);
        settingPanel.add(settingButton);

        return settingPanel;
    }

    @Override
    public synchronized boolean receive(byte[] dataArray, String layerName) {
        if (dataArray == null) {
            printError("null data");
            return false;
        }
        if (layerName == null) {
            printError("unknown layer");
            return false;
        }

        print("receive : " + String.format("%s from %s", dataArray.toString(), layerName));
        printHex(dataArray, dataArray.length);

        switch (layerName) {
            case "ChatApp":
                chatTextArea.append("[RECV] : " + new String(dataArray) + "\n");
                JScrollBar chatTextPaneVerticalScrollBar = chatTextPane.getVerticalScrollBar();
                chatTextPaneVerticalScrollBar.setValue(chatTextPaneVerticalScrollBar.getMaximum());
                break;
            case "FileApp":
                break;
            default:
                printError("undefined type");
                return false;
        }
        return true;
    }

}
