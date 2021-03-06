package se.cambio.openehr.util;

import openEHR.v1.template.*;
import org.openehr.am.archetype.Archetype;
import org.openehr.am.archetype.constraintmodel.*;
import org.openehr.am.archetype.constraintmodel.CAttribute.Existence;
import org.openehr.am.archetype.constraintmodel.primitive.CString;
import org.openehr.am.openehrprofile.datatypes.quantity.CDvQuantity;
import org.openehr.am.openehrprofile.datatypes.quantity.CDvQuantityItem;
import org.openehr.am.openehrprofile.datatypes.text.CCodePhrase;
import org.openehr.am.template.FlatteningException;
import org.openehr.am.template.TermMap;
import org.openehr.am.template.UnknownArchetypeException;
import org.openehr.am.template.UnknownTemplateException;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.datatypes.text.CodePhrase;
import org.openehr.rm.support.basic.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;

public class TemplateFlattener {

    public TemplateFlattener() {
        termMap = new TermMap();
    }

    public Archetype toFlattenedArchetype(TEMPLATE template,
                                          Map<String, Archetype> archetypeMap) throws FlatteningException {

        return toFlattenedArchetype(template, archetypeMap, new HashMap<>());
    }


    public Archetype toFlattenedArchetype(TEMPLATE template,
                                          Map<String, Archetype> archetypeMap,
                                          Map<String, TEMPLATE> templateMap) throws FlatteningException {
        if (archetypeMap == null) {
            throw new FlatteningException("null archetypeMap");
        }
        this.archetypeMap = archetypeMap;
        // no need to copy templateMap
        this.templateMap = templateMap;

        log.debug("Loaded archetype/template maps, total archetypes: "
                + archetypeMap.size() + ", total templates: "
                + (templateMap == null ? 0 : templateMap.size()));

        return toFlattenedArchetype(template);
    }


    private Archetype toFlattenedArchetype(TEMPLATE template)
            throws FlatteningException {

        log.debug("Flattening template.. STARTED");

        Archetyped definition = template.getDefinition();

        // start flattening
        Archetype flattended = flattenArchetyped(definition);
        flattended.reloadNodeMaps();

        log.debug("Flattening template DONE");

        return flattended;
    }

    private Archetype flattenArchetyped(Archetyped definition) throws FlatteningException {

        log.debug("flattening archetyped on archetype: " + definition.getArchetypeId());

        if (definition instanceof COMPOSITION) {

            // only exception when parentArchetype is always not set
            return flattenComposition((COMPOSITION) definition);

        } else if (definition instanceof ITEMSTRUCTURE) {

            return flattenItemStructure(null, (ITEMSTRUCTURE) definition);

        } else if (definition instanceof ContentItem) {

            return flattenContentItem(null, (ContentItem) definition);

        } else if (definition instanceof ITEM) {

            return flattenItem(null, (ITEM) definition);

        } else {
            throw new FlatteningException("Unkown archetyped sub-type");
        }
    }

    private Archetype flattenItem(Archetype parentArchetype, ITEM item)
            throws FlatteningException {

        log.debug("flattening item at path " + item.getPath());

        Archetype archetype = retrieveArchetype(item.getArchetypeId());

        applyTemplateConstraints(archetype, item);

        fillArchetypeSlot(parentArchetype, archetype, item.getPath(),
                item.getName());

        return archetype;
    }

    private Archetype flattenComposition(COMPOSITION composition) throws FlatteningException {

        log.debug("flattening composition on archetype: " + composition.getArchetypeId());

        Archetype archetype = retrieveArchetype(composition.getArchetypeId());
        CComplexObject root = archetype.getDefinition();
        CAttribute contentAttribute = root.getAttribute(CONTENT);

        removeArchetypeSlots(contentAttribute);

        // handle "/content" attribute
        ContentItem[] items = composition.getContentArray();
        if (items != null && items.length > 0) {
            String path = "/" + CONTENT;

            if (contentAttribute == null) {
                List<CObject> alternatives = new ArrayList<>();
                contentAttribute = new CMultipleAttribute(path, CONTENT,
                        Existence.OPTIONAL, Cardinality.LIST, alternatives);
                archetype.getDefinition().addAttribute(contentAttribute);
            }

            for (ContentItem item : items) {

                log.debug("flattening composition.content..");

                flattenContentItem(archetype, item);
            }
        }

        // TODO handle "/context" attribute
        // log.warn("flattening composition.context..not implemented");

        applyRules(archetype, composition.getRuleArray());
        applyNameConstraint(archetype, archetype.getDefinition(),
                composition.getName(), "/");

        return archetype;
    }

    private Archetype flattenContentItem(Archetype parentArchetype,
                                         ContentItem definition) throws FlatteningException {

        log.debug("flattening content_item on archetype: "
                + definition.getArchetypeId() + " on path: " + definition.getPath());

        Archetype archetype;
        String templateId = definition.getTemplateId();

        if (templateId == null) {

            archetype = retrieveArchetype(definition.getArchetypeId());

        } else {

            // TODO only needed at SECTION level so far for ifk2
            TEMPLATE template = retrieveTemplate(templateId);
            archetype = toFlattenedArchetype(template);
        }

        applyTemplateConstraints(archetype, definition);

        if (definition instanceof ENTRY) {

            flattenEntry(parentArchetype, archetype, (ENTRY) definition);

        } else if (definition instanceof SECTION) {

            flattenSection(parentArchetype, archetype, (SECTION) definition);

        } else {
            throw new FlatteningException(
                    "Unexpected subtype of ContentItem: " + definition);
        }
        return archetype;
    }

