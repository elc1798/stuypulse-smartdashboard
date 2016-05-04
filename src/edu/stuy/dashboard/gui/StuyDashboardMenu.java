package edu.stuy.dashboard.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jfree.ui.ExtensionFileFilter;

import edu.stuy.dashboard.workarounds.MainPanel;
import edu.stuy.dashboard.workarounds.PropertyEditor;
import edu.wpi.first.smartdashboard.gui.DisplayElement;
import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.gui.Widget;
import edu.wpi.first.smartdashboard.livewindow.elements.Controller;
import edu.wpi.first.smartdashboard.livewindow.elements.LWSubsystem;
import edu.wpi.first.smartdashboard.robot.Robot;
import edu.wpi.first.smartdashboard.types.DisplayElementRegistry;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;

@SuppressWarnings("serial")
public class StuyDashboardMenu extends JMenuBar {

    private JMenu fileMenu;
    private JMenu viewMenu;

    public StuyDashboardMenu(final StuyDashboardFrame frame, final MainPanel mainPanel) {
        fileMenu = new JMenu("File");

        JMenuItem loadMenu = new JMenuItem("Open...");
        loadMenu.setAccelerator(KeyStroke.getKeyStroke(79, 128));
        loadMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File((String) frame.getPrefs().saveFile.getValue()));
                fc.addChoosableFileFilter(new ExtensionFileFilter("XML File", ".xml"));
                fc.setMultiSelectionEnabled(false);
                fc.setFileSelectionMode(0);
                fc.setApproveButtonText("Open");
                fc.setDialogTitle("Open");

