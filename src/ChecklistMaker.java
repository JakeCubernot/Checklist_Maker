import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class ChecklistMaker extends JFrame {

    private JPanel checklistPanel;
    private JLabel emptyMessageLabel;
    private JPopupMenu contextMenu;

    public ChecklistMaker() {
        setTitle("Checklist Maker");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        createMenuBar();
        setupUIComponents();
        setupDragAndDrop();
        setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(createMenuItem("Open", e -> showFileChooser()));
        fileMenu.add(createMenuItem("Exit", e -> System.exit(0)));

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(createMenuItem("Remove Selected Items", e -> removeSelectedItems()));
        editMenu.add(createMenuItem("Clear All Items", e -> removeAllItems()));

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(createMenuItem("About", e -> showAboutDialog()));
        helpMenu.add(createMenuItem("Help", e -> showHelpDialog()));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private JMenuItem createMenuItem(String text, ActionListener action) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(action);
        return menuItem;
    }

    private void setupUIComponents() {
        checklistPanel = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(checklistPanel);
        add(scrollPane, BorderLayout.CENTER);

        emptyMessageLabel = new JLabel("Drag and drop files here or use File -> Open to add files.", SwingConstants.CENTER);
        emptyMessageLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        emptyMessageLabel.setForeground(Color.GRAY);

        checklistPanel.add(emptyMessageLabel, createGridBagConstraints());

        contextMenu = new JPopupMenu();
        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addActionListener(e -> removeSelectedItems());
        contextMenu.add(removeItem);
    }

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }

    private void setupDragAndDrop() {
        new DropTarget(checklistPanel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                handleFileDrop(dtde);
            }
        });
    }

    private void handleFileDrop(DropTargetDropEvent dtde) {
        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            Transferable transferable = dtde.getTransferable();
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                for (File file : fileList) {
                    if (file.isDirectory()) {
                        listFilesInDirectory(file);
                    } else {
                        addFileToChecklist(file);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void listFilesInDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    listFilesInDirectory(file);
                } else {
                    addFileToChecklist(file);
                }
            }
        }
    }

    private void addFileToChecklist(File file) {
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        Icon icon = fileSystemView.getSystemIcon(file);

        JPanel itemPanel = new JPanel(new BorderLayout(10, 0));
        JCheckBox checkBox = new JCheckBox();
        JLabel iconLabel = new JLabel(icon);
        JLabel fileNameLabel = new JLabel(file.getName());

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        contentPanel.add(checkBox);
        contentPanel.add(iconLabel);
        contentPanel.add(fileNameLabel);

        itemPanel.add(contentPanel, BorderLayout.CENTER);
        itemPanel.addMouseListener(createItemMouseListener(checkBox, itemPanel));

        checklistPanel.remove(emptyMessageLabel);
        checklistPanel.add(itemPanel, createGridBagConstraints());
        checklistPanel.revalidate();
        checklistPanel.repaint();
    }

    private MouseAdapter createItemMouseListener(JCheckBox checkBox, JPanel itemPanel) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    contextMenu.show(itemPanel, e.getX(), e.getY());
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    checkBox.setSelected(!checkBox.isSelected());
                }
            }
        };
    }

    private void removeSelectedItems() {
        for (Component component : checklistPanel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel itemPanel = (JPanel) component;
                JCheckBox checkBox = findCheckBoxInPanel(itemPanel);
                if (checkBox != null && checkBox.isSelected()) {
                    checklistPanel.remove(itemPanel);
                }
            }
        }
        updateChecklistPanel();
    }

    private JCheckBox findCheckBoxInPanel(JPanel panel) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel contentPanel = (JPanel) component;
                for (Component c : contentPanel.getComponents()) {
                    if (c instanceof JCheckBox) {
                        return (JCheckBox) c;
                    }
                }
            }
        }
        return null;
    }

    private void updateChecklistPanel() {
        if (checklistPanel.getComponentCount() == 0) {
            checklistPanel.add(emptyMessageLabel, createGridBagConstraints());
        }
        checklistPanel.revalidate();
        checklistPanel.repaint();
    }

    private void removeAllItems() {
        checklistPanel.removeAll();
        checklistPanel.add(emptyMessageLabel, createGridBagConstraints());
        checklistPanel.revalidate();
        checklistPanel.repaint();
    }

    private void showFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.isDirectory()) {
                listFilesInDirectory(selectedFile);
            } else {
                addFileToChecklist(selectedFile);
            }
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this, "Checklist Maker v1.0\nDeveloped by Jake Cubernot", "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showHelpDialog() {
        JOptionPane.showMessageDialog(this, "To use this application:\n\n" +
                "- Drag and drop files here or use File -> Open to add files.\n" +
                "- Right-click on an item to remove it.\n" +
                "- Use Edit -> Remove Selected Items to remove all checked items from the list.\n" +
                "- Use Edit -> Clear All Items to remove all items from the list.", 
                "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChecklistMaker::new);
    }
}