    private void applyTemplateConstraints(Archetype archetype,
                                          Archetyped definition) throws FlatteningException {

        log.debug("applying common template constraints.. ");

        if (archetype == null) {
            return;
        }

        String name = null;
        Statement[] rules = null;
        BigInteger max = null;
        BigInteger min = null;
        boolean hideOnForm = false;
        String annotation = null;

        if (definition instanceof ContentItem) {

            ContentItem item = (ContentItem) definition;
            name = item.getName();
            rules = item.getRuleArray();
            max = item.getMax();
            min = item.getMin();
            hideOnForm = item.getHideOnForm();
            annotation = item.getAnnotation();

        } else if (definition instanceof ITEMSTRUCTURE) {

            ITEMSTRUCTURE item = (ITEMSTRUCTURE) definition;
            name = item.getName();
            rules = item.getRuleArray();
            max = item.getMax();
            min = item.getMin();
            hideOnForm = item.getHideOnForm();
            annotation = item.getAnnotation();

        } else if (definition instanceof ITEM) {

            ITEM item = (ITEM) definition;
            name = item.getName();
            rules = item.getRuleArray();
            max = item.getMax();
            min = item.getMin();
            hideOnForm = item.getHideOnForm();
            annotation = item.getAnnotation();

        } else {
            log.warn("unsupported definition type: " + definition);
        }

        applyNameConstraint(archetype, archetype.getDefinition(), name, "/");

        applyRules(archetype, rules);

        applyOccurrencesConstraint(archetype, archetype.getDefinition(), max, min);

        applyHideOnFormConstraint(archetype.getDefinition(), hideOnForm);

        applyAnnotationConstraint(archetype.getDefinition(), annotation);
    }

    private void setPathPrefixOnCObjectTree(CObject cobj, String prefix)
            throws FlatteningException {

        // TODO shouldn't happen
        if (cobj == null) {
            log.warn("null cobj encountered at setPathPrefix(): " + prefix);
            return;
        }

        String path = cobj.path();
        if ("/".equals(path)) {
            path = prefix;
        } else {
            path = prefix + path;
        }
        cobj.setPath(path);

        if (cobj instanceof CComplexObject) {
            CComplexObject ccobj = (CComplexObject) cobj;
            for (CAttribute cattr : ccobj.getAttributes()) {
                path = cattr.path();
                cattr.setPath(prefix + path);
                for (Iterator<CObject> it = cattr.getChildren().iterator(); it.hasNext(); ) {

                    CObject child = it.next();
                    // TODO
                    if (child == null) {
                        it.remove();
                        log.warn("null child encountered remove in setPathPrefix()..");
                    }
                    setPathPrefixOnCObjectTree(child, prefix);
                }
            }
        }
    }

    private void updatePathWithNamedNodeOnCObjectTree(CObject cobj,
                                                      String nodeId, String name) throws FlatteningException {

        // TODO shouldn't happen
        if (cobj == null) {
            log.warn("null cobj in updatePathWithNamedNodeOnCObjectTree(): "
                    + nodeId + "/" + name);
            return;
        }

        String path = cobj.path();
        path = replaceNodeIdWithNamedNode(path, nodeId, name);
        cobj.setPath(path);

        //log.debug("cobj.path: " + cobj.path());

        if (cobj instanceof CComplexObject) {
            CComplexObject ccobj = (CComplexObject) cobj;

            for (CAttribute cattr : ccobj.getAttributes()) {

                path = cattr.path();
                path = replaceNodeIdWithNamedNode(path, nodeId, name);
                cattr.setPath(path);

                //log.debug("cattr.path: " + cattr.path());

                for (Iterator<CObject> it = cattr.getChildren().iterator(); it.hasNext(); ) {

                    CObject child = it.next();
                    // TODO
                    if (child == null) {
                        it.remove();
                        log.warn("null child encountered remove in setPathPrefix()..");
                    }
                    updatePathWithNamedNodeOnCObjectTree(child, nodeId, name);
                }
            }
        }
    }

    private String replaceNodeIdWithNamedNode(String path, String nodeId,
                                              String name) {
        return path.replaceFirst("\\[" + nodeId + "\\]",
                "\\[" + namedNodeSegment(nodeId, name) + "\\]");
    }

    private String namedNodeSegment(String nodeId, String name) {
        return nodeId + " and name/value='" + name + "'";
    }

    private Archetype flattenSection(Archetype parentArchetype,
                                     Archetype archetype, SECTION section) throws FlatteningException {

        log.debug("flattening section on archetype " + section.getArchetypeId()
                + " at path " + section.getPath());

        ContentItem[] items = section.getItemArray();
        String path = section.getPath();
        CComplexObject root = archetype.getDefinition();

        fillArchetypeSlot(parentArchetype, archetype, path, section.getName());

        CAttribute itemsAttribute = root.getAttribute(ITEMS);

        // handle SECTION.items
        if (items != null && items.length > 0) {

            // TODO shouldn't be necessary if the archetype has
            // proper archetype_slots
            if (itemsAttribute == null) {

                if (parentArchetype == null) {
                    path = "";
                } else {
                    path = root.path();
                }
                path += "/" + ITEMS;

                List<CObject> alternatives = new ArrayList<>();
                itemsAttribute = new CMultipleAttribute(path, ITEMS,
                        Existence.OPTIONAL, Cardinality.LIST, alternatives);
                root.addAttribute(itemsAttribute);
            }

            // flatten each item in the list
            for (ContentItem item : items) {

                log.debug("flattening section.items.. ");

                flattenContentItem(archetype, item);
            }
        }
        return archetype;
    }

    // validity check of node_id (archetype_id) and name among siblings
    private void checkSiblingNodeIdAndName(CAttribute parent,
                                           String nodeId, String name) throws FlatteningException {

        for (CObject cobj : parent.getChildren()) {
            if (nodeId.equals(cobj.path()) && name.equals(cobj.getNodeId())) {
                throw new FlatteningException("duplicated node_id/name: "
                        + nodeId + "/" + name + "at path: " + parent.path());
            }
        }
    }

