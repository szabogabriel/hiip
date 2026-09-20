package com.hiip.datastorage.service.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.hiip.datastorage.service.search.QuickSearchExpression.And;
import com.hiip.datastorage.service.search.QuickSearchExpression.Condition;
import com.hiip.datastorage.service.search.QuickSearchExpression.Operator;
import com.hiip.datastorage.service.search.QuickSearchExpression.Or;

class QuickSearchFilterParserTest {

    @Test
    void parsesEqualsCondition() {
        QuickSearchExpression expression = QuickSearchFilterParser.parse("status EQUALS \"shipped\"");

        assertThat(expression).isEqualTo(new Condition("status", Operator.EQUALS, java.util.List.of("shipped")));
    }

    @Test
    void parsesInCondition() {
        QuickSearchExpression expression = QuickSearchFilterParser.parse("status IN [\"shipped\", \"cancelled\"]");

        assertThat(expression).isEqualTo(new Condition("status", Operator.IN, java.util.List.of("shipped", "cancelled")));
    }

    @Test
    void keywordsAreCaseInsensitive() {
        QuickSearchExpression expression = QuickSearchFilterParser.parse("status in ['shipped']");

        assertThat(expression).isEqualTo(new Condition("status", Operator.IN, java.util.List.of("shipped")));
    }

    @Test
    void parsesAndConjunction() {
        QuickSearchExpression expression = QuickSearchFilterParser.parse("a EQUALS \"1\" AND b EQUALS \"2\"");

        assertThat(expression).isEqualTo(new And(java.util.List.of(
                new Condition("a", Operator.EQUALS, java.util.List.of("1")),
                new Condition("b", Operator.EQUALS, java.util.List.of("2")))));
    }

    @Test
    void parsesOrDisjunction() {
        QuickSearchExpression expression = QuickSearchFilterParser.parse("a EQUALS \"1\" OR b EQUALS \"2\"");

        assertThat(expression).isEqualTo(new Or(java.util.List.of(
                new Condition("a", Operator.EQUALS, java.util.List.of("1")),
                new Condition("b", Operator.EQUALS, java.util.List.of("2")))));
    }

    @Test
    void andBindsTighterThanOr() {
        QuickSearchExpression expression = QuickSearchFilterParser.parse(
                "a EQUALS \"1\" AND b EQUALS \"2\" OR c EQUALS \"3\"");

        QuickSearchExpression expected = new Or(java.util.List.of(
                new And(java.util.List.of(
                        new Condition("a", Operator.EQUALS, java.util.List.of("1")),
                        new Condition("b", Operator.EQUALS, java.util.List.of("2")))),
                new Condition("c", Operator.EQUALS, java.util.List.of("3"))));

        assertThat(expression).isEqualTo(expected);
    }

    @Test
    void parenthesesOverridePrecedence() {
        QuickSearchExpression expression = QuickSearchFilterParser.parse(
                "(a EQUALS \"1\" OR b EQUALS \"2\") AND c EQUALS \"3\"");

        QuickSearchExpression expected = new And(java.util.List.of(
                new Or(java.util.List.of(
                        new Condition("a", Operator.EQUALS, java.util.List.of("1")),
                        new Condition("b", Operator.EQUALS, java.util.List.of("2")))),
                new Condition("c", Operator.EQUALS, java.util.List.of("3"))));

        assertThat(expression).isEqualTo(expected);
    }

    @Test
    void blankFilterThrows() {
        assertThatThrownBy(() -> QuickSearchFilterParser.parse("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingOperatorThrows() {
        assertThatThrownBy(() -> QuickSearchFilterParser.parse("status \"shipped\""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unterminatedInListThrows() {
        assertThatThrownBy(() -> QuickSearchFilterParser.parse("status IN [\"shipped\""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void trailingGarbageThrows() {
        assertThatThrownBy(() -> QuickSearchFilterParser.parse("status EQUALS \"shipped\" garbage"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidCharacterThrows() {
        assertThatThrownBy(() -> QuickSearchFilterParser.parse("status EQUALS #shipped"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
