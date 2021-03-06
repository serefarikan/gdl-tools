package se.cambio.cm.util;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.openehr.rm.datatypes.text.DvCodedText;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TerminologyNodeVO implements Serializable {

    private final DvCodedText value;
    private final List<TerminologyNodeVO> children;

    private TerminologyNodeVO(DvCodedText value, List<TerminologyNodeVO> children) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.value = value;
        if (children != null) {
            this.children = new ArrayList<>(children);
        } else {
            this.children = new ArrayList<>();
        }
    }

    public TerminologyNodeVO(DvCodedText value) {
        this(value, null);
    }

    public DvCodedText getValue() {
        return value;
    }

    public void addChild(TerminologyNodeVO child) {
        children.add(child);
    }

    public List<TerminologyNodeVO> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof TerminologyNodeVO)) {
            return false;
        } else if (!super.equals(obj)) {
            return false;
        }

        final TerminologyNodeVO node = (TerminologyNodeVO) obj;

        return new EqualsBuilder().append(value, node.value)
                .append(children, node.children)
                .isEquals();
    }

    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + children.hashCode();
        return result;
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