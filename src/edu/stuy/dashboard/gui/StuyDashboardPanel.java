package edu.stuy.dashboard.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import edu.wpi.first.smartdashboard.gui.DashboardPanel;
import edu.wpi.first.smartdashboard.gui.DisplayElement;
import edu.wpi.first.smartdashboard.gui.GlassPane;
import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.gui.Widget;
import edu.wpi.first.smartdashboard.livewindow.elements.LWSubsystem;
import edu.wpi.first.smartdashboard.types.DataType;
import edu.wpi.first.smartdashboard.types.DisplayElementRegistry;
import edu.wpi.first.smartdashboard.types.NamedDataType;
import edu.wpi.first.wpilibj.tables.ITable;
import edu.wpi.first.wpilibj.tables.ITableListener;

@SuppressWarnings("serial")
public class StuyDashboardPanel extends JPanel {

    private StuyGlassPane glassPane;
    private JPanel backPane = new JPanel();

    private LinkedList<DisplayElement> elements = new LinkedList<DisplayElement>();

    private Map<String, Widget> fields = new HashMap<String, Widget>();

    private Set<String> hiddenFields = new HashSet<String>();

    private boolean editable = false;

    private final RobotListener listener = new RobotListener();
    private final ArrayList<LWSubsystem> subsystems = new ArrayList<LWSubsystem>();
    private final StuyDashboardFrame frame;
    private final ITable table;
    private static final Random random = new Random();

    public StuyDashboardPanel(StuyDashboardFrame frame, ITable table) {
        this.frame = frame;
        this.table = table;
        this.glassPane = new StuyGlassPane(frame, this);
        add(this.glassPane);
        add(this.backPane);

        this.backPane.setLayout(new DashboardLayout());
        this.backPane.setFocusable(true);

        setLayout(new DashboardLayout());

        setEditable(this.editable);

        table.addTableListenerEx(this.listener, 23);
        table.addSubTableListener(this.listener, true);
    }

    public ITable getTable() {
        return table;
    }