    private int countChildOfArchetypeId(CAttribute cattr, String archetypeId) {
        int count = 0;
        for (CObject cobj : cattr.getChildren()) {
            String nodeId = cobj.getNodeId();
            if (nodeId == null) {
                continue;
            }
            if (nodeId.startsWith(archetypeId)) {
                count++;
            }
        }
        return count;
    }

    protected long adjustNodeIds(CObject cobj, long count)
            throws FlatteningException {

        if (cobj.getNodeId() != null) {
            count++;
            cobj.setNodeId(formatNodeId(count));
        }
        if (cobj instanceof CComplexObject) {
            for (CAttribute attr : ((CComplexObject) cobj).getAttributes()) {
                for (CObject child : attr.getChildren()) {
                    count = adjustNodeIds(child, count);
                }
            }
        }
        return count;
    }

    // TODO until this is in sync with pathNodeMap, this shouldn't be used
    protected void adjustNodeIds(CObject root) throws FlatteningException {
        this.nodeCount = adjustNodeIds(root, this.nodeCount);
    }

    // "/content" => "content"
    // "/content/name" => "name"
    // "/data[at0001]/items[at0002]" => "items"
    private String lastAttribute(String path) {
        int index = path.lastIndexOf("/");
        String attr = path.substring(index + 1, path.length());
        index = attr.indexOf("[");
        if (index >= 0) {
            attr = attr.substring(0, index);
        }
        log.debug("attribute name: " + attr);
        return attr;
    }

    private Archetype flattenEntry(Archetype parentArchetype,
                                   Archetype archetype, ENTRY definition) throws FlatteningException {

        log.debug("flattening entry on archetype: " + definition.getArchetypeId()
                + ", path: " + ((ENTRY) definition).getPath());

        String path = definition.getPath();

        // bind current archetype to the parent
        fillArchetypeSlot(parentArchetype, archetype, path, definition.getName());

        // common part for all entries
        ITEM[] items = definition.getItemsArray();
        if (items != null) {
            for (ITEM item : items) {
                flattenItem(archetype, item);
            }
        }

        // more specialized handling of sub-entry
        if (definition instanceof EVALUATION) {

            // no special handling

        } else if (definition instanceof OBSERVATION) {

            // no special handling

        } else if (definition instanceof ACTION) {

            // TODO handle description item
            ACTION action = (ACTION) definition;
            retrieveArchetype(action.getArchetypeId());
            ITEMSTRUCTURE description = action.getDescription();
            if (description != null) {
                flattenItemStructure(archetype, description);
            }

        } else if (definition instanceof INSTRUCTION) {

            flattenInstruction(parentArchetype, archetype, (INSTRUCTION) definition);

        } else if (definition instanceof ADMINENTRY) {

            // no special handling
        }

        return archetype;
    }

    private void fillArchetypeSlot(Archetype parentArchetype,
                                   Archetype archetype, String path, String name) throws FlatteningException {

        String archetypeId = archetype.getArchetypeId().toString();
        CComplexObject root = archetype.getDefinition();

        if (parentArchetype != null) {

            // TODO has a quick-fix for the following path syntax
            // "/activities[at0001 and name/value='Medication activity']/description"
            // WILL NOT WORK IN OTHER CASES!!

            int hybridStart = path.indexOf(" and name/value='");
            if (hybridStart > 0) {
                int index = path.indexOf("]");
                path = path.substring(0, hybridStart) + path.substring(index);

                log.debug("hybrid path detected, converted physical path: " + path);
            }

            CAttribute attribute = getParentAttribute(parentArchetype, path);
            if (attribute == null) {
                throw new FlatteningException("CAttribute not found at " + path);
            }
            removeArchetypeSlots(attribute);

            root.setNodeId(archetypeId);
            String pathSegment = archetypeId;
            if (name != null) {
                checkSiblingNodeIdAndName(attribute, archetypeId, name);
                pathSegment = namedNodeSegment(archetypeId, name);
            }
            setPathPrefixOnCObjectTree(root, attribute.path() + "["
                    + pathSegment + "]");

            attribute.addChild(root);
        }
    }

    private CAttribute getParentAttribute(Archetype archetype, String path)
            throws FlatteningException {

        String parentPath = Locatable.parentPath(path);
        ArchetypeConstraint ac = archetype.node(parentPath);

        if (!(ac instanceof CComplexObject)) {
            throw new FlatteningException("Parent node not found at " + path
                    + ", computed parentPath: " + parentPath);
        }
        CComplexObject parentNode = (CComplexObject) ac;
        String attributeName = lastAttribute(path);
        CAttribute attribute = parentNode.getAttribute(attributeName);
        return attribute;
    }

    private void flattenInstruction(Archetype parentArchetype,
                                    Archetype archetype, INSTRUCTION instruction)
            throws FlatteningException {

        log.debug("flattening instruction on archetype: " + instruction.getArchetypeId()
                + ", path: " + ((ENTRY) instruction).getPath());

        ITEMSTRUCTURE[] descriptions = instruction.getActivityDescriptionArray();

        if (descriptions != null) {
            for (ITEMSTRUCTURE item : descriptions) {
                flattenItemStructure(archetype, item);
            }
        }
    }

    private void removeArchetypeSlots(CAttribute cattr) {
        if (cattr == null) {
            return;
        }
        // TODO verify slots by RM types
        for (Iterator<CObject> it = cattr.getChildren().iterator(); it.hasNext(); ) {
            CObject cobj = it.next();
            if (cobj instanceof ArchetypeSlot) {
                it.remove();
                log.debug("archetype_slot removed from attribute "
                        + cattr.getRmAttributeName());
            }
        }
    }

    private Archetype flattenItemStructure(Archetype parentArchetype,
                                           ITEMSTRUCTURE structure) throws FlatteningException {

        log.debug("flattening item_structure on archetype: "
                + structure.getArchetypeId() + " on path: " + structure.getPath());

        Archetype archetype = retrieveArchetype(structure.getArchetypeId());

        applyTemplateConstraints(archetype, structure);

        fillArchetypeSlot(parentArchetype, archetype, structure.getPath(),
                structure.getName());

        ITEM[] items = structure.getItemsArray();
        if (items != null) {
            for (ITEM item : items) {
                flattenItem(archetype, item);
            }
        }
        return archetype;
    }

