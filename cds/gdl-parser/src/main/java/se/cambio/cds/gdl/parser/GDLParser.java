package se.cambio.cds.gdl.parser;

import org.openehr.am.parser.*;
import se.cambio.cds.gdl.model.ArchetypeBinding;
import se.cambio.cds.gdl.model.Guide;
import se.cambio.cds.gdl.model.GuideDefinition;
import se.cambio.cds.gdl.model.Rule;
import se.cambio.cds.gdl.model.expression.AssignmentExpression;
import se.cambio.cds.gdl.model.expression.ExpressionItem;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GDLParser {

    public Guide parse(InputStream input) {
        DADLParser parser = new DADLParser(input, "UTF-8");
        try {
            ContentObject content = parser.parse();
            GDLBinding binding = new GDLBinding();

            Object obj = binding.bind(content);
            Guide guide = (Guide) obj;
            bindExpressions(guide);
            return guide;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public Guide parse(Reader input) throws Exception {
        DADLParser parser = new DADLParser(input);
        ContentObject content = parser.parse();
        GDLBinding binding = new GDLBinding();
        Object obj = binding.bind(content);
        Guide guide = (Guide) obj;
        bindExpressions(guide);
        return guide;
    }

    /*
     * List of expressions in GDL 1. guide definition pre-conditions 2.
     * archetype binding predicates 3. rule when statements 4. rule then
     * statements
     */
    private void bindExpressions(Guide guide) throws Exception {
        List<String> preConditions = guide.getDefinition().getPreConditions();
        List<ExpressionItem> preConditionExpressions = parseExpressions(preConditions);
        guide.getDefinition().setPreConditionExpressions(preConditionExpressions);

        List<String> defaultActions = guide.getDefinition().getDefaultActions();
        List<ExpressionItem> expressionItems = parseExpressions(defaultActions);
        List<AssignmentExpression> defaultActionExpressions = toAssignments(expressionItems);
        guide.getDefinition().setDefaultActionExpressions(defaultActionExpressions);

        GuideDefinition definition = guide.getDefinition();
        if (definition.getArchetypeBindings() != null) {
            Map<String, ArchetypeBinding> bindings = definition
                    .getArchetypeBindings();
            for (ArchetypeBinding binding : bindings.values()) {
                List<ExpressionItem> predicateStatements = parseExpressions(binding.getPredicates());
                binding.setPredicateStatements(predicateStatements);
            }
            if (definition.getRules() != null) {
                Collection<Rule> rules = definition.getRules().values();
                for (Rule rule : rules) {
                    List<ExpressionItem> whenStatements = parseExpressions(rule.getWhen());
                    rule.setWhenStatements(whenStatements);
                    List<ExpressionItem> thenExpressionItems = parseExpressions(rule.getThen());
                    List<AssignmentExpression> thenStatements = toAssignments(thenExpressionItems);
                    rule.setThenStatements(thenStatements);
                }
            }
        }
    }

    private List<AssignmentExpression> toAssignments(List<ExpressionItem> items) {
        List<AssignmentExpression> ret = new ArrayList<>();
        if (items != null) {
            for (ExpressionItem item : items) {
                ret.add((AssignmentExpression) item);
            }
        }
        return ret;
    }

    private List<ExpressionItem> parseExpressions(List<String> lines)
            throws Exception {
        if (lines == null) {
            return null;
        }
        List<ExpressionItem> items = new ArrayList<>();
        for (String line : lines) {
            items.add(Expressions.parse(line));
        }
        return items;
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