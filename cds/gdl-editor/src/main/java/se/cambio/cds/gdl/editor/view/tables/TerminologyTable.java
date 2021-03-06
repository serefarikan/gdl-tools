package se.cambio.cds.gdl.editor.view.tables;

import org.apache.commons.jxpath.JXPathContext;
import se.cambio.cds.gdl.editor.util.GDLEditorLanguageManager;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;

public class TerminologyTable extends JTable {

    private static final long serialVersionUID = 1L;
    private JXPathContext context = null;

    public TerminologyTable(JXPathContext context) {
        this.context = context;
        this.setModel(new TerminologyTableModel());
        Vector<String> columnIdentifiers = new Vector<>();
        columnIdentifiers.add(GDLEditorLanguageManager.getMessage("Code"));
        columnIdentifiers.add(GDLEditorLanguageManager.getMessage("Text"));
        columnIdentifiers.add(GDLEditorLanguageManager.getMessage("Description"));
        getTerminologyTableModel().setColumnIdentifiers(columnIdentifiers);
        this.getColumnModel().getColumn(0).setMaxWidth(80);
        final TerminologyTableCellEditor cellEditor = new TerminologyTableCellEditor();
        this.getColumnModel().getColumn(1).setCellEditor(cellEditor);
        this.getColumnModel().getColumn(2).setCellEditor(cellEditor);
        this.putClientProperty("terminateEditOnFocusLost", true);
    }

    public TerminologyTableModel getTerminologyTableModel() {
        return (TerminologyTableModel) getModel();
    }


    public boolean isCellEditable(int row, int column) {
        return column != 0;
    }

    public static class TerminologyTableModel extends DefaultTableModel {
        private static final long serialVersionUID = 1L;
    }

    private void updateResults(TerminologyTableCellEditor otce) {
        String value = (String) otce.getCellEditorValue();
        int row = otce.getRow();
        int column = otce.getColumn();
        String gtCode = (String) getTerminologyTableModel().getValueAt(row, 0);
        String attribute = column == 1 ? "text" : "description";
        context.setValue(gtCode + "/" + attribute, value);
    }

    private class TerminologyTableCellEditor extends DefaultCellEditor {
        private int row;
        private int column;

        TerminologyTableCellEditor() {
            super(new JTextField());
            this.addCellEditorListener(new CellEditorListener() {
                public void editingStopped(ChangeEvent ev) {
                    Object obj = ev.getSource();
                    if (obj instanceof TerminologyTableCellEditor) {
                        updateResults((TerminologyTableCellEditor) obj);
                    }
                }

                public void editingCanceled(ChangeEvent ev) {
                }
            });
        }

        public int getRow() {
            return row;
        }

        int getColumn() {
            return column;
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected,
                                                     int row, int column) {
            this.row = row;
            this.column = column;
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }
}
/*
 *  ***** BEGIN LICENSE BLOCK *****
 *  Version: MPL 2.0/GPL 2.0/LGPL 2.1
 *
 *  The contents of this file are subject to the Mozilla Public License Version
 *  2.0 (the 'License'); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an 'AS IS' basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *
 *  The Initial Developers of the Original Code are Iago Corbal and Rong Chen.
 *  Portions created by the Initial Developer are Copyright (C) 2012-2013
 *  the Initial Developer. All Rights Reserved.
 *
 *  Contributor(s):
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 *  ***** END LICENSE BLOCK *****
 */