    public synchronized void addMouseListener(MouseListener l) {
        glassPane.addMouseListener(l);
        backPane.addMouseListener(l);
    }

    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        glassPane.addMouseMotionListener(l);
        backPane.addMouseMotionListener(l);
    }

    public void revalidateBacking() {
        backPane.revalidate();
    }

    public void setEditable(boolean editable) {
        this.editable = editable;

        this.glassPane.setVisible(editable);
        if (editable) {
            this.glassPane.requestFocus();
        }
    }

    public boolean isEditable() {
        return this.editable;
    }

    public Iterable<String> getHiddenFields() {
        return this.hiddenFields;
    }

    public Iterable<DisplayElement> getElements() {
        return this.elements;
    }

    private Object verifyValue(DataType type, Object value) {
        if ((type == null) || (value == null))
            return null;
        if ((type instanceof NamedDataType)) {
            return value;
        }
        return value;
    }

    public void clear() {
        hiddenFields.clear();
        fields.clear();
        for (DisplayElement element : elements) {
            disconnect(element);
            backPane.remove(element);
        }
        elements.clear();

        table.removeTableListener(listener);
        table.addTableListenerEx(listener, 23);
        table.addSubTableListener(listener, true);

        repaint();
    }

    public void removeUnusedFields() {
        ArrayList<String> unused = new ArrayList<String>();
        for (String field : fields.keySet()) {
            if ((!table.containsKey(field)) && (!table.containsSubTable(field))) {
                unused.add(field);
            }
        }
        for (String field : unused) {
            removeField(field);
            hiddenFields.remove(field);
        }
    }

    public void removeField(String field) {
        Widget elem = (Widget) fields.get(field);
        hiddenFields.add(field);
        if (elem != null) {
            disconnect(elem);
            backPane.remove(elem);
            fields.remove(field);
            elements.remove(elem);
            repaint(elem.getBounds());
        }
    }

    public void removeElement(StaticWidget widget) {
        disconnect(widget);
        backPane.remove(widget);
        elements.remove(widget);
        repaint(widget.getBounds());
    }

    public void shiftToBack(DisplayElement element) {
        int count = 0;

        elements.remove(element);

        for (DisplayElement e : elements) {
            backPane.setComponentZOrder(e, count++);
        }
        backPane.setComponentZOrder(element, count);

        elements.add(element);

        repaint();
    }

    public void addElement(DisplayElement element, Point point) {
        element.init();
        Dimension preferred;
        if (point == null) {
            Dimension saved = element.getSavedSize();
            preferred = element.getPreferredSize();
            if (saved.width > 0) {
                preferred.width = saved.width;
            }
            if (saved.height > 0) {
                preferred.height = saved.height;
            }
            element.setSize(preferred);
            point = findSpace(element);
            element.setBounds(new Rectangle(point, preferred));
        }
        element.setSavedLocation(point);

        backPane.add(element);

        int count = 1;
        for (DisplayElement e : this.elements) {
            backPane.setComponentZOrder(e, count++);
        }
        backPane.setComponentZOrder(element, 0);

        elements.addFirst(element);

        revalidate();
        repaint();
    }

    public void setField(String key, Widget element, DataType type, Object value, Point point) {
        removeField(key);

        hiddenFields.remove(key);

        value = verifyValue(type, value);

        element.setFieldName(key);
        if (type != null) {
            element.setType(type);
        }

        fields.put(key, element);

        addElement(element, point);

        if (value != null)
            element.setValue(value);
    }

    public void addField(String key) {
        setField(key, null, table.containsKey(key) ? table.getValue(key) : null, null);
    }

    public void setField(String key, Class<? extends Widget> preferred, Object value, Point point) {
        setField(key, preferred, DataType.getType(value), value, point);
    }

    public void setField(String key, Class<? extends Widget> preferred, DataType type, Object value, Point point) {
        Widget element = (Widget) fields.get(key);

        if (type == null) {
            System.out.println("WARNING: has no way of handling data at field \"" + key + "\"");
            removeField(key);
        } else if ((element != null) && (preferred == null) && ((element.getType() == type) || (element.supportsType(type)))) {
            if (element.getType() != type) {
                element.setType(type);
            }

            value = verifyValue(type, value);
            if (value != null)
                element.setValue(value);
        } else {
            Class clazz = preferred == null ? type.getDefault() : preferred;

            if (clazz == null) {
                Set candidates = DisplayElementRegistry.getWidgetsForType(type);

                if (candidates.isEmpty()) {
                    System.out.println("WARNING: has no way of handling type " + type);
                    return;
                }
                clazz = (Class) candidates.toArray()[0];
            }

            try {
                element = (Widget) clazz.newInstance();

                setField(key, element, type, value, point);
            } catch (InstantiationException ex) {
                System.out.println("ERROR: " + clazz.getName() + " has no default constructor!");
            } catch (IllegalAccessException ex) {
                System.out.println("ERROR: " + clazz.getName() + " has no public default constructor!");
            }
        }
    }

    public DisplayElement findElementContaining(Point point) {
        for (DisplayElement element : this.elements) {
            if (element.getBounds().contains(point)) {
                return element;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Point findSpace(DisplayElement toPlace) {
        Stack<Point> positions = new Stack<Point>();
        positions.add(new Point(0, 0));

        Dimension size = toPlace.getSize();
        Dimension panelBounds = getSize();

        while (!positions.isEmpty()) {
            Point position = (Point) positions.pop();
            Rectangle area = new Rectangle(position, size);

            System.out.println("Adding an element at [" + position.x + "," + position.y + "]");

            if ((area.x >= 0) && (area.y >= 0) && (area.x + area.width <= panelBounds.width) && (area.y + area.height <= panelBounds.height)) {
                Iterator localIterator = elements.iterator();
                while (localIterator.hasNext()) {
                    DisplayElement element = (DisplayElement) localIterator.next();
                    if ((element != toPlace) && (element.isObstruction())) {
                        Rectangle bounds = element.getBounds();

                        if ((bounds.x <= area.x + area.width) && (bounds.x + bounds.width >= area.x) && (bounds.y <= area.y + area.height)
                            && (bounds.y + bounds.height >= area.y)) {
                            Point right = new Point(bounds.x + bounds.width + 1, position.y);
                            if (positions.isEmpty()) {
                                positions.add(right);
                                right = null;
                            }
                            positions.add(new Point(position.x, bounds.y + bounds.height + 1));
                            if ((right == null) || (Math.abs(right.x - area.x) >= area.width / 3)) break;
                            positions.add(right);
                            break;
                        }
                    }
                }
                return position;
            }
        }

        return new Point(random.nextInt(32), random.nextInt(32));
    }

    private void disconnect(DisplayElement element) {
        try {
            element.disconnect();
        } catch (Exception e) {
            String message = "An exception occurred while removing the " +
                DisplayElement.getName(element
                    .getClass())
                + " of type " + e
                    .getClass()
                + ".\nThe message is:\n" + e
                    .getMessage()
                + "\nThe stack trace is:\n";

            for (StackTraceElement trace : e.getStackTrace()) {
                message = message + trace.toString() + "\n";
            }
            JOptionPane.showMessageDialog(frame, message, "Exception When Removing Element", 0);
        }
    }

    public ArrayList<LWSubsystem> getSubsystems() {
        return this.subsystems;
    }

    public void addSubsystem(LWSubsystem subsystem) {
        this.subsystems.add(subsystem);
    }

    private class DashboardLayout implements LayoutManager {

        private DashboardLayout() {
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(640, 480);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0, 0);
        }

        public void layoutContainer(Container parent) {
            Dimension size;
            if (parent == StuyDashboardPanel.this) {
                size = getSize();
                glassPane.setBounds(0, 0, size.width, size.height);
                backPane.setBounds(0, 0, size.width, size.height);
            } else {
                for (DisplayElement element : elements) {
                    element.setLocation(element.getSavedLocation());

                    Dimension savedSize = element.getSavedSize();
                    Dimension preferredSize = element.getPreferredSize();

                    size = new Dimension(preferredSize);
                    if ((savedSize != null) && (savedSize.width != -1)) {
                        size.width = savedSize.width;
                    }
                    if ((savedSize != null) && (savedSize.height != -1)) {
                        size.height = savedSize.height;
                    }

                    element.setSize(size);
                }
            }
        }
    }

    private class RobotListener implements ITableListener {

        private RobotListener() {
        }

        public void valueChanged(ITable source, final String key, final Object value, boolean isNew) {
            if ((isNew) && (!((Boolean) frame.getPrefs().autoShowWidgets.getValue()).booleanValue()) && (!fields.containsKey(key))) {
                hiddenFields.add(key);
            } else if (!hiddenFields.contains(key))
                if ((value instanceof ITable)) {
                final ITable table = (ITable) value;
                table.addTableListenerEx("~TYPE~", new ITableListener() {
                    public void valueChanged(ITable typeSource, String typeKey, Object typeValue, boolean typeIsNew) {
                        table.removeTableListener(this);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                setField(key, null, this, null);
                            }
                        });
                    }
                }, 23);
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setField(key, null, value, null);
                    }
                });
            }
        }
    }

}
