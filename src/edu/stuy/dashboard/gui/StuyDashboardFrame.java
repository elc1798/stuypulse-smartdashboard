package edu.stuy.dashboard.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import edu.stuy.dashboard.utils.Color;
import edu.stuy.dashboard.workarounds.LogToCSV;
import edu.stuy.dashboard.workarounds.MainPanel;
import edu.stuy.dashboard.workarounds.PropertyEditor;
import edu.wpi.first.smartdashboard.gui.DashboardFrame;
import edu.wpi.first.smartdashboard.gui.DisplayElement;
import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.gui.Widget;
import edu.wpi.first.smartdashboard.gui.elements.bindings.AbstractTableWidget;
import edu.wpi.first.smartdashboard.livewindow.elements.LWSubsystem;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.robot.Robot;
import edu.wpi.first.smartdashboard.types.DataType;
import edu.wpi.first.smartdashboard.types.DisplayElementRegistry;
import edu.wpi.first.smartdashboard.xml.SmartDashboardXMLReader;
import edu.wpi.first.smartdashboard.xml.SmartDashboardXMLWriter;
import edu.wpi.first.smartdashboard.xml.XMLWidget;

@SuppressWarnings("serial")
public class StuyDashboardFrame extends JFrame {

    private static final int MENU_HEADER = 10;
    private static final Dimension NETBOOK_SIZE = new Dimension(1024, 400);

    private static final Dimension MINIMUM_SIZE = new Dimension(300, 200);

    private final StuyDashboardPrefs prefs = new StuyDashboardPrefs(this);
    private final StuyDashboardPanel smartDashboardPanel;
    private final StuyDashboardPanel liveWindowPanel;
    private final MainPanel mainPanel;
    private DisplayMode displayMode = DisplayMode.SmartDashboard;
    private final StuyDashboardMenu menuBar;
    private final PropertyEditor propEditor;
    private boolean shouldHideMenu = ((Boolean) this.prefs.hideMenu.getValue()).booleanValue();

    private static final String LW_SAVE = "_" + Robot.getLiveWindow().getSubTable("~STATUS~").getString("Robot", "LiveWindow") + ".xml";

    private final LogToCSV logger = new LogToCSV(this);

    private static StuyDashboardFrame INSTANCE = null;

    public static StuyDashboardFrame getInstance() {
        return INSTANCE;
    }

    public StuyDashboardFrame(boolean competition) {
        super("SmartDashboard - ");

        setLayout(new BorderLayout());

        this.smartDashboardPanel = new StuyDashboardPanel(this, Robot.getTable());
        this.smartDashboardPanel.setName("SmartDashboard");

        this.liveWindowPanel = new StuyDashboardPanel(this, Robot.getLiveWindow());
        this.liveWindowPanel.setName("LiveWindow");

        this.mainPanel = new MainPanel(new CardLayout(), this.smartDashboardPanel, new StuyDashboardPanel[] {
            this.liveWindowPanel, this.smartDashboardPanel
        });
        this.mainPanel.add(this.smartDashboardPanel, DisplayMode.SmartDashboard.toString());
        this.mainPanel.add(this.liveWindowPanel, DisplayMode.LiveWindow.toString());

        setDisplayMode(DisplayMode.SmartDashboard);
        this.menuBar = new StuyDashboardMenu(this, this.mainPanel);

        this.propEditor = new PropertyEditor(this);

        if (!this.shouldHideMenu) {
            add(this.menuBar, "North");
        }
        add(this.mainPanel, "Center");

        MouseAdapter hideListener = new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                if ((StuyDashboardFrame.this.shouldHideMenu) && (e.getY() < 10)) {
                    StuyDashboardFrame.this.add(StuyDashboardFrame.this.menuBar, "North");
                    StuyDashboardFrame.this.validate();
                }
            }

