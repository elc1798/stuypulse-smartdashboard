package edu.stuy.dashboard.gui;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import edu.wpi.first.smartdashboard.main;
import edu.wpi.first.smartdashboard.properties.BooleanProperty;
import edu.wpi.first.smartdashboard.properties.FileProperty;
import edu.wpi.first.smartdashboard.properties.IntegerListProperty;
import edu.wpi.first.smartdashboard.properties.IntegerProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.PropertyHolder;
import edu.wpi.first.smartdashboard.robot.Robot;

public class StuyDashboardPrefs implements PropertyHolder {

    private static final File USER_HOME = new File(System.getProperty("user.home"));
    private static final File USER_SMARTDASHBOARD_HOME = new File(USER_HOME, "SmartDashboard");

    private Map<String, Property> properties = new LinkedHashMap<String, Property>();
    public final IntegerProperty team = new IntegerProperty(this, "Team Number", 0);
    public final BooleanProperty usemDNS = new BooleanProperty(this, "Use mDNS (supported on roboRIO)", true);
    public final BooleanProperty hideMenu = new BooleanProperty(this, "Hide Menu", false);
    public final BooleanProperty autoShowWidgets = new BooleanProperty(this, "Automatically Show Widgets", true);
    public final IntegerListProperty grid_widths = new IntegerListProperty(this, "Grid Cell Width(s)", new int[] {
        16
    });
    public final IntegerListProperty grid_heights = new IntegerListProperty(this, "Grid Cell Height(s)", new int[] {
        16
    });
    public final IntegerProperty x = new IntegerProperty(this, "Window X Position", 0);
    public final IntegerProperty y = new IntegerProperty(this, "Window Y Position", 0);
    public final IntegerProperty width = new IntegerProperty(this, "Window Width", 640);
    public final IntegerProperty height = new IntegerProperty(this, "Window Height", 480);
    public final FileProperty saveFile = new FileProperty(this, "Save File", new File(USER_SMARTDASHBOARD_HOME, "save.xml").getAbsolutePath());
    public final BooleanProperty logToCSV = new BooleanProperty(this, "Log to CSV", false);
    public final FileProperty csvFile = new FileProperty(this, "CSV File", new File(USER_SMARTDASHBOARD_HOME, "csv.txt").getAbsolutePath());
    private Preferences node;
    private final StuyDashboardFrame frame;

    public static StuyDashboardPrefs getInstance() {
        return StuyDashboardFrame.getInstance().getPrefs();
    }

    public StuyDashboardPrefs(StuyDashboardFrame frame) {
        this.frame = frame;
        this.node = Preferences.userNodeForPackage(main.class);

        for (Property property : this.properties.values())
            if (property != this.logToCSV) {
                load(property);
            }
    }

    private void load(Property property) {
        property.setSaveValue(this.node.get(property.getName(), property.getSaveValue()));
    }

    public Map<String, Property> getProperties() {
        return this.properties;
    }

    public boolean validatePropertyChange(Property property, Object value) {
        if ((property == this.team) || (property == this.width) || (property == this.height))
            return ((Integer) value).intValue() > 0;
        if ((property == this.grid_widths) || (property == this.grid_heights)) {
            int[] values = (int[]) value;

            if (values.length == 0) {
                return false;
            }
            for (int i : values) {
                if (i <= 0) {
                    return false;
                }
            }
            return true;
        }
        if ((property == this.logToCSV) &&
            (((Boolean) value).booleanValue())) {
            int result = JOptionPane.showOptionDialog(null,
                "Should SmartDashboard start logging to the CSV file? (This will override the existing file)", "Warning", 0, 2, null, null,
                Boolean.valueOf(false));

            return result == 0;
        }

        return true;
    }

    public void propertyChanged(Property property) {
        this.node.put(property.getName(), property.getSaveValue());

        if (property == this.x) {
            this.frame.setLocation(this.x.getValue().intValue(), this.frame.getY());
        } else if (property == this.y) {
            this.frame.setLocation(this.frame.getX(), this.y.getValue().intValue());
        } else if (property == this.width) {
            this.frame.setSize(this.width.getValue().intValue(), this.frame.getHeight());
        } else if (property == this.height) {
            this.frame.setSize(this.frame.getWidth(), this.height.getValue().intValue());
        } else if (property == this.team) {
            Robot.setTeam(this.team.getValue().intValue());
            this.frame.setTitle("SmartDashboard - " + this.team.getValue());
        } else if (property == this.usemDNS) {
            Robot.setUseMDNS(((Boolean) this.usemDNS.getValue()).booleanValue());
        } else if (property == this.hideMenu) {
            this.frame.setShouldHideMenu(((Boolean) this.hideMenu.getValue()).booleanValue());
        } else if (property == this.logToCSV) {
            if (((Boolean) this.logToCSV.getValue()).booleanValue())
                this.frame.getLogger().start((String) this.csvFile.getValue());
            else
                this.frame.getLogger().stop();
        }
    }

    static {
        if (!USER_SMARTDASHBOARD_HOME.exists())
            USER_SMARTDASHBOARD_HOME.mkdirs();
    }
}
