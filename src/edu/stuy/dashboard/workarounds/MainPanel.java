package edu.stuy.dashboard.workarounds;

import java.awt.LayoutManager;
import java.util.HashMap;

import javax.swing.JPanel;

import edu.stuy.dashboard.gui.StuyDashboardPanel;
import edu.stuy.dashboard.utils.Color;

@SuppressWarnings("serial")
public final class MainPanel extends JPanel {

    public static final HashMap<String, StuyDashboardPanel> panels = new HashMap<String, StuyDashboardPanel>();
    private static StuyDashboardPanel currentPanel;

    public MainPanel(LayoutManager layout, StuyDashboardPanel defaultPanel, StuyDashboardPanel[] panels) {
        super(layout);
        for (StuyDashboardPanel panel : panels)
            MainPanel.panels.put(panel.getName(), panel);
        currentPanel = defaultPanel;
    }

    public static StuyDashboardPanel getCurrentPanel() {
        return currentPanel;
    }

    public static StuyDashboardPanel getPanel(String name) {
        return (StuyDashboardPanel) panels.get(name);
    }

    public void setCurrentPanel(StuyDashboardPanel panel) {
        if (panels.containsValue(panel))
            currentPanel = panel;
        else
            throw new IllegalArgumentException("Not a valid panel");
    }

    public void addPanel(String name, StuyDashboardPanel panel) {
        if (!panels.containsValue(panel))
            panels.put(name, panel);
        else
            throw new IllegalArgumentException("That panel already exists");
    }

    public void setColorScheme(Color c) {
        super.setBackground(c);
        super.setForeground(c.inverted());
    }
}
