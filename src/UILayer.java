import org.jnetpcap.PcapIf;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class UILayer extends JFrame implements LayerInterface {

    private static final long serialVersionUID = 1L;
    private static final LayerManager layerManager = new LayerManager();

    private final Dimension preferredDimension = new Dimension(80, 24);
    private final HashMap<String, Component> directory = new HashMap<>();
    private final ArrayList<LayerInterface> upperLayerList = new ArrayList<>();
    private final String layerName;
    private LayerInterface underLayer = null;
    private String filePath = null;

    public UILayer(String name) {
        layerName = name;
        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        directory.put("mainPanel", mainPanel);
        JPanel interactivePanel = new JPanel();
        interactivePanel.setLayout(new BoxLayout(interactivePanel, BoxLayout.Y_AXIS));
        directory.put("interactivePanel", interactivePanel);

        interactivePanel.add(createChatPanel());
        interactivePanel.add(createFilePanel());

        mainPanel.add(interactivePanel, BorderLayout.CENTER);
        mainPanel.add(createSettingPanel(), BorderLayout.EAST);

        this.add(mainPanel);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setVisible(true);
    }

    public static void main(String[] args) {
        layerManager.addLayer(new NILayer("NI"));
        layerManager.addLayer(new EthernetLayer("Ethernet"));
        layerManager.addLayer(new ChatAppLayer("ChatApp"));
        layerManager.addLayer(new FileAppLayer("FileApp"));
        layerManager.addLayer(new UILayer("GUI"));
        layerManager.connectLayers(" NI ( *Ethernet ( *ChatApp ( *GUI ) )");
    }

    private LayerManager getLayerManager() {
        return UILayer.layerManager;
    }

    @Override
    public boolean receive(byte[] data) {
        if (data != null) {
            ((JTextArea) directory.get("chatTextArea")).append("[RECV] : " + new String(data) + "\n");
            JScrollBar chatTextPaneVerticalScrollBar = ((JScrollPane) directory.get("chatTextPane")).getVerticalScrollBar();
            chatTextPaneVerticalScrollBar.setValue(chatTextPaneVerticalScrollBar.getMaximum());
        }
        return false;
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(0, 0));
        directory.put("chatPanel", chatPanel);
        JTextArea chatTextArea = new JTextArea(12, 40);
        directory.put("chatTextArea", chatTextArea);
        JScrollPane chatTextPane = new JScrollPane(chatTextArea);
        directory.put("chatTextPane", chatTextPane);
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        directory.put("chatInputPanel", chatInputPanel);
        JTextField chatInputField = new JTextField();
        directory.put("chatInputField", chatInputField);
        JButton chatSendButton = new JButton("Send");
        directory.put("chatSendButton", chatSendButton);

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
            if (((JButton) directory.get("settingButton")).getText().equals("Reset")) {
                String inputString = chatInputField.getText();
                chatTextArea.append("[SEND] : " + inputString + "\n");

                byte[] data = inputString.getBytes();
                layerManager.getLayer("ChatApp").send(data, data.length);

                JScrollBar chatTextPaneVerticalScrollBar = ((JScrollPane) directory.get("chatTextPane")).getVerticalScrollBar();
                chatTextPaneVerticalScrollBar.setValue(chatTextPaneVerticalScrollBar.getMaximum());
                chatInputField.setText("");
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
        directory.put("filePanel", filePanel);
        JTextField filePathField = new JTextField();
        directory.put("filePathField", filePathField);
        JPanel fileButtonPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        directory.put("fileButtonPanel", fileButtonPanel);
        JButton fileSelectButton = new JButton("Select");
        directory.put("fileSelectButton", fileSelectButton);
        JButton fileSendButton = new JButton("Send");
        directory.put("fileSendButton", fileSendButton);
        JProgressBar fileProgressBar = new JProgressBar();
        directory.put("fileProgressBar", fileProgressBar);


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
        directory.put("settingPanel", settingPanel);
        JLabel networkInterfaceLabel = new JLabel("NIC Select");
        directory.put("networkInterfaceLabel", networkInterfaceLabel);
        JComboBox<String> networkInterfaceComboBox = new JComboBox<>();
        directory.put("networkInterfaceComboBox", networkInterfaceComboBox);
        JLabel sourceAddressLabel = new JLabel("Source Address");
        directory.put("sourceAddressLabel", sourceAddressLabel);
        JTextField sourceAddressField = new JTextField();
        directory.put("sourceAddressField", sourceAddressField);
        JLabel destinationAddressLabel = new JLabel("Destination Address");
        directory.put("destinationAddressLabel", destinationAddressLabel);
        JTextField destinationAddressField = new JTextField();
        directory.put("destinationAddressField", destinationAddressField);
        JButton settingButton = new JButton("Setting");
        directory.put("settingButton", settingButton);

        Dimension settingDefaultDimension = new Dimension(140, 24);

        settingPanel.setBorder(BorderFactory.createTitledBorder("Setting"));
        networkInterfaceLabel.setPreferredSize(settingDefaultDimension);
        networkInterfaceComboBox.setPreferredSize(settingDefaultDimension);
        {
            NILayer niLayer = (NILayer) layerManager.getLayer("NI");
            for (int index = 0; index < niLayer.getAdapterList().size(); index++) {
                PcapIf pcapIf = niLayer.getAdapterList().get(index);
                networkInterfaceComboBox.addItem(pcapIf.getName());
            }
        }
        networkInterfaceComboBox.addActionListener(actionEvent -> {
            if (networkInterfaceComboBox.getSelectedItem() != null)
                sourceAddressField.setText(((NILayer) layerManager.getLayer("NI")).getAdapterMACAddress(networkInterfaceComboBox.getSelectedItem().toString()));
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
                ((EthernetLayer) layerManager.getLayer("Ethernet")).setSourceAddress(srcByteAddr);
                ((EthernetLayer) layerManager.getLayer("Ethernet")).setDestinationAddress(dstByteAddr);
                if (networkInterfaceComboBox.getSelectedItem() != null)
                    ((NILayer) layerManager.getLayer("NI")).setAdapter(networkInterfaceComboBox.getSelectedItem().toString());

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
    public String getLayerName() {
        return layerName;
    }

    @Override
    public LayerInterface getUnderLayer() {
        return underLayer;
    }

    @Override
    public void setUnderLayer(LayerInterface uLayer) {
        this.underLayer = uLayer;
    }

    @Override
    public LayerInterface getUpperLayer(int index) {
        if (index < 0 || index > upperLayerList.size()) return null;
        return upperLayerList.get(index);
    }

    @Override
    public void setUpperLayer(LayerInterface uLayer) {
        if (uLayer == null) return;
        this.upperLayerList.add(upperLayerList.size(), uLayer);
    }

    @Override
    public void setUpperUnderLayer(LayerInterface upperUnderLayer) {
        this.setUpperLayer(upperUnderLayer);
        upperUnderLayer.setUnderLayer(this);
    }
}