    void applyRules(Archetype archetype, Statement[] rules)
            throws FlatteningException {

        if (rules == null) {
            return;
        }
        String name = null;
        String leadingPath = null;

        for (Statement rule : rules) {
            if (rule.getName() != null) {
                if (name == null) {
                    name = rule.getName();
                    leadingPath = rule.getPath();

                } else if (rule.getPath().equals(leadingPath)) {

                    // more than one named node for the same path,
                    // thus no need to rewrite the paths
                    log.debug("more than one named node [" + name
                            + "] on path: " + leadingPath);

                    name = null;
                    break;
                }
            }
        }

        for (Statement rule : rules) {
            if (name != null && rule.getPath().startsWith(leadingPath)
                    && rule.getPath().length() > leadingPath.length()) {
                int len = leadingPath.length();
                String path = rule.getPath();
                path = path.substring(0, len - 1) + " and name/value='"
                        + name + "'" + path.substring(len - 1);

                rule.setPath(path);

                log.debug("rewrote path with named node: {}", path);
            }
            applyRule(archetype, rule);
        }
    }

    void applyRule(Archetype archetype, Statement rule)
            throws FlatteningException {

        log.debug("apply rule [" + rule + "] on archetype: "
                + archetype.getArchetypeId().toString());

        String path = rule.getPath();

        ArchetypeConstraint constraint = archetype.node(rule.getPath());

        if (constraint == null) {
            throw new FlatteningException("no constraint on path: "
                    + path + " of " + archetype.getArchetypeId());
        }

        constraint = applyNameConstraint(archetype, constraint, rule.getName(),
                path);

        applyOccurrencesConstraints(archetype, constraint, rule);

        applyDefaultValueConstraint(constraint, rule.getDefault());

        applyHideOnFormConstraint(constraint, rule.getHideOnForm());

        applyAnnotationConstraint(constraint, rule.getAnnotation());

        applyValueConstraint(archetype, constraint, rule);

        archetype.updatePathNodeMap((CObject) constraint);

        if (constraint != null) {
            log.debug("newly set Occurrences: " + ((CObject) constraint).getOccurrences());
        }

    }

    // TODO
    protected void applyHideOnFormConstraint(ArchetypeConstraint constraint,
                                             boolean hideOnForm) {
    }

    // TODO
    protected void applyAnnotationConstraint(ArchetypeConstraint constraint,
                                             String annotation) {
        if (annotation == null || annotation.length() == 0) {
            return;
        }
        constraint.setAnnotation(annotation);
    }

    protected void applyDefaultValueConstraint(ArchetypeConstraint constraint,
                                               String defaultValue) throws FlatteningException {

        if (defaultValue == null) {
            return;
        }

        log.debug("applying default value on path: " + constraint.path());

        if (!(constraint instanceof CComplexObject)) {

            throw new FlatteningException("failed to apply default constraint,"
                    + "unexpected constraint node: " + constraint.getClass());
        }

        CComplexObject ccobj = (CComplexObject) constraint;

        if (!ccobj.getRmTypeName().equalsIgnoreCase(ELEMENT)) {

            throw new FlatteningException("failed to apply default constraint,"
                    + "unexpected rmType[" + ccobj.getRmTypeName()
                    + "] on node: " + ccobj.path());
        }

        // handles case like:
        //
        // ELEMENT match {
        //     value match {
        //         DV_TEXT matches {*}
        //     }
        // }
        // or
        // ELEMENT match {
        //     value match {
        //         DV_CODED_TEXT matches {*}
        //     }
        // }
        //
        // replace it with CPrimitiveObject.constrainString or CCodePhrase
        //
        // TODO: an alternative is to use the term_definition
        //       in archetype_ontology
        //

        CAttribute cattr = ccobj.getAttribute(VALUE);

        if (cattr == null) {
            cattr = new CSingleAttribute(ccobj.path() + "/" + VALUE,
                    VALUE, Existence.REQUIRED);

        } else if (cattr.getChildren().size() == 1) {
            CObject child = cattr.getChildren().get(0);

            if (child instanceof CComplexObject) {
                CComplexObject childCCObj = (CComplexObject) child;
                String rmType = childCCObj.getRmTypeName();
                if (DV_TEXT.equals(rmType) || DV_CODED_TEXT.equals(rmType)) {
                    cattr.removeChild(child);
                }
            }
        }

        Interval<Integer> occurrences = new Interval<Integer>(1, 1);

        // simple text value
        if (!defaultValue.contains("::")) {

            String path = cattr.path() + "/" + VALUE;
            CComplexObject valueObj = CComplexObject.createSingleRequired(path, DV_TEXT);
            CAttribute valueAttr = CSingleAttribute.createRequired(path, VALUE);
            cattr.addChild(valueObj);
            valueObj.addAttribute(valueAttr);

            CString cstring = new CString(null, null, null, defaultValue);
            CPrimitiveObject cpo = CPrimitiveObject.createSingleRequired(path, cstring);
            valueAttr.addChild(cpo);

            log.debug("c_string applied on path: " + constraint.path());

        } else {
            // or coded_text value
            String path = ccobj.path() + "/" + VALUE;
            CodePhrase codePhrase = parseCodePhraseAndCollectText(defaultValue,
                    path);
            CCodePhrase ccp = new CCodePhrase(path, occurrences, null, cattr,
                    null, null, codePhrase, null);
            cattr.addChild(ccp);

            log.debug("c_code_phrase constraint applied on path: " + constraint.path());
        }
    }

