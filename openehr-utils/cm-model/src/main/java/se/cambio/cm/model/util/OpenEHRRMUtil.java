package se.cambio.cm.model.util;

import se.cambio.cm.model.archetype.vo.ArchetypeElementVO;
import se.cambio.cm.model.archetype.vo.ArchetypeElementVOBuilder;
import se.cambio.cm.model.archetype.vo.ClusterVO;
import se.cambio.openehr.util.OpenEHRConst;
import se.cambio.openehr.util.OpenEHRDataValues;
import se.cambio.openehr.util.OpenEHRLanguageManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class OpenEHRRMUtil {
    public static String EVENT_TIME_PATH = "/data/events/time";
    public static String EXPIRY_TIME_PATH = "/expiry_time";
    public static String NARRATIVE_PATH =  "/narrative";
    public static String TIME_PATH = "/time";
    public static String TIMING_PATH = "/activities/timing";
    public static String ISM_TRANSITION_PATH = "/ism_transition/current_state";
    public static String TEMPLATE_ID_PATH = "/archetype_details/template_id";
    public static String ARCHETYPE_DETAILS_PATH = "/archetype_details";

    private static Collection<String> rmPaths;
    static {
        rmPaths = new ArrayList<String>();
        rmPaths.add(EVENT_TIME_PATH);
        rmPaths.add(EXPIRY_TIME_PATH);
        rmPaths.add(NARRATIVE_PATH);
        rmPaths.add(TIME_PATH);
        rmPaths.add(ISM_TRANSITION_PATH);
        rmPaths.add(TEMPLATE_ID_PATH);
    }
    public static Collection<ArchetypeElementVO> getRMElements(String idArchetype, String idTemplate, String entryType) {
        return getRMElements(idArchetype, idTemplate, entryType, "");
    }

    public static Collection<ArchetypeElementVO> getRMElements(String idArchetype, String idTemplate, String entryType, String parentPath) {
        Collection<ArchetypeElementVO> rmArchetypeElements = new ArrayList<ArchetypeElementVO>();
        if (OpenEHRConst.OBSERVATION.equals(entryType)) {
            String eventsTimePath = EVENT_TIME_PATH;
            //EventTime
            rmArchetypeElements.add(
                    new ArchetypeElementVOBuilder()
                            .setName(OpenEHRLanguageManager.getMessage("EventTime"))
                            .setDescription(OpenEHRLanguageManager.getMessage("EventTimeDesc"))
                            .setType(OpenEHRDataValues.DV_DATE_TIME)
                            .setIdArchetype(idArchetype).setIdTemplate(idTemplate)
                            .setPath(parentPath + eventsTimePath).createArchetypeElementVO());
        }else if (OpenEHRConst.INSTRUCTION.equals(entryType)){
            //Expiry Time
            rmArchetypeElements.add(
                    new ArchetypeElementVOBuilder()
                            .setName(OpenEHRLanguageManager.getMessage("ExpireTime"))
                            .setDescription(OpenEHRLanguageManager.getMessage("ExpireTimeDesc"))
                            .setType(OpenEHRDataValues.DV_DATE_TIME)
                            .setIdArchetype(idArchetype)
                            .setIdTemplate(idTemplate)
                            .setPath(parentPath + EXPIRY_TIME_PATH).createArchetypeElementVO());
            //Narrative Description
            rmArchetypeElements.add(
                    new ArchetypeElementVOBuilder()
                            .setName(OpenEHRLanguageManager.getMessage("NarrativeDescription"))
                            .setDescription(OpenEHRLanguageManager.getMessage("NarrativeDescriptionDesc"))
                            .setType(OpenEHRDataValues.DV_TEXT)
                            .setIdArchetype(idArchetype)
                            .setIdTemplate(idTemplate)
                            .setPath(parentPath + NARRATIVE_PATH)
                            .createArchetypeElementVO());
            //Detailed activity timing
            rmArchetypeElements.add(
                    new ArchetypeElementVOBuilder()
                            .setName(OpenEHRLanguageManager.getMessage("DetailedActivityTiming"))
                            .setDescription(OpenEHRLanguageManager.getMessage("DetailedActivityTimingDesc"))
                            .setType(OpenEHRDataValues.DV_PARSABLE)
                            .setIdArchetype(idArchetype)
                            .setIdTemplate(idTemplate)
                            .setPath(parentPath + TIMING_PATH)
                            .createArchetypeElementVO());
        }else if (OpenEHRConst.ACTION.equals(entryType)){
            //Date and time Action step performed
            rmArchetypeElements.add(
                    new ArchetypeElementVOBuilder()
                            .setName(OpenEHRLanguageManager.getMessage("DateTimeActionPerformed"))
                            .setDescription(OpenEHRLanguageManager.getMessage("DateTimeActionPerformedDesc"))
                            .setType(OpenEHRDataValues.DV_DATE_TIME)
                            .setIdArchetype(idArchetype)
                            .setIdTemplate(idTemplate)
                            .setPath(parentPath + TIME_PATH)
                            .createArchetypeElementVO());
            //Current Action State
            rmArchetypeElements.add(
                    new ArchetypeElementVOBuilder()
                            .setName(OpenEHRLanguageManager.getMessage("CurrentActionState"))
                            .setDescription(OpenEHRLanguageManager.getMessage("CurrentActionStateDesc"))
                            .setType(OpenEHRDataValues.DV_CODED_TEXT)
                            .setIdArchetype(idArchetype)
                            .setIdTemplate(idTemplate)
                            .setPath(parentPath + ISM_TRANSITION_PATH)
                            .createArchetypeElementVO());
        }
        if (parentPath.isEmpty()) { //TODO Check if this assumption is correct
            //Template Id
            rmArchetypeElements.add(
                    new ArchetypeElementVOBuilder()
                            .setName(OpenEHRLanguageManager.getMessage("TemplateId"))
                            .setDescription(OpenEHRLanguageManager.getMessage("TemplateIdDesc"))
                            .setType(OpenEHRDataValues.DV_TEXT)
                            .setIdArchetype(idArchetype)
                            .setIdTemplate(idTemplate)
                            .setPath(TEMPLATE_ID_PATH)
                            .createArchetypeElementVO());
        }
        return rmArchetypeElements;
    }

    public static Collection<ClusterVO> getRMClusters(String idArchetype, String idTemplate) {
        Collection<ClusterVO> rmArchetypeClusters = new ArrayList<ClusterVO>();
        ClusterVO clusterVO =
                new ClusterVO(
                        OpenEHRLanguageManager.getMessage("ArchetypeDetails"),
                        OpenEHRLanguageManager.getMessage("ArchetypeDetails"),
                        OpenEHRConst.CLUSTER,
                        idArchetype,
                        idTemplate,
                        ARCHETYPE_DETAILS_PATH
                );
        rmArchetypeClusters.add(clusterVO);
        return rmArchetypeClusters;
    }

    public final static Collection<String> getRmPaths(){
        return rmPaths;
    }

    public static boolean isRMPath(String path) {
        Iterator<String> i = rmPaths.iterator();
        while(i.hasNext()){
            if (path.endsWith(i.next())){
                return true;
            }
        }
        return false;
    }
}
