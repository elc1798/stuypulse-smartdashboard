package edu.stuy.dashboard.workarounds;

import java.awt.BorderLayout;
import java.awt.Color;
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
        if (this.table.isEditing()) {
            this.table.getCellEditor().stopCellEditing();
        }
        if ((data instanceof Widget))
            setTitle(((Widget) data).getFieldName());
        else {
            setTitle("Edit Properties");
        }
        this.values = data.getProperties();
        this.names = ((String[]) this.values.keySet().toArray(new String[this.values.size()]));
        this.tableModel.fireTableDataChanged();
    }

    class PropTableModel extends AbstractTableModel {
        PropTableModel() {
        }

        public int getRowCount() {
            return PropertyEditor.this.values.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int i) {
            if (i == 0)
                return "Property";
            if (i == 1) {
                return "Value";
            }
            return "Error";
        }

        public boolean isCellEditable(int row, int col) {
            boolean editable = col == 1;
            return editable;
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return PropertyEditor.this.names[row];
                case 1:
                    return ((Property) PropertyEditor.this.values.get(PropertyEditor.this.names[row])).getTableValue();
            }
            return "Bad row, col";
        }

        public void setValueAt(Object value, int row, int col) {
            assert (col == 1);
            ((Property) PropertyEditor.this.values.get(PropertyEditor.this.names[row])).setValue(value);
        }
    }

    class PropertiesTable extends JTable {
        AbstractTableModel model;

        PropertiesTable(AbstractTableModel model) {
            super();
            this.model = model;
        }

        public TableCellEditor getCellEditor(int row, int col) {
            TableCellEditor editor = ((Property) PropertyEditor.this.values.get(PropertyEditor.this.names[row])).getEditor(PropertyEditor.this);
            return editor == null ? super.getCellEditor(row, col) : editor;
        }

        public TableCellRenderer getCellRenderer(int row, int col) {
            if (col == 0) {
                return super.getCellRenderer(row, col);
            }
            TableCellRenderer renderer = ((Property) PropertyEditor.this.values.get(PropertyEditor.this.names[row])).getRenderer();
            return renderer == null ? super.getCellRenderer(row, col) : renderer;
        }
    }

}