    protected void applyOccurrencesConstraints(Archetype archetype,
                                               ArchetypeConstraint constraint, Statement rule) throws FlatteningException {

        BigInteger max = rule.getMax();
        BigInteger min = rule.getMin();

        if (max != null || min != null) {
            if (constraint instanceof CObject) {
                applyOccurrencesConstraint(archetype, (CObject) constraint, rule.getMax(),
                        rule.getMin());
            }
        }
    }

    protected void applyOccurrencesConstraint(Archetype archetype,
                                              CObject cobj, BigInteger max, BigInteger min) throws FlatteningException {

        log.debug("applyOccurrencesConstraint, min: " + min + ", max: " + max
                + ", at: " + cobj.path());


        String path = cobj.path();

        Interval<Integer> occurrences = cobj.getOccurrences();

        // default occurrences, required [1,1]
        // special-case in archetype root, then the default occurrences
        // can be overriden
        if ((!"/".equals(path)) && occurrences == null) {

            log.warn("try to set occurrences constraint on default(null)"
                    + " occurrences: " + cobj.path());
            return;
        }

        if (max != null && occurrences.getUpper() != null
                && max.intValue() > occurrences.getUpper()) {
            throw new FlatteningException("more permissive max occurrences, " + path);
        }

        if (max != null && occurrences.getLower() != null
                && max.intValue() < occurrences.getLower()) {
            throw new FlatteningException("contradicting max occurrences [max: "
                    + max.intValue() + ", occurrences.lower: "
                    + occurrences.getLower() + "] at path: " + path);
        }

        if (min != null && occurrences.getLower() != null
                && min.intValue() < occurrences.getLower()) {
            throw new FlatteningException("more permissive min occurrences, " + path);
        }

        if (min != null && occurrences.getUpper() != null
                && min.intValue() > occurrences.getUpper().intValue()) {
            throw new FlatteningException("contradicting min occurrences, " + path);
        }

        // it's required already
        // special case for archetype node!
        if ((!"/".equals(path)) && occurrences.getUpper() != null && occurrences.getLower() != null
                && occurrences.getUpper() == 1
                && occurrences.getLower() == 1) {
            log.warn("try to set occurrences constraint on required node: "
                    + cobj.path());
            return;
        }

        // TODO temp fix for missing min=0 when setting
        // optional occurrences in template designer
        if (min == null && max != null) {
            min = BigInteger.valueOf(0L);
        }

        Integer lower = null;
        Integer upper = null;

        if (occurrences != null) {
            lower = occurrences.getLower();
            upper = occurrences.getUpper();
        }
        if (min != null) {
            lower = min.intValue();
            if (upper != null && lower > upper) {
                upper = lower;
            }
        }
        if (max != null) {
            upper = max.intValue();
            if (lower != null && lower > upper) {
                lower = upper;
            }
        }

        Interval<Integer> newOccurrences =
                new Interval<Integer>(lower, upper, lower != null, upper != null);


        log.debug("newOccurrences: " + newOccurrences);

        if (newOccurrences.getLower() != null
                && newOccurrences.getLower() > 0
                && !"/".equals(path)) {

            CAttribute parent = getParentAttribute(archetype, path);

            if (parent instanceof CMultipleAttribute) {
                CMultipleAttribute cma = (CMultipleAttribute) parent;

                log.debug("setting parent.cardinality: " + cma.getCardinality());

                // TODO temporarily switched off
                cma.getCardinality().getInterval().setLower(newOccurrences.getLower());

                log.debug("AFTER parent.cardinality: " + cma.getCardinality());
            } else {
                if (parent == null) {
                    log.debug("parent null at " + cobj.path());
                }
            }
        }
        cobj.setOccurrences(newOccurrences);
    }

    // TODO add support for name-based hybrid path
    // At least for Rule-based name constraint, if the node doesn't
    // exist before, a copy of the node should be created with given name
    protected CComplexObject applyNameConstraint(Archetype archetype,
                                                 ArchetypeConstraint constraint, String name, String localPath)
            throws FlatteningException {

        log.debug("applying name constraint [" + name + "] on path: "
                + localPath + "constraint: " + constraint.path());

        // preconditions
        assert (constraint instanceof CComplexObject);

        CComplexObject ccobj = (CComplexObject) constraint;
        if (name == null) {
            return ccobj;
        }

        //String path = ccobj.path();
        String path = localPath;

        if (!"/".equals(path)) {
            CAttribute parent = getParentAttribute(archetype, path);
            assert (parent != null);

            // add a copy of the ccobj if sibling with same node_id exists
            // this is the key to enable named-node path in template
            if (hasSiblingNodeWithNodeId(parent, ccobj.getNodeId())) {
                removeUnnamedSiblingNode(archetype, parent, ccobj.getNodeId());

                ccobj = (CComplexObject) ccobj.copy();
                parent.addChild(ccobj);
                ccobj.setParent(parent);
                checkSiblingNodeIdAndName(parent, ccobj.getNodeId(), name);

                log.debug("sibling node with same node_id added, " + name);
            }
        }

        // TODO check physicalPath needed
        path = ccobj.path();

        log.debug("applyNameConstraint - middle ccobj.path: " + ccobj.path());

        // perhaps unnecessary
        CAttribute nameAttr = ccobj.getAttribute(NAME);
        CPrimitiveObject cpo = null;

        if (nameAttr == null) {

            if ("/".equals(path)) {
                path = path + NAME;
            } else {
                path = path + "/" + NAME;
            }
            CComplexObject nameObj = CComplexObject.createSingleRequired(path, DV_TEXT);
            nameAttr = CSingleAttribute.createRequired(path, NAME);
            nameAttr.addChild(nameObj);

            path = path + "/" + VALUE;
            CAttribute valueAttr = CSingleAttribute.createRequired(path, VALUE);
            nameObj.addAttribute(valueAttr);
            CString cstring = constrainString(name);
            cpo = CPrimitiveObject.createSingleRequired(path, cstring);
            valueAttr.addChild(cpo);
        }
        ccobj.addAttribute(nameAttr);
        updatePathWithNamedNodeOnCObjectTree(ccobj, ccobj.getNodeId(), name);
        archetype.updatePathNodeMapRecursively(ccobj);

        log.debug("after setting name, cobj.path: " + ccobj.path());

        return ccobj;
    }

