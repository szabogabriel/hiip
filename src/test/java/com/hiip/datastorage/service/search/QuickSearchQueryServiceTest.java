package com.hiip.datastorage.service.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.DataStorage;
import com.hiip.datastorage.service.search.QuickSearchExpression.And;
import com.hiip.datastorage.service.search.QuickSearchExpression.Condition;
import com.hiip.datastorage.service.search.QuickSearchExpression.Operator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@ExtendWith(MockitoExtension.class)
class QuickSearchQueryServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<DataStorage> typedQuery;

    @InjectMocks
    private QuickSearchQueryService quickSearchQueryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("orders", "orders", null, "alice", false);
        category.setQuickSearchPaths(List.of("$.status", "$.total"));
        category.setQuickSearchLabels(List.of("status", "total"));
    }

    @Test
    void equalsConditionResolvesToFirstColumnAndBindsParameter() {
        when(entityManager.createQuery(anyString(), eq(DataStorage.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());

        Condition expression = new Condition("status", Operator.EQUALS, List.of("shipped"));
        quickSearchQueryService.search(category, expression, "alice");

        ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createQuery(jpqlCaptor.capture(), eq(DataStorage.class));
        assertThat(jpqlCaptor.getValue()).contains("d.quickSearch1 = :p0");
        verify(typedQuery).setParameter("p0", "shipped");
        verify(typedQuery).setParameter("category", category);
        verify(typedQuery).setParameter("username", "alice");
    }

    @Test
    void inConditionResolvesToSecondColumnAndBindsListParameter() {
        when(entityManager.createQuery(anyString(), eq(DataStorage.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());

        Condition expression = new Condition("total", Operator.IN, List.of("10", "20"));
        quickSearchQueryService.search(category, expression, "alice");

        ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createQuery(jpqlCaptor.capture(), eq(DataStorage.class));
        assertThat(jpqlCaptor.getValue()).contains("d.quickSearch2 IN (:p0)");
        verify(typedQuery).setParameter("p0", List.of("10", "20"));
    }

    @Test
    void labelMatchingIsCaseInsensitive() {
        when(entityManager.createQuery(anyString(), eq(DataStorage.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());

        Condition expression = new Condition("STATUS", Operator.EQUALS, List.of("shipped"));
        quickSearchQueryService.search(category, expression, "alice");

        ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createQuery(jpqlCaptor.capture(), eq(DataStorage.class));
        assertThat(jpqlCaptor.getValue()).contains("d.quickSearch1 = :p0");
    }

    @Test
    void unknownLabelThrowsWithoutQueryingTheDatabase() {
        Condition expression = new Condition("unknown", Operator.EQUALS, List.of("x"));

        assertThatThrownBy(() -> quickSearchQueryService.search(category, expression, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
        verifyNoInteractions(entityManager);
    }

    @Test
    void andCombinesFragmentsWithAndKeyword() {
        when(entityManager.createQuery(anyString(), eq(DataStorage.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(List.of());

        And expression = new And(List.of(
                new Condition("status", Operator.EQUALS, List.of("shipped")),
                new Condition("total", Operator.EQUALS, List.of("42"))));
        quickSearchQueryService.search(category, expression, "alice");

        ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createQuery(jpqlCaptor.capture(), eq(DataStorage.class));
        String jpql = jpqlCaptor.getValue();
        assertThat(jpql).contains("d.quickSearch1 = :p0").contains("d.quickSearch2 = :p1").contains(" AND ");
    }

    @Test
    void searchReturnsResultsFromTheQuery() {
        when(entityManager.createQuery(anyString(), eq(DataStorage.class))).thenReturn(typedQuery);
        DataStorage match = new DataStorage();
        when(typedQuery.getResultList()).thenReturn(List.of(match));

        Condition expression = new Condition("status", Operator.EQUALS, List.of("shipped"));
        List<DataStorage> results = quickSearchQueryService.search(category, expression, "alice");

        assertThat(results).containsExactly(match);
    }
}
