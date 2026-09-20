package com.hiip.datastorage.service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiip.datastorage.dto.DataStorageResponse;
import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.DataStorage;
import com.hiip.datastorage.service.CategoryService;
import com.hiip.datastorage.service.DataStorageService;
import com.hiip.datastorage.service.search.QuickSearchQueryService;

@ExtendWith(MockitoExtension.class)
class DataStorageFacadeServiceTest {

    @Mock
    private DataStorageService dataStorageService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private QuickSearchQueryService quickSearchQueryService;

    @InjectMocks
    private DataStorageFacadeService dataStorageFacadeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void searchByQuickSearch_blankCategory_throwsWithoutLookup() {
        assertThatThrownBy(() -> dataStorageFacadeService.searchByQuickSearch(" ", "status EQUALS \"shipped\"", "alice"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(categoryService, quickSearchQueryService);
    }

    @Test
    void searchByQuickSearch_categoryNotFound_throws() {
        when(categoryService.findByPath("orders")).thenReturn(null);

        assertThatThrownBy(() -> dataStorageFacadeService.searchByQuickSearch("orders", "status EQUALS \"shipped\"", "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orders");
        verifyNoInteractions(quickSearchQueryService);
    }

    @Test
    void searchByQuickSearch_delegatesToQueryServiceAndMapsResults() throws Exception {
        Category category = new Category("orders", "orders", null, "alice", false);
        category.setQuickSearchPaths(List.of("$.status"));
        category.setQuickSearchLabels(List.of("status"));
        when(categoryService.findByPath("orders")).thenReturn(category);

        DataStorage stored = new DataStorage(objectMapper.readTree("{\"status\": \"shipped\"}"), null, "alice", category);
        when(quickSearchQueryService.search(eq(category), any(), eq("alice"))).thenReturn(List.of(stored));

        List<DataStorageResponse> results = dataStorageFacadeService.searchByQuickSearch(
                "orders", "status EQUALS \"shipped\"", "alice");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategory()).isEqualTo("orders");
    }

    @Test
    void searchByQuickSearch_malformedFilter_throwsWithoutQuerying() {
        Category category = new Category("orders", "orders", null, "alice", false);
        when(categoryService.findByPath("orders")).thenReturn(category);

        assertThatThrownBy(() -> dataStorageFacadeService.searchByQuickSearch("orders", "status \"shipped\"", "alice"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(quickSearchQueryService);
    }
}