    private void removeUnnamedSiblingNode(Archetype archetype,
                                          CAttribute parent, String nodeId) throws FlatteningException {

        for (Iterator<CObject> it = parent.getChildren().iterator(); it.hasNext(); ) {
            CObject cobj = it.next();
            if (nodeId.equals(cobj.getNodeId())) {
                if (!(cobj instanceof CComplexObject)) {
                    throw new FlatteningException("unexpected constraint type: "
                            + cobj.getClass() + " for node_id[" + nodeId + "] at "
                            + parent.path());
                }
                CComplexObject ccobj = (CComplexObject) cobj;

                if (!ccobj.hasAttribute(NAME)) {

                    it.remove();

                    // seems unnecessary
                    // archetype.reloadNodeMaps();

                    log.debug("Unnamed sibling node[" + nodeId + "] removed..");

                    break;
                }
            }
        }
    }

    private boolean hasSiblingNodeWithNodeId(CAttribute parent, String nodeId) {
        for (CObject cobj : parent.getChildren()) {
            if (nodeId.equals(cobj.getNodeId())) {
                return true;
            }
        }
        return false;
    }

    protected void applyValueConstraint(Archetype archetype, ArchetypeConstraint constraint, Statement rule)
            throws FlatteningException {

        if (rule.getConstraint() == null) {
            return;
        }

        log.debug("applying value constraint on path: " + constraint.path());

        if (!(constraint instanceof CComplexObject)) {
            throw new FlatteningException("Unexpected constraint type: " + (constraint.getClass()));
        }

        CComplexObject ccobj = (CComplexObject) constraint;
        if (!ccobj.getRmTypeName().equalsIgnoreCase(ELEMENT)) {
            throw new FlatteningException("Unexpected constraint rmType: "
                    + ccobj.getRmTypeName());
        }
        ValueConstraint vc = rule.getConstraint();

        if (vc instanceof TextConstraint) {

            TextConstraint tc = (TextConstraint) vc;
            applyTextConstraint(ccobj, tc);

        }
        if (vc instanceof QuantityConstraint) {

            QuantityConstraint tc = (QuantityConstraint) vc;
            applyQuantityConstraint(ccobj, tc);
            archetype.updatePathNodeMapRecursively(ccobj);

        } else if (vc instanceof MultipleConstraint) {

            MultipleConstraint mc = (MultipleConstraint) vc;
            applyMultipleConstraint(ccobj, mc);
        }
    }

    protected void applyQuantityConstraint(CComplexObject ccobj,
                                           QuantityConstraint qc) throws FlatteningException {

        log.debug("applying quantity constraint on path: " + ccobj.path());

        String[] includedUnits = qc.getIncludedUnitsArray();
        String[] excludedUnits = qc.getExcludedUnitsArray();
        QuantityUnitConstraint[] magnitudeUnits = qc.getUnitMagnitudeArray();
        CDvQuantityItem item;
        CAttribute valueAttr = ccobj.getAttribute(VALUE);
        String valuePath = ccobj.path() + "/" + VALUE;

        if (magnitudeUnits != null && magnitudeUnits.length == 1) {

            log.debug("setting unit_magnitude quantity constraint");

            QuantityUnitConstraint quc = magnitudeUnits[0];
            Interval<Double> magnitude = new Interval<Double>(
                    quc.getMinMagnitude(), quc.getMaxMagnitude(),
                    quc.getIncludesMinimum(), quc.getIncludesMaximum());

            item = new CDvQuantityItem(magnitude, quc.getUnit());
            if (valueAttr != null) {
                valueAttr.removeAllChildren();
                CDvQuantity cdq = CDvQuantity.singleRequired(valuePath, item);
                valueAttr.addChild(cdq);
            }

        } else if (includedUnits != null && includedUnits.length == 1) {
            // <constraint xsi:type="quantityConstraint">
            //     <includedUnits>mmol/L</includedUnits>
            // </constraint>

            log.debug("setting included_units quantity constraint");

            item = new CDvQuantityItem(includedUnits[0]);

        } else if (excludedUnits != null && excludedUnits.length > 0) {

            log.debug("setting excluded_units quantity constraint");

            // <constraint xsi:type="quantityConstraint">
            //     <excludedUnits>in</excludedUnits>
            // </constraint>

            if (valueAttr == null) {
                throw new FlatteningException(
                        "Missing value attribute for quantityConstraint.excludedUnits");
            }
            if (valueAttr.getChildren() == null || valueAttr.getChildren().isEmpty()) {
                throw new FlatteningException(
                        "Missing child obj for quantityConstraint.excludedUnits");
            }
            if (valueAttr.getChildren().size() > 1) {
                throw new FlatteningException(
                        "More than one child obj for quantityConstraint.excludedUnits");
            }
            CObject child = valueAttr.getChildren().get(0);
            if (!(child instanceof CDvQuantity)) {
                throw new FlatteningException(
                        "Non-CDvQuantity child obj for quantityConstraint.excludedUnits");
            }
            CDvQuantity cdq = (CDvQuantity) child;
            if (cdq.getList() == null || cdq.getList().isEmpty()) {
                throw new FlatteningException(
                        "Empty CDvQuantity.list for quantityConstraint.excludedUnits");
            }

            cdq.removeItemByUnitsList(excludedUnits);
            return;

        } else {
            // TODO > 1 in the array etc
            throw new FlatteningException(
                    "Unsupported quantityConstraint in Template: " + qc);
        }


        if (valueAttr == null) {

            valueAttr = new CSingleAttribute(valuePath, VALUE, Existence.REQUIRED);
            CDvQuantity cdq = CDvQuantity.singleRequired(valuePath, item);
            valueAttr.addChild(cdq);
            ccobj.addAttribute(valueAttr);

        } else {

            // deal with empty c_dv_quantity to add new items
            if (valueAttr.getChildren() == null || valueAttr.getChildren().isEmpty()) {
                throw new FlatteningException(
                        "Missing child obj for quantityConstraint.excludedUnits");
            }
            if (valueAttr.getChildren().size() > 1) {
                throw new FlatteningException(
                        "More than one child obj for quantityConstraint.excludedUnits");
            }
            CObject child = valueAttr.getChildren().get(0);
            if (!(child instanceof CDvQuantity)) {
                throw new FlatteningException(
                        "Non-CDvQuantity child obj for quantityConstraint.excludedUnits");
            }
            CDvQuantity cdq = (CDvQuantity) child;
            cdq.addItem(item);
        }
    }

