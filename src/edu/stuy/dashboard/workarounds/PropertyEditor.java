package edu.stuy.dashboard.workarounds;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import edu.wpi.first.smartdashboard.gui.Widget;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.PropertyHolder;

@SuppressWarnings("serial")
public class PropertyEditor extends JDialog {

    private JTable table;
    private PropTableModel tableModel;
    private Map<String, Property> values;
    private String[] names;

    public PropertyEditor(JFrame frame) {
        super(frame, true);
        this.tableModel = new PropTableModel();
        this.table = new PropertiesTable(this.tableModel);
        this.table.setGridColor(Color.LIGHT_GRAY);
        this.table.setRowSelectionAllowed(false);
        JScrollPane scrollPane = new JScrollPane(this.table);
        setBounds(100, 100, 300, 400);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, "Center");
    }

    // This method was not set public
    public void setPropertyHolder(PropertyHolder data) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        if ((data instanceof Widget)) {
            System.out.println("FIELD NAME: " + ((Widget) data).getFieldName());
            setTitle(((Widget) data).getFieldName());
        } else {
            setTitle("Edit Properties");
        }
        values = data.getProperties();
        names = ((String[]) values.keySet().toArray(new String[values.size()]));
        System.out.println(Arrays.toString(names));
        tableModel.fireTableDataChanged();
        System.out.println("Table reloaded");
    }

    private class PropTableModel extends AbstractTableModel {

        public PropTableModel() {
        }

        public int getRowCount() {
            return PropertyEditor.this.values.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int i) {
            if (i == 0) {
                return "Property";
            } else if (i == 1) {
                return "Value";
            } else {
                return "Error";
            }
        }

        public boolean isCellEditable(int row, int col) {
            return col == 1;
        }

        public Object getValueAt(int row, int col) {
            System.out.printf("Getting %d, %d\n", row, col);
            switch (col) {
                case 0:
                    return names[row];
                case 1:
                    return ((Property) PropertyEditor.this.values.get(names[row])).getTableValue();
            }
            return "Bad row, col";
        }

        public void setValueAt(Object value, int row, int col) {
            assert (col == 1);
            ((Property) PropertyEditor.this.values.get(names[row])).setValue(value);
        }
    }

    private class PropertiesTable extends JTable {

        private AbstractTableModel model;

        PropertiesTable(AbstractTableModel model) {
            super();
            this.model = model;
        }

        public TableCellEditor getCellEditor(int row, int col) {
            TableCellEditor editor = ((Property) values.get(names[row])).getEditor(this);
            System.out.println("Called getCellEditor");
            return editor == null ? super.getCellEditor(row, col) : editor;
        }

        public TableCellRenderer getCellRenderer(int row, int col) {
            System.out.println("Called getCellEditor");
            if (col == 0) {
                return super.getCellRenderer(row, col);
            }
            TableCellRenderer renderer = ((Property) values.get(names[row])).getRenderer();
            return renderer == null ? super.getCellRenderer(row, col) : renderer;
        }
    }

}
