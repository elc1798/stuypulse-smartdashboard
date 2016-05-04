package edu.stuy.dashboard.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import edu.stuy.dashboard.workarounds.PropertyEditor;
import edu.wpi.first.smartdashboard.gui.DisplayElement;
import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.gui.Widget;
import edu.wpi.first.smartdashboard.types.DataType;
import edu.wpi.first.smartdashboard.types.DisplayElementRegistry;

@SuppressWarnings("serial")
public class StuyGlassPane extends JPanel {

    private static final int DRAG_BUFFER = 5;
    private boolean dragging;
    private Rectangle dragStartBounds;
    private Dimension dragMinSizeDelta;
    private Dimension dragMaxSizeDelta;
    private Point dragStartPoint;
    private int dragType = -1;
    private Map<Integer, Rectangle> areas = new HashMap<Integer, Rectangle>();
    private JPopupMenu elementMenu;
    private JMenuItem resizeMenu;
    private JMenu changeToMenu;
    private DisplayElement selectedElement;
    private DisplayElement menuElement;
    private boolean showGrid = false;
    private final StuyDashboardPanel panel;
    private final StuyDashboardFrame frame;
    private static final Color GRID_COLOR = new Color(0, 0, 0, 40);

    public StuyGlassPane(StuyDashboardFrame frame, StuyDashboardPanel panel) {
        this.frame = frame;
        this.panel = panel;
        this.elementMenu = new JPopupMenu();
        this.elementMenu.add(this.changeToMenu = new JMenu("Change to..."));
        this.elementMenu.add(new JMenuItem(new PropertiesItemAction("Properties...")));
        this.elementMenu.add(new JMenuItem(new MoveToBackAction("Send to Back")));
        this.elementMenu.add(this.resizeMenu = new JMenuItem(new ResetSizeAction()));
        this.elementMenu.add(new JMenuItem(new DeleteItemAction()));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                requestFocus();
                setShowingGrid(false);
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == 16)
                    setShowingGrid(true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 16)
                    setShowingGrid(false);
            }
        });
        setOpaque(false);
        setFocusable(true);

        GlassMouseListener listener = new GlassMouseListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    public void setShowingGrid(boolean showGrid) {
        if (this.showGrid != showGrid) {
            this.showGrid = showGrid;
            repaint();
        }
    }

    private DisplayElement findElementContaining(Point point) {
        return this.panel.findElementContaining(point);
    }

    private void prepareElementMenu(DisplayElement element) {
        this.menuElement = element;

        Dimension savedSize = this.menuElement.getSavedSize();
        this.resizeMenu.setEnabled((savedSize.width != -1) || (savedSize.height != -1));

        if ((element instanceof Widget)) {
            DataType type = ((Widget) element).getType();

            if (type == null) {
                this.changeToMenu.setEnabled(false);
            } else {
                this.changeToMenu.setEnabled(true);

                Set<Class<? extends Widget>> choices = DisplayElementRegistry.getWidgetsForType(type);

                this.changeToMenu.removeAll();
                int count = 0;
                for (Object obj : choices) {
                    Class c = (Class) obj;
                    if (!c.equals(element.getClass())) {
                        count++;
                        this.changeToMenu.add(new ChangeToAction(c));
                    }
                }
                if (count == 0)
                    this.changeToMenu.setEnabled(false);
            }
        } else {
            this.changeToMenu.setEnabled(false);
        }
    }

    private void showEditor(DisplayElement element) {
        PropertyEditor editor = this.frame.getPropertyEditor();
        editor.setPropertyHolder(element);
        editor.setVisible(true);
    }

    private void defineBounds() {
        Rectangle sb = this.selectedElement.getBounds();

        int ybuffer = Math.max(Math.min(sb.height / 5, 5), 1);
        int xbuffer = Math.max(Math.min(sb.width / 5, 5), 1);

        this.areas.clear();

        if (this.selectedElement.isResizable()) {
            Rectangle area = new Rectangle(sb.x - xbuffer, sb.y - ybuffer, 2 * xbuffer, 2 * ybuffer);
            this.areas.put(Integer.valueOf(8), area);

            area = new Rectangle(sb.x + xbuffer, sb.y - ybuffer, sb.width - 2 * xbuffer, 2 * ybuffer);
            this.areas.put(Integer.valueOf(1), area);

            area = new Rectangle(sb.x + sb.width - xbuffer, sb.y - ybuffer, 2 * xbuffer, 2 * ybuffer);
            this.areas.put(Integer.valueOf(2), area);

            area = new Rectangle(sb.x + sb.width - xbuffer, sb.y + ybuffer, 2 * xbuffer, sb.height - 2 * ybuffer);
            this.areas.put(Integer.valueOf(3), area);

            area = new Rectangle(sb.x + sb.width - xbuffer, sb.y + sb.height - ybuffer, 2 * xbuffer, 2 * ybuffer);
            this.areas.put(Integer.valueOf(4), area);

            area = new Rectangle(sb.x + xbuffer, sb.y + sb.height - ybuffer, sb.width - 2 * xbuffer, 2 * ybuffer);
            this.areas.put(Integer.valueOf(5), area);

            area = new Rectangle(sb.x - xbuffer, sb.y + sb.height - ybuffer, 2 * xbuffer, 2 * ybuffer);
            this.areas.put(Integer.valueOf(6), area);

            area = new Rectangle(sb.x - xbuffer, sb.y + ybuffer, 2 * xbuffer, sb.height - 2 * ybuffer);
            this.areas.put(Integer.valueOf(7), area);

            area = new Rectangle(sb.x + xbuffer, sb.y + ybuffer, sb.width - 2 * xbuffer, sb.height - 2 * ybuffer);
            this.areas.put(Integer.valueOf(0), area);
        } else {
            this.areas.put(Integer.valueOf(0), sb);
        }
    }

    protected void paintComponent(Graphics g) {
        Rectangle bounds = getBounds();

        if (this.selectedElement != null) {
            Rectangle eb = this.selectedElement.getBounds();

            g.setColor(Color.GRAY);
            g.drawRoundRect(eb.x - 1, eb.y - 1, eb.width + 1, eb.height + 1, 8, 8);
        }

        if (this.showGrid) {
            g.setColor(GRID_COLOR);

            StuyDashboardPrefs pref = this.frame.getPrefs();
            int[] w = pref.grid_widths.getValue();
            int[] h = pref.grid_heights.getValue();

            int cell = -1;
            for (int i = 0; i < bounds.width; i += w[(cell = (cell + 1) % w.length)]) {
                g.drawLine(i, 0, i, bounds.height);
            }

            cell = -1;
            for (int i = 0; i < bounds.height; i += h[(cell = (cell + 1) % h.length)])
                g.drawLine(0, i, bounds.width, i);
        }
    }

    private void setSelected(DisplayElement element) {
        if (this.selectedElement != element) {
            this.selectedElement = element;
            if (this.selectedElement == null)
                this.areas.clear();
            else {
                defineBounds();
            }
            repaint();
        }
    }

    private class DeleteItemAction extends AbstractAction {
        public DeleteItemAction() {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            if ((menuElement instanceof StaticWidget))
                panel.removeElement((StaticWidget) menuElement);
            else if ((menuElement instanceof Widget))
                panel.removeField(((Widget) menuElement).getFieldName());
        }
    }

    private class PropertiesItemAction extends AbstractAction {
        private PropertiesItemAction(String string) {
            super();
        }

        public void actionPerformed(ActionEvent ae) {
            showEditor(menuElement);
        }
    }

    private class ResetSizeAction extends AbstractAction {
        private ResetSizeAction() {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            menuElement.setSavedSize(new Dimension(-1, -1));
            panel.revalidateBacking();
        }
    }

    private class MoveToBackAction extends AbstractAction {
        private MoveToBackAction(String string) {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            panel.shiftToBack(menuElement);
        }
    }

    private class ChangeToAction extends AbstractAction {
        Class<? extends Widget> elementClass;

        private ChangeToAction(Class<? extends Widget> c) {
            super();
            // The line below was too golden to remove:
            // this.elementClass = elementClass;
            // wtf??? Assign elementClass to elementClass?!?!?
            elementClass = c;
        }

        @SuppressWarnings("deprecation")
        public void actionPerformed(ActionEvent e) {
            if ((menuElement instanceof Widget)) {
                Widget oldElement = (Widget) menuElement;

                if (panel.getTable().containsKey(oldElement.getFieldName())) {
                    Object value = panel.getTable().getValue(oldElement.getFieldName());
                    panel.setField(oldElement.getFieldName(), elementClass, value, oldElement.getLocation());
                } else {
                    panel.setField(oldElement.getFieldName(), elementClass, oldElement.getType(), null, oldElement.getLocation());
                }
            }
        }
    }

    private class GlassMouseListener extends MouseAdapter {
        private int lastDW;
        private int lastDH;
        private int lastDX;
        private int lastDY;

        private GlassMouseListener() {
        }

        private int adjust(int delta, int original, int[] cells) {
            if (showGrid) {
                int total = 0;
                for (int cell : cells) {
                    total += cell;
                }

                int n = (delta + original) % total;
                if (n < 0) {
                    n += total;
                }

                int i = 0;
                for (int cumulative = 0; i < cells.length; cumulative += cells[(i++)]) {
                    if (n < cumulative + cells[i] / 2) {
                        return delta - n + cumulative;
                    }
                }

                return delta - n + total;
            }
            return delta;
        }

        private int adjustX(int value) {
            return adjust(value, dragStartBounds.x, frame.getPrefs().grid_widths.getValue());
        }

        private int adjustY(int value) {
            return adjust(value, dragStartBounds.y, frame.getPrefs().grid_heights.getValue());
        }

        private int adjustW(int value) {
            return adjust(value, dragStartBounds.x + dragStartBounds.width,
                frame.getPrefs().grid_widths.getValue());
        }

        private int adjustH(int value) {
            return adjust(value, dragStartBounds.y + dragStartBounds.height,
                frame.getPrefs().grid_heights.getValue());
        }

        private int inRange(boolean horizontal, int value) {
            int min = horizontal ? dragMinSizeDelta.width : dragMinSizeDelta.height;
            int max = horizontal ? dragMaxSizeDelta.width : dragMaxSizeDelta.height;
            return value <= max ? value : value < min ? min : max;
        }

        public void mousePressed(MouseEvent e) {
            dragType = -1;

            if (selectedElement != null)
                if (e.isPopupTrigger()) {
                prepareElementMenu(selectedElement);
                elementMenu.show(StuyGlassPane.this, e.getPoint().x, e.getPoint().y);
            } else {
                for (Map.Entry entry : areas.entrySet())
                    if (((Rectangle) entry.getValue()).contains(e.getPoint())) {
                        dragType = ((Integer) entry.getKey()).intValue();
                        break;
                    }
            }
        }

        public void mouseDragged(MouseEvent e) {
            if (e.isMetaDown()) {
                return;
            }

            if ((selectedElement != null) && (dragType != -1)) {
                if (!dragging) {
                    dragging = true;

                    dragStartBounds = selectedElement.getBounds();
                    dragMinSizeDelta = selectedElement.getMinimumSize();
                    dragMinSizeDelta.width -= dragStartBounds.width;
                    dragMinSizeDelta.height -= dragStartBounds.height;
                    dragMaxSizeDelta = selectedElement.getMaximumSize();
                    dragMaxSizeDelta.width -= dragStartBounds.width;
                    dragMaxSizeDelta.height -= dragStartBounds.height;
                    dragStartPoint = e.getPoint();

                    this.lastDH = (this.lastDW = this.lastDX = this.lastDY = 0);
                }
                int dh;
                int dw;
                int dy;
                int dx = dy = dw = dh = 0;

                switch (dragType) {
                    case 1:
                        dh = inRange(false, -adjustY(e.getPoint().y - dragStartPoint.y));
                        dy = -dh;
                        break;
                    case 2:
                        dh = inRange(false, -adjustY(e.getPoint().y - dragStartPoint.y));
                        dy = -dh;
                        dw = inRange(true, adjustW(e.getPoint().x - dragStartPoint.x));
                        break;
                    case 3:
                        dw = inRange(true, adjustW(e.getPoint().x - dragStartPoint.x));
                        break;
                    case 4:
                        dw = inRange(true, adjustW(e.getPoint().x - dragStartPoint.x));
                        dh = inRange(false, adjustH(e.getPoint().y - dragStartPoint.y));
                        break;
                    case 5:
                        dh = inRange(false, adjustH(e.getPoint().y - dragStartPoint.y));
                        break;
                    case 6:
                        dh = inRange(false, adjustH(e.getPoint().y - dragStartPoint.y));
                        dw = inRange(true, -adjustX(e.getPoint().x - dragStartPoint.x));
                        dx = -dw;
                        break;
                    case 7:
                        dw = inRange(true, -adjustX(e.getPoint().x - dragStartPoint.x));
                        dx = -dw;
                        break;
                    case 8:
                        dh = inRange(false, -adjustY(e.getPoint().y - dragStartPoint.y));
                        dy = -dh;
                        dw = inRange(true, -adjustX(e.getPoint().x - dragStartPoint.x));
                        dx = -dw;
                        break;
                    case 0:
                        StuyDashboardPrefs prefs = frame.getPrefs();
                        int leading = adjust(e.getPoint().x - dragStartPoint.x, dragStartBounds.x,
                            prefs.grid_widths.getValue());
                        int trailing = adjust(e.getPoint().x - dragStartPoint.x,
                            dragStartBounds.x + dragStartBounds.width, prefs.grid_widths.getValue());
                        dx = Math.abs(leading) < Math.abs(trailing) ? leading : trailing;
                        leading = adjust(e.getPoint().y - dragStartPoint.y, dragStartBounds.y,
                            prefs.grid_heights.getValue());
                        trailing = adjust(e.getPoint().y - dragStartPoint.y,
                            dragStartBounds.y + dragStartBounds.height, prefs.grid_heights.getValue());
                        dy = Math.abs(leading) < Math.abs(trailing) ? leading : trailing;
                        break;
                    default:
                        System.err.println("Dragging resize failed");
                        break;
                }
                boolean changed = false;

                if ((dw != this.lastDW) || (dh != this.lastDH)) {
                    changed = true;
                    Dimension size = selectedElement.getSavedSize();
                    if (dw != this.lastDW) {
                        size.width = (dragStartBounds.width + dw);
                        this.lastDW = dw;
                    }
                    if (dh != this.lastDH) {
                        size.height = (dragStartBounds.height + dh);
                        this.lastDH = dh;
                    }
                    selectedElement.setSavedSize(size);
                }
                if ((dx != this.lastDX) || (dy != this.lastDY)) {
                    changed = true;
                    Point origin = dragStartBounds.getLocation();
                    origin.translate(dx, dy);
                    selectedElement.setSavedLocation(origin);
                    this.lastDX = dx;
                    this.lastDY = dy;
                }

                if (changed) {
                    panel.revalidateBacking();
                    frame.repaint();
                }
            }
        }

        public void mouseExited(MouseEvent e) {
            dragType = -1;
            dragging = false;
            setSelected(null);
        }

        public void mouseReleased(MouseEvent e) {
            if (dragging == true) {
                dragging = false;
                defineBounds();
                mouseMoved(e);
            } else if (selectedElement != null) {
                if (e.isPopupTrigger()) {
                    prepareElementMenu(selectedElement);
                    elementMenu.show(StuyGlassPane.this, e.getPoint().x, e.getPoint().y);
                } else if (e.getClickCount() == 2) {
                    showEditor(selectedElement);
                }
            }
        }

        public void mouseMoved(MouseEvent e) {
            DisplayElement element = findElementContaining(e.getPoint());
            boolean found;
            if (element != selectedElement) {
                if (element == null) {
                    found = false;
                    for (Rectangle area : areas.values()) {
                        if (area.contains(e.getPoint())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        setSelected(null);
                } else {
                    setSelected(element);
                }
            }

            if (!areas.isEmpty())
                for (Map.Entry entry : areas.entrySet()) {
                    Rectangle area = (Rectangle) entry.getValue();
                    if (area.contains(e.getPoint()))
                        switch (((Integer) entry.getKey()).intValue()) {
                        case 1:
                            setCursor(Cursor.getPredefinedCursor(8));
                            break;
                        case 2:
                            setCursor(Cursor.getPredefinedCursor(7));
                            break;
                        case 3:
                            setCursor(Cursor.getPredefinedCursor(11));
                            break;
                        case 4:
                            setCursor(Cursor.getPredefinedCursor(5));
                            break;
                        case 5:
                            setCursor(Cursor.getPredefinedCursor(9));
                            break;
                        case 6:
                            setCursor(Cursor.getPredefinedCursor(4));
                            break;
                        case 7:
                            setCursor(Cursor.getPredefinedCursor(10));
                            break;
                        case 8:
                            setCursor(Cursor.getPredefinedCursor(6));
                            break;
                        case 0:
                            setCursor(Cursor.getDefaultCursor());
                            break;
                        default:
                            System.err.println("?????");
                            break;
                    }
                }
            else
                setCursor(Cursor.getDefaultCursor());
        }
    }

}