    protected void applyTextConstraint(CComplexObject ccobj,
                                       TextConstraint tc) throws FlatteningException {

        log.debug("applying text constraint on path: " + ccobj.path());

        String[] includedValues = tc.getIncludedValuesArray();
        String[] excludedValues = tc.getExcludedValuesArray();

        CAttribute valueAttr = ccobj.getAttribute(VALUE);
        String valuePath = ccobj.path() + "/" + VALUE;
        String definingCodePath = valuePath + "/" + DEFINING_CODE;

        if (includedValues != null && includedValues.length > 0) {
            if (valueAttr == null) {
                valueAttr = new CSingleAttribute(valuePath, VALUE,
                        Existence.REQUIRED);
                ccobj.addAttribute(valueAttr);
            } else if (valueAttr.getChildren().size() > 0) {
                valueAttr.removeAllChildren();
            }

            CComplexObject valueObj = CComplexObject.createSingleRequired(
                    valuePath, DV_CODED_TEXT);
            valueAttr.addChild(valueObj);

            CAttribute definingCode = CSingleAttribute.createRequired(
                    definingCodePath, DEFINING_CODE);
            valueObj.addAttribute(definingCode);

            List<String> codeList = new ArrayList<String>();
            String terminology = null;
            String text = null;
            String code = null;
            for (String value : includedValues) {
                int index = value.indexOf("::");
                int index2 = value.lastIndexOf("::");
                if (index == index2 || index2 > value.length() - 2) {
                    throw new FlatteningException(
                            "wrong syntax for coded text: " + value);
                }
                terminology = value.substring(0, index);
                code = value.substring(index + 2, index2);
                text = value.substring(index2 + 2);
                codeList.add(code);
                termMap.addTerm(terminology, code, text, valuePath);
            }

            CCodePhrase ccp = CCodePhrase.singleRequired(definingCodePath,
                    terminology, codeList);
            definingCode.addChild(ccp);

        } else if (excludedValues != null && excludedValues.length > 0) {
            if (valueAttr == null) {
                throw new FlatteningException(
                        "failed to set excluded values on text constraint, "
                                + "VALUE attribute missing");
            }
            if (valueAttr.getChildren().size() == 1) {
                CObject child = valueAttr.getChildren().get(0);
                if (child instanceof CComplexObject) {
                    CComplexObject childCCObj = (CComplexObject) child;
                    if (childCCObj.getRmTypeName().equals(DV_CODED_TEXT)) {
                        CAttribute definingCodeAttr =
                                childCCObj.getAttribute(DEFINING_CODE);

                        if (definingCodeAttr == null) {
                            throw new FlatteningException(
                                    "Missing defining_code attribute");
                        }

                        CCodePhrase ccp =
                                (CCodePhrase) definingCodeAttr.getChildren().get(0);

                        log.debug("before codeList.size: " + ccp.getCodeList().size());

                        for (Iterator<String> it = ccp.getCodeList().iterator(); it.hasNext(); ) {
                            String code = it.next();
                            for (String value : excludedValues) {
                                int index = value.indexOf("::");
                                if (code.equals(value.substring(index + 2))) {
                                    it.remove();
                                }
                            }
                        }
                        log.debug("after codeList.size: " + ccp.getCodeList().size());

                    } else {
                        // TODO other data types?
                    }
                } else {
                    throw new FlatteningException(
                            "Unexpected VALUE.child constrainType: "
                                    + (child == null ? "null" : child.getClass()));
                }
            } else {
                throw new FlatteningException("Unexpected VALUE.children.size: "
                        + valueAttr.getChildren().size());
            }
        }
    }

    protected void applyMultipleConstraint(CComplexObject ccobj,
                                           MultipleConstraint mc) throws FlatteningException {

        log.debug("applying multiple constraint on path: " + ccobj.path());

        String[] includedTypes = mc.getIncludedTypesArray();
        CAttribute cattr = ccobj.getAttribute(VALUE);

        if (cattr != null) {
            if (cattr.getChildren() == null || cattr.getChildren().isEmpty()) {
                throw new FlatteningException(
                        "failed to set value constraint, null/empty children list");
            }

            List<CObject> newChildrenList = new ArrayList<>();
            for (CObject child : cattr.getChildren()) {
                String childType = child.getRmTypeName();
                for (String type : includedTypes) {
                    if ((DATA_TYPES_PREFIX + type).equalsIgnoreCase(childType)
                            || type.equalsIgnoreCase(childType)) {
                        newChildrenList.add(child);
                        break;
                    }
                }
            }
            cattr.removeAllChildren();
            for (CObject child : newChildrenList) {
                cattr.addChild(child);
            }
            log.debug("total " + newChildrenList.size() + " children left");

        } else {
            // in case value attribute doesn't exist in archetype
            String valuePath = ccobj.path() + "/" + VALUE;
            cattr = new CSingleAttribute(valuePath, VALUE, Existence.REQUIRED);
            ccobj.addAttribute(cattr);

            for (String rmType : includedTypes) {
                rmType = rmType.toUpperCase();
                if (!rmType.startsWith("DV_")) {
                    rmType = DATA_TYPES_PREFIX + rmType;
                }
                CComplexObject child =
                        CComplexObject.createSingleRequired(valuePath, rmType);
                cattr.addChild(child);
            }
            log.debug("value attribute added with total "
                    + cattr.getChildren().size() + "child(s)");
        }
    }

