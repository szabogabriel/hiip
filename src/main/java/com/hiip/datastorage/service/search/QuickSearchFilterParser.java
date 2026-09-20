package com.hiip.datastorage.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hiip.datastorage.service.search.QuickSearchExpression.And;
import com.hiip.datastorage.service.search.QuickSearchExpression.Condition;
import com.hiip.datastorage.service.search.QuickSearchExpression.Operator;
import com.hiip.datastorage.service.search.QuickSearchExpression.Or;

/**
 * Parses a pseudo-SQL quick-search filter, e.g.:
 * <pre>status IN ["shipped", "cancelled"] AND total EQUALS "42.5"</pre>
 * into a {@link QuickSearchExpression} tree. Grammar:
 * <pre>
 * orExpr    := andExpr (OR andExpr)*
 * andExpr   := primary (AND primary)*
 * primary   := '(' orExpr ')' | condition
 * condition := LABEL (EQUALS STRING | IN '[' STRING (',' STRING)* ']')
 * </pre>
 * Labels refer to the quick-search labels configured on a category's JSON schema
 * and must be a single word (letters, digits, underscore).
 */
public final class QuickSearchFilterParser {

    private enum TokenType { STRING, LBRACKET, RBRACKET, LPAREN, RPAREN, COMMA, AND, OR, IN, EQUALS, IDENT }

    private record Token(TokenType type, String text) {
    }

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "\\s*(?:(\"[^\"]*\"|'[^']*')|(\\[)|(\\])|(\\()|(\\))|(,)|([A-Za-z_][A-Za-z0-9_]*))");

    private final List<Token> tokens;
    private int pos = 0;

    private QuickSearchFilterParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static QuickSearchExpression parse(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            throw new IllegalArgumentException("Quick-search filter cannot be empty");
        }

        QuickSearchFilterParser parser = new QuickSearchFilterParser(tokenize(filter));
        QuickSearchExpression expression = parser.parseOr();
        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException("Unexpected token '" + parser.peek().text() + "' in quick-search filter");
        }
        return expression;
    }

    private static List<Token> tokenize(String filter) {
        List<Token> result = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(filter);
        int lastEnd = 0;
        while (matcher.lookingAt()) {
            if (matcher.group(1) != null) {
                String raw = matcher.group(1);
                result.add(new Token(TokenType.STRING, raw.substring(1, raw.length() - 1)));
            } else if (matcher.group(2) != null) {
                result.add(new Token(TokenType.LBRACKET, "["));
            } else if (matcher.group(3) != null) {
                result.add(new Token(TokenType.RBRACKET, "]"));
            } else if (matcher.group(4) != null) {
                result.add(new Token(TokenType.LPAREN, "("));
            } else if (matcher.group(5) != null) {
                result.add(new Token(TokenType.RPAREN, ")"));
            } else if (matcher.group(6) != null) {
                result.add(new Token(TokenType.COMMA, ","));
            } else if (matcher.group(7) != null) {
                String word = matcher.group(7);
                result.add(new Token(keywordType(word), word));
            }
            lastEnd = matcher.end();
            matcher.region(lastEnd, filter.length());
        }
        if (!filter.substring(lastEnd).isBlank()) {
            throw new IllegalArgumentException("Invalid quick-search filter near: '" + filter.substring(lastEnd).trim() + "'");
        }
        return result;
    }

    private static TokenType keywordType(String word) {
        return switch (word.toUpperCase()) {
            case "AND" -> TokenType.AND;
            case "OR" -> TokenType.OR;
            case "IN" -> TokenType.IN;
            case "EQUALS" -> TokenType.EQUALS;
            default -> TokenType.IDENT;
        };
    }

    private QuickSearchExpression parseOr() {
        List<QuickSearchExpression> operands = new ArrayList<>();
        operands.add(parseAnd());
        while (check(TokenType.OR)) {
            advance();
            operands.add(parseAnd());
        }
        return operands.size() == 1 ? operands.get(0) : new Or(operands);
    }

    private QuickSearchExpression parseAnd() {
        List<QuickSearchExpression> operands = new ArrayList<>();
        operands.add(parsePrimary());
        while (check(TokenType.AND)) {
            advance();
            operands.add(parsePrimary());
        }
        return operands.size() == 1 ? operands.get(0) : new And(operands);
    }

    private QuickSearchExpression parsePrimary() {
        if (check(TokenType.LPAREN)) {
            advance();
            QuickSearchExpression expression = parseOr();
            expect(TokenType.RPAREN, "')'");
            return expression;
        }
        return parseCondition();
    }

    private QuickSearchExpression parseCondition() {
        Token label = expect(TokenType.IDENT, "a quick-search label");

        if (check(TokenType.EQUALS)) {
            advance();
            Token value = expect(TokenType.STRING, "a quoted value");
            return new Condition(label.text(), Operator.EQUALS, List.of(value.text()));
        }

        if (check(TokenType.IN)) {
            advance();
            expect(TokenType.LBRACKET, "'['");
            List<String> values = new ArrayList<>();
            values.add(expect(TokenType.STRING, "a quoted value").text());
            while (check(TokenType.COMMA)) {
                advance();
                values.add(expect(TokenType.STRING, "a quoted value").text());
            }
            expect(TokenType.RBRACKET, "']'");
            return new Condition(label.text(), Operator.IN, values);
        }

        throw new IllegalArgumentException("Expected EQUALS or IN after '" + label.text() + "'");
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type() == type;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private boolean isAtEnd() {
        return pos >= tokens.size();
    }

    private Token expect(TokenType type, String description) {
        if (!check(type)) {
            throw new IllegalArgumentException("Expected " + description + " in quick-search filter" +
                    (isAtEnd() ? " but reached the end of input" : ", found '" + peek().text() + "'"));
        }
        return advance();
    }
}
