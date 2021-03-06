package se.cambio.cds.gdl.editor.view.panels;

import se.cambio.cds.gdl.editor.util.GDLEditorImageUtil;
import se.cambio.cds.gdl.editor.util.GDLEditorLanguageManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public abstract class DraggablePanel extends JPanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private Point mLastPoint;
    private JLabel dragLabel = null;

    public DraggablePanel(JComponent component) {
        super(new BorderLayout());
        JPanel aux = new JPanel(new BorderLayout());
        aux.add(getDragLabel(), BorderLayout.NORTH);
        this.add(aux, BorderLayout.WEST);
        this.add(component, BorderLayout.CENTER);
        this.setBounds(50, 50, (int) component.getSize().getWidth(), (int) component.getSize().getHeight());
        getDragLabel().addMouseListener(this);
        getDragLabel().addMouseMotionListener(this);
    }

    private JLabel getDragLabel() {
        if (dragLabel == null) {
            dragLabel = new JLabel();
            dragLabel.setIcon(GDLEditorImageUtil.DRAG_ICON);
            dragLabel.setToolTipText(GDLEditorLanguageManager.getMessage("MoveLine"));
        }
        return dragLabel;
    }

    public void mouseDragged(MouseEvent event) {
        int valueX;
        int valueY;
        if (mLastPoint != null) {
            valueX = 0;
            valueY = super.getY() + (event.getY() - (int) mLastPoint.getY());
            super.setLocation(valueX, valueY);
        }
    }

    public void mouseMoved(MouseEvent event) {
        setCursorType(event.getPoint());
    }

    public void mouseClicked(MouseEvent event) {
    }

    public void mouseEntered(MouseEvent event) {
    }

    public void mouseExited(MouseEvent event) {
        if (mLastPoint == null) {
            super.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public void mousePressed(MouseEvent event) {
        if (super.getCursor().equals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))) {
            mLastPoint = event.getPoint();
            super.getParent().add(this, 0);
            super.getParent().repaint();
        } else {
            mLastPoint = null;
        }
    }

    public void mouseReleased(MouseEvent ev) {
        mLastPoint = null;
        if (super.getParent() instanceof DropPanel) {
            ((DropPanel) super.getParent()).panelDragged(this);
        }
    }

    private void setCursorType(Point point) {
        Point loc = super.getLocation();
        Dimension size = super.getSize();

        if ((point.y + 4 < loc.y + size.height) && (point.x + 4 < point.x + size.width)) {
            super.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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