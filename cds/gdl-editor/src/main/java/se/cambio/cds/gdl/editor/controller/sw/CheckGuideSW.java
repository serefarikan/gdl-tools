package se.cambio.cds.gdl.editor.controller.sw;

import lombok.extern.slf4j.Slf4j;
import se.cambio.cds.gdl.editor.controller.GDLEditor;
import se.cambio.cds.gdl.model.Guide;
import se.cambio.cds.util.CDSSwingWorker;
import se.cambio.openehr.util.exceptions.InternalErrorException;

import java.io.ByteArrayInputStream;

@Slf4j
public class CheckGuideSW extends CDSSwingWorker {
    private String errorMsg = null;
    private GDLEditor controller = null;
    private Guide guide = null;
    private String guideStr = null;
    private boolean checkOk = false;
    private Runnable pendingRunnable = null;

    public CheckGuideSW(
            GDLEditor controller, String guideStr, Runnable pendingRunnable) {
        this.controller = controller;
        this.guideStr = guideStr;
        this.pendingRunnable = pendingRunnable;
    }

    @Override
    protected void executeCDSSW() throws InternalErrorException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(guideStr.getBytes("UTF-8"));
            guide = controller.getGuidelineEditorManager().parseGuide(bais);
            if (guide != null) {
                controller.getGuideImporter().importGuide(guide, controller.getCurrentLanguageCode());
                controller.getGuideExportPluginDirectory().compile(guide);
                checkOk = true;
            }
        } catch (Exception ex) {
            log.error("Error executing guideline", ex);
            errorMsg = ex.getMessage();
        }
    }

    protected void done() {
        controller.gdlEditingChecked(guide, checkOk, errorMsg, pendingRunnable);
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