            public void mouseEntered(MouseEvent e) {
                if (StuyDashboardFrame.this.shouldHideMenu) {
                    StuyDashboardFrame.this.remove(StuyDashboardFrame.this.menuBar);
                    StuyDashboardFrame.this.validate();
                }
            }
        };
        this.smartDashboardPanel.addMouseListener(hideListener);
        this.smartDashboardPanel.addMouseMotionListener(hideListener);

        if (competition) {
            setPreferredSize(NETBOOK_SIZE);
            setUndecorated(true);
            setLocation(0, 0);
            setResizable(false);
        } else {
            setMinimumSize(MINIMUM_SIZE);

            setDefaultCloseOperation(0);

            setPreferredSize(new Dimension(this.prefs.width.getValue().intValue(), this.prefs.height.getValue().intValue()));
            setLocation(this.prefs.x.getValue().intValue(), this.prefs.y.getValue().intValue());
        }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                StuyDashboardFrame.this.exit();
            }
        });
        addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                StuyDashboardFrame.this.prefs.width.setValue(Integer.valueOf(StuyDashboardFrame.this.getWidth()));
                StuyDashboardFrame.this.prefs.height.setValue(Integer.valueOf(StuyDashboardFrame.this.getHeight()));
            }

            public void componentMoved(ComponentEvent e) {
                StuyDashboardFrame.this.prefs.x.setValue(Integer.valueOf(StuyDashboardFrame.this.getX()));
                StuyDashboardFrame.this.prefs.y.setValue(Integer.valueOf(StuyDashboardFrame.this.getY()));
            }

            public void componentShown(ComponentEvent e) {
            }

            public void componentHidden(ComponentEvent e) {
            }
        });

        INSTANCE = this;

        setMenuBarColor(new Color(0, 255, 0));
        setBodyColor(new Color(0, 0, 0));
    }

    public void setMenuBarColor(Color c) {
        this.menuBar.setColorScheme(c);
    }

    public void setBodyColor(Color c) {
        this.mainPanel.setColorScheme(c);
        this.smartDashboardPanel.setColorScheme(c);

        this.setBackground(c);
        this.setForeground(c.inverted());
    }

    public final void setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
        CardLayout cl = (CardLayout) this.mainPanel.getLayout();
        cl.show(this.mainPanel, mode.toString());
    }

    public PropertyEditor getPropertyEditor() {
        return this.propEditor;
    }

    public void setShouldHideMenu(boolean shouldHide) {
        if (this.shouldHideMenu != shouldHide) {
            this.shouldHideMenu = shouldHide;
            if (this.shouldHideMenu)
                remove(this.menuBar);
            else {
                add(this.menuBar, "North");
            }
            validate();
        }
    }

    public void save(String path) {
        try {
            System.out.println("Saving to:\t" + path);
            SmartDashboardXMLWriter writer = new SmartDashboardXMLWriter(path);

            writer.beginSmartDashboard();
            saveElements(writer, this.smartDashboardPanel);
            writer.endSmartDashboard();

            writer.beginLiveWindow();
            saveElements(writer, this.liveWindowPanel);
            writer.endLiveWindow();

            for (String field : this.smartDashboardPanel.getHiddenFields()) {
                writer.addHiddenField(field);
            }

            writer.close();
        } catch (Exception ex) {
            Logger.getLogger(DashboardFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void saveElements(SmartDashboardXMLWriter writer, StuyDashboardPanel toSave) throws IOException {
        for (DisplayElement element : toSave.getElements()) {
            boolean isWidget = element instanceof Widget;
            assert ((isWidget) || ((element instanceof StaticWidget)));
            if (isWidget)
                writer.beginWidget(((Widget) element).getFieldName(), ((Widget) element).getType().getName(), element.getClass().getName());
            else
                writer.beginStaticWidget(element.getClass().getName());
            Iterator localIterator2;
            if ((element instanceof LWSubsystem))
                for (localIterator2 = ((LWSubsystem) element).getWidgets().iterator(); localIterator2.hasNext();) {
                Widget w = (Widget) localIterator2.next();
                System.out.println("   Saving " + ((LWSubsystem) element).getFieldName() + "|" + w.getFieldName());
                writer.addSubWidget(w.getFieldName(), w.getType().getName(), w.getClass().getName());
                writer.addSubWidgetLocation(w.getLocation());
                writer.addSubWudgetHeight(w.getHeight());
                writer.addSubWidgetWidth(w.getWidth());
                writer.endSubWidget();
            }
            Widget w;
            writer.addLocation(element.getLocation());

            Dimension size = element.getSavedSize();
            if (size.width > 0) {
                writer.addWidth(size.width);
            }
            if (size.height > 0) {
                writer.addHeight(size.height);
            }

            for (Map.Entry prop : element.getProperties().entrySet()) {
                if (!((Property) prop.getValue()).isDefault()) {
                    writer.addProperty((String) prop.getKey(), ((Property) prop.getValue()).getSaveValue());
                }
            }

            if (isWidget)
                writer.endWidget();
            else
                writer.endStaticWidget();
        }
    }

    public void load(String path) {
        try {
            SmartDashboardXMLReader reader = new SmartDashboardXMLReader(path);

            System.out.println("\nLoading from \t" + path);

            List sdWidgets = reader.getXMLWidgets();
            for (int i = sdWidgets.size(); i > 0; i--) {
                System.out.println("Loading SmartDashboard widgets....");
                XMLWidget widget = (XMLWidget) sdWidgets.get(i - 1);
                DisplayElement element = widget.convertToDisplayElement();
                if ((element instanceof Widget)) {
                    Widget e = (Widget) element;
                    Object value = null;
                    if (Robot.getTable().containsKey(e.getFieldName())) {
                        value = Robot.getTable().getValue(e.getFieldName());
                        DataType type = DataType.getType(value);
                        if (DisplayElementRegistry.supportsType(e.getClass(), type))
                            this.smartDashboardPanel.setField(e.getFieldName(), e, type, value, e.getSavedLocation());
                    } else {
                        this.smartDashboardPanel.setField(e.getFieldName(), e, widget.getType(), null, e.getSavedLocation());
                    }
                } else if ((element instanceof StaticWidget)) {
                    StaticWidget e = (StaticWidget) element;
                    this.smartDashboardPanel.addElement(e, widget.getLocation());
                }

            }

            LWSubsystem mostRecentParent = null;
            for (XMLWidget subsys : reader.getSubsystems().keySet()) {
                System.out.println("\nLoading \"" + subsys.getField() + "\"");
                LWSubsystem subsystem = (LWSubsystem) subsys.convertToDisplayElement();
                mostRecentParent = subsystem;
                Object value1 = null;
                if (Robot.getLiveWindow().containsKey(subsystem.getFieldName())) {
                    value1 = Robot.getTable().getValue(subsystem.getFieldName());
                    DataType type = DataType.getType(value1);
                    if (DisplayElementRegistry.supportsType(subsystem.getClass(), type))
                        this.liveWindowPanel.setField(subsystem.getFieldName(), subsystem, type, value1, subsystem.getSavedLocation());
                } else {
                    this.liveWindowPanel.setField(subsystem.getFieldName(), subsystem, subsystem.getType(), null, subsystem.getSavedLocation());
                }
                for (XMLWidget component : reader.getSubwidgetMap(subsys).values()) {
                    System.out.println("Adding subcomponent \"" + component.getField() + "\"");
                    AbstractTableWidget w = (AbstractTableWidget) component.convertToDisplayElement();
                    Object value2 = null;
                    value2 = Robot.getLiveWindow().getSubTable(mostRecentParent.getFieldName()).getSubTable(w.getFieldName());
                    DataType type = DataType.getType(value2);
                    mostRecentParent.addWidget(w);
                    w.setField(w.getFieldName(), w, type, value2, mostRecentParent, w.getSavedLocation());
                    mostRecentParent.setSize(mostRecentParent.getPreferredSize());
                }
            }
            DataType type;
            for (XMLWidget widget = (XMLWidget) reader.getHiddenFields().iterator(); ((Iterator<String>) widget).hasNext();) {
                String field = (String) ((Iterator<String>) widget).next();
                this.smartDashboardPanel.removeField(field);
            }
            String field;
            Map prefMap = this.prefs.getProperties();
            for (Map.Entry entry : reader.getProperties().entrySet()) {
                Property prop = (Property) prefMap.get(entry.getKey());
                prop.setValue(entry.getValue());
            }

            repaint();
        } catch (FileNotFoundException localFileNotFoundException) {
        }
    }

    public void exit() {
        int result = JOptionPane.showConfirmDialog(this, new String[] {
            "Do you wish to save this layout?"
        }, "Save before quitting?", 1, 2);

        switch (result) {
            case 0:
                save((String) this.prefs.saveFile.getValue());
            case 1:
                System.exit(0);
        }
    }

    public StuyDashboardPrefs getPrefs() {
        return this.prefs;
    }

    public LogToCSV getLogger() {
        return this.logger;
    }

    public static enum DisplayMode {
        SmartDashboard, LiveWindow;
    }
}