                if (fc.showOpenDialog(frame) == 0) {
                    String filepath = fc.getSelectedFile().getAbsolutePath();

                    frame.load(filepath);
                    frame.getPrefs().saveFile.setValue(filepath);
                }
            }
        });
        fileMenu.add(loadMenu);

        JMenuItem newMenu = new JMenuItem("New");
        newMenu.setAccelerator(KeyStroke.getKeyStroke(78, 128));
        newMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.getPanel("SmartDashboard").clear();
            }
        });
        fileMenu.add(newMenu);

        JMenuItem saveMenu = new JMenuItem("Save");
        saveMenu.setAccelerator(KeyStroke.getKeyStroke(83, 128));
        saveMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.save((String) frame.getPrefs().saveFile.getValue());
            }
        });
        fileMenu.add(saveMenu);

        JMenuItem saveAs = new JMenuItem("Save As...");
        saveAs.setAccelerator(KeyStroke.getKeyStroke(83, 192));
        saveAs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(".");
                fc.addChoosableFileFilter(new ExtensionFileFilter("XML File", ".xml"));
                fc.setApproveButtonText("Save");
                fc.setDialogTitle("Save As...");

                fc.setMultiSelectionEnabled(false);
                fc.setFileSelectionMode(0);

                if (fc.showOpenDialog(frame) == 0) {
                    String filepath = fc.getSelectedFile().getAbsolutePath();
                    if (!filepath.endsWith(".xml")) {
                        filepath = filepath + ".xml";
                    }
                    frame.save(filepath);
                    frame.getPrefs().saveFile.setValue(filepath);
                }
            }
        });
        fileMenu.add(saveAs);

        JMenuItem prefMenu = new JMenuItem("Preferences");
        prefMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PropertyEditor editor = frame.getPropertyEditor();
                editor.setPropertyHolder(frame.getPrefs());
                editor.setTitle("Edit Preferences");
                editor.setVisible(true);
            }
        });
        fileMenu.add(prefMenu);

        JMenuItem exitMenu = new JMenuItem("Exit");

        exitMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.exit();
            }
        });
        fileMenu.add(exitMenu);

        viewMenu = new JMenu("View");
        JCheckBoxMenuItem editMode = new JCheckBoxMenuItem("Editable");
        editMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (StuyDashboardPanel panel : MainPanel.panels.values())
                    panel.setEditable(!panel.isEditable());
            }
        });
        editMode.setAccelerator(KeyStroke.getKeyStroke(69, 128));
        viewMenu.add(editMode);

        JCheckBoxMenuItem editSystems = new JCheckBoxMenuItem("Edit Subsystems");
        editSystems.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LWSubsystem.setEditable(!LWSubsystem.isEditable());
            }
        });
        editSystems.setAccelerator(KeyStroke.getKeyStroke(69, 192));
        editSystems.doClick();
        viewMenu.add(editSystems);

        final JMenuItem resetLW = new JMenuItem("Reset LiveWindow");
        resetLW.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (LWSubsystem subsystem : MainPanel.getPanel("LiveWindow").getSubsystems())
                    for (Widget component : subsystem.getWidgets())
                        if ((component instanceof Controller)) {
                            System.out.println("\tResetting " + component.getFieldName());
                            ((Controller) component).reset();
                        }
            }
        });
        resetLW.setAccelerator(KeyStroke.getKeyStroke(82, 128));
        Robot.getLiveWindow().getSubTable("~STATUS~").addTableListenerEx("LW Enabled", new ITableListener() {
            public void valueChanged(ITable itable, String string, Object o, boolean bln) {
                final boolean isInLW = Robot.getLiveWindow().getSubTable("~STATUS~").getBoolean("LW Enabled", false);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        frame.setDisplayMode(isInLW ? StuyDashboardFrame.DisplayMode.LiveWindow : StuyDashboardFrame.DisplayMode.SmartDashboard);

                        mainPanel.setCurrentPanel(isInLW ? MainPanel.getPanel("LiveWindow") : MainPanel.getPanel("SmartDashboard"));

                        if (!isInLW)
                            resetLW.doClick();
                    }
                });
            }
        }, 23);

        viewMenu.add(resetLW);

        JMenu addMenu = new JMenu("Add...");
        Set<Class<? extends StaticWidget>> panels = DisplayElementRegistry.getStaticWidgets();
        for (Object obj : panels) {
            final Class option = (Class) obj;
            JMenuItem item = new JMenuItem(DisplayElement.getName(option));
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        StaticWidget element = (StaticWidget) option.newInstance();
                        MainPanel.getPanel("SmartDashboard").addElement(element, null);
                    } catch (InstantiationException localInstantiationException) {
                    } catch (IllegalAccessException localIllegalAccessException) {
                    }
                }
            });
            addMenu.add(item);
        }

        viewMenu.add(addMenu);

        final JMenu revealMenu = new JMenu("Reveal...");

        viewMenu.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent e) {
                revealMenu.removeAll();

                int count = 0;
                for (final String field : MainPanel.getPanel("SmartDashboard").getHiddenFields()) {
                    if (MainPanel.getPanel("SmartDashboard").getTable().containsKey(field)) {
                        count++;
                        revealMenu.add(new JMenuItem(new AbstractAction(field) {
                            public void actionPerformed(ActionEvent e) {
                                MainPanel.getPanel("SmartDashboard").addField(field);
                            }
                        }));
                    }
                }

                revealMenu.setEnabled(count != 0);
            }

            public void menuDeselected(MenuEvent e) {
            }

            public void menuCanceled(MenuEvent e) {
            }
        });
        viewMenu.add(revealMenu);

        JMenuItem removeUnusedMenu = new JMenuItem("Remove Unused");
        removeUnusedMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.getCurrentPanel().removeUnusedFields();
            }
        });
        viewMenu.add(removeUnusedMenu);

        add(fileMenu);
        add(viewMenu);
    }

    public void setBackground(Color c) {
        super.setBackground(c);
        try {
            fileMenu.setBackground(c);
        } catch (Exception e) {
            System.err.println("File Menu not initialized yet!");
        }
        try {
            viewMenu.setBackground(c);
        } catch (Exception e) {
            System.err.print("View Menu not intialized yet!");
        }
    }
}