    /*
     * Returns next unique nodeId in "at0001" format
     */
    protected String formatNodeId(long count) {
        return AT + format.format(count);
    }

    /*
     * Gets next unique id and increase the counter
     */
    private String nextNodeId() {
        nodeCount++;
        //log.debug("next nodeId: " + nextId);
        return formatNodeId(nodeCount);
    }

    /*
     * Parse coded text like 'SNOMED-CT::258835005::mg/dygn'
     * the last segment of actual text value
     * is now collected in termMap as a quick fix
     *
     */
    private CodePhrase parseCodePhraseAndCollectText(String value, String path)
            throws FlatteningException {

        int index = value.indexOf("::");
        int index2 = value.lastIndexOf("::");
        if (index == index2) {
            throw new FlatteningException(
                    "wrong syntax for coded text: " + value);
        }
        String terminology = value.substring(0, index);
        String code = value.substring(index + 2, index2);
        String text = value.substring(index2 + 2);
        CodePhrase codePhrase = new CodePhrase(terminology, code);

        termMap.addTerm(terminology, code, text, path);
        return codePhrase;
    }

    private CString constrainString(String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        CString cstring = new CString(null, values, null);
        return cstring;
    }

    void replaceSlotWithFlattened(CAttribute cattr, Archetyped definition, CObject cobj) {

        List<CObject> children = cattr.getChildren();
        if (children == null) {
            return;
        }
        for (int index = 0, j = children.size(); index < j; index++) {
            CObject child = children.get(index);
            if (child instanceof ArchetypeSlot) {
                // if the slot matches the template def
                children.remove(index);
                cobj.setPath(child.path());
                children.add(index, cobj);
            }
        }
    }

    private TEMPLATE retrieveTemplate(String templateId)
            throws FlatteningException {
        TEMPLATE template = templateMap.get(templateId);
        if (template == null) {
            throw new UnknownTemplateException(templateId);
        }
        return template;
    }

    private Archetype retrieveArchetype(String archetypeId)
            throws FlatteningException {

        Archetype archetype = archetypeMap.get(archetypeId);
        if (archetype == null) {
            throw new UnknownArchetypeException(archetypeId);
        }
        // make a deep copy
        archetype = archetype.copy();
        return archetype;
    }

    protected int findLargestNodeId(Archetype archetype)
            throws FlatteningException {

        return findLargestNodeId(archetype.getDefinition(), 0);
    }

    // Traverse given cobj tree and find the largest node id
    protected int findLargestNodeId(CObject cobj, int largest)
            throws FlatteningException {
        int current = largest;
        int last = 0;

        current = parseNodeId(cobj.getNodeId());
        if (largest > current) {
            current = largest;
        }
        if (cobj instanceof CComplexObject) {
            CComplexObject ccobj = (CComplexObject) cobj;

            for (CAttribute cattr : ccobj.getAttributes()) {
                for (CObject child : cattr.getChildren()) {
                    last = findLargestNodeId(child, current);
                    if (last > current) {
                        current = last;
                    }
                }
            }
        }
        return current;
    }

    /*
     * Parses given nodeId and returns the integer value
     */
    protected int parseNodeId(String nodeId) throws FlatteningException {
        if (nodeId == null) {
            return 0;
        }
        if (nodeId.length() <= AT.length()) {
            throw new FlatteningException("Too short nodeId: " + nodeId);
        }
        nodeId = nodeId.substring(AT.length());
        int dot = nodeId.indexOf(".");
        if (dot > 0) {
            nodeId = nodeId.substring(0, dot);
        } else if (dot == 0) {
            throw new FlatteningException("Bad format of nodeId: " + nodeId);
        }
        int value = 0;
        try {
            value = Integer.parseInt(nodeId);
        } catch (NumberFormatException nfe) {
            throw new FlatteningException("Bad format of nodeId: " + nodeId);
        }
        return value;
    }

    protected ArchetypeConstraint nodeAtHybridPath(String path)
            throws FlatteningException {
        return null;
    }

    public TermMap getTermMap() {
        return termMap;
    }

    /* constant values */
    private static final String DV_TEXT = "DV_TEXT";
    private static final String DV_CODED_TEXT = "DV_CODED_TEXT";
    private static final String ELEMENT = "ELEMENT";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String ITEMS = "items";
    private static final String CONTENT = "content";
    private static final String DESCRIPTION = "description";
    private static final String DEFINING_CODE = "defining_code";
    private static final String AT = "at";
    private static final String DATA_TYPES_PREFIX = "DV_";

    /* static fields */
    private static DecimalFormat format = new DecimalFormat("####");

    static {
        // number of digits is not limited according to the specs
        // using 4 as minimum is by convention
        format.setMinimumIntegerDigits(4);
        format.setMaximumIntegerDigits(8);
    }

    private static Logger log = LoggerFactory.getLogger(TemplateFlattener.class);

    /* fields */
    private Map<String, Archetype> archetypeMap;
    private Map<String, TEMPLATE> templateMap;
    private TermMap termMap;
    private long nodeCount;
}
/*
 * ***** BEGIN LICENSE BLOCK ***** Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the 'License'); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is Flattener.java
 *
 * The Initial Developer of the Original Code is Rong Chen. Portions created by
 * the Initial Developer are Copyright (C) 2009-2010 the Initial Developer. All
 * Rights Reserved.
 *
 * Contributor(s):
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * ***** END LICENSE BLOCK *****
 */
