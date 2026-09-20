package com.hiip.datastorage.service.search;

import java.util.List;

/**
 * AST for a parsed quick-search filter expression (see {@link QuickSearchFilterParser}).
 */
public sealed interface QuickSearchExpression {

    enum Operator {
        EQUALS,
        IN
    }

    /**
     * A single "label OP value(s)" condition, e.g. {@code status IN ["shipped", "cancelled"]}.
     */
    record Condition(String label, Operator operator, List<String> values) implements QuickSearchExpression {
    }

    /**
     * Conjunction of two or more sub-expressions.
     */
    record And(List<QuickSearchExpression> operands) implements QuickSearchExpression {
    }

    /**
     * Disjunction of two or more sub-expressions.
     */
    record Or(List<QuickSearchExpression> operands) implements QuickSearchExpression {
    }
}
