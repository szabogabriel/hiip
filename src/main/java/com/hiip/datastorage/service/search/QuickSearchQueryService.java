package com.hiip.datastorage.service.search;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.DataStorage;
import com.hiip.datastorage.service.search.QuickSearchExpression.And;
import com.hiip.datastorage.service.search.QuickSearchExpression.Condition;
import com.hiip.datastorage.service.search.QuickSearchExpression.Or;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Executes a parsed {@link QuickSearchExpression} against {@link DataStorage} entries of a given
 * category by translating quick-search labels to their corresponding {@code quickSearchN} columns
 * and building a parameterized JPQL query.
 */
@Service
public class QuickSearchQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Search data entries accessible by a user (owned, shared, or global), within a single category,
     * matching the given quick-search expression.
     *
     * @param category The category the quick-search labels belong to
     * @param expression The parsed filter expression
     * @param username The requesting user (for access filtering)
     * @return Matching data storage entries
     * @throws IllegalArgumentException if a label in the expression is not configured on the category
     */
    public List<DataStorage> search(Category category, QuickSearchExpression expression, String username) {
        Map<String, Object> params = new HashMap<>();
        String conditionFragment = buildFragment(expression, category, params, new AtomicInteger());

        String jpql = "SELECT DISTINCT d FROM DataStorage d " +
                "LEFT JOIN d.category c " +
                "LEFT JOIN c.sharedWith cs " +
                "WHERE d.category = :category " +
                "AND d.hidden = false " +
                "AND (d.owner = :username OR (cs.sharedWithUsername = :username AND cs.canRead = true) OR c.isGlobal = true) " +
                "AND (" + conditionFragment + ")";

        TypedQuery<DataStorage> query = entityManager.createQuery(jpql, DataStorage.class);
        query.setParameter("category", category);
        query.setParameter("username", username);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    private String buildFragment(QuickSearchExpression expression, Category category,
                                  Map<String, Object> params, AtomicInteger paramCounter) {
        if (expression instanceof Condition condition) {
            return buildConditionFragment(condition, category, params, paramCounter);
        }
        if (expression instanceof And and) {
            return and.operands().stream()
                    .map(operand -> buildFragment(operand, category, params, paramCounter))
                    .collect(Collectors.joining(" AND ", "(", ")"));
        }
        if (expression instanceof Or or) {
            return or.operands().stream()
                    .map(operand -> buildFragment(operand, category, params, paramCounter))
                    .collect(Collectors.joining(" OR ", "(", ")"));
        }
        throw new IllegalStateException("Unsupported quick-search expression: " + expression);
    }

    private String buildConditionFragment(Condition condition, Category category,
                                           Map<String, Object> params, AtomicInteger paramCounter) {
        // The column index comes only from the category's configured labels, never from raw user
        // input, so it's safe to interpolate the resulting field name directly into the JPQL.
        int columnIndex = resolveColumnIndex(category, condition.label());
        String field = "d.quickSearch" + (columnIndex + 1);
        String paramName = "p" + paramCounter.getAndIncrement();

        switch (condition.operator()) {
            case EQUALS:
                params.put(paramName, condition.values().get(0));
                return field + " = :" + paramName;
            case IN:
                params.put(paramName, condition.values());
                return field + " IN (:" + paramName + ")";
            default:
                throw new IllegalStateException("Unsupported quick-search operator: " + condition.operator());
        }
    }

    private int resolveColumnIndex(Category category, String label) {
        List<String> labels = category.getQuickSearchLabels();
        for (int i = 0; i < labels.size(); i++) {
            if (label.equalsIgnoreCase(labels.get(i))) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown quick-search label '" + label +
                "' for category '" + category.getPath() + "'");
    }
}
