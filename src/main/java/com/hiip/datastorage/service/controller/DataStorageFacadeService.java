package com.hiip.datastorage.service.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hiip.datastorage.dto.DataStorageRequest;
import com.hiip.datastorage.dto.DataStorageResponse;
import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.DataStorage;
import com.hiip.datastorage.service.CategoryService;
import com.hiip.datastorage.service.DataStorageService;
import com.hiip.datastorage.service.search.QuickSearchExpression;
import com.hiip.datastorage.service.search.QuickSearchFilterParser;
import com.hiip.datastorage.service.search.QuickSearchQueryService;

/**
 * Facade service for data storage operations.
 * This service acts as a coordinator between the controller and business services,
 * providing a clean interface for data storage operations and handling the conversion
 * between DTOs and entities.
 */
@Service
public class DataStorageFacadeService {

    private static final Logger logger = LoggerFactory.getLogger(DataStorageFacadeService.class);

    @Autowired
    private DataStorageService dataStorageService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private QuickSearchQueryService quickSearchQueryService;

    /**
     * Create new data storage entry.
     * 
     * @param request the data storage request
     * @param owner the owner username
     * @return the created data storage response
     */
    public DataStorageResponse createData(DataStorageRequest request, String owner) {
        logger.info("Creating data for owner: {}", owner);
        logger.info("Content: {}", request.getContent());
        logger.info("Tags: {}", request.getTags());
        logger.info("Tags size: {}", request.getTags() != null ? request.getTags().size() : "null");
        logger.info("Category: {}", request.getCategory());
        
        // Get or create category if provided
        Category category = null;
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            category = categoryService.getOrCreateCategory(request.getCategory(), owner);
            logger.info("Category resolved: {}", category != null ? category.getPath() : "null");
        }
        
        // Check if content is null, empty, or just an empty JSON object
        if (request.getContent() == null || 
            request.getContent().isNull() || 
            request.getContent().isEmpty()) {
            logger.warn("Content is empty, cannot create data");
            throw new IllegalArgumentException("Content cannot be empty");
        }

        // Validate content against the category's JSON schema, if one is defined
        categoryService.validateContentAgainstSchema(category, request.getContent());

        DataStorage dataStorage = new DataStorage(request.getContent(), request.getTags(), owner, category);
        dataStorage.setQuickSearchValues(categoryService.extractQuickSearchValues(category, request.getContent()));
        DataStorage saved = dataStorageService.createData(dataStorage);
        
        logger.info("Saved data with ID: {}, tags: {}, category: {}", 
                saved.getId(), saved.getTags(), saved.getCategory() != null ? saved.getCategory().getPath() : "null");
        
        return new DataStorageResponse(saved);
    }

    /**
     * Get data storage entry by ID.
     * 
     * @param id the data storage ID
     * @param owner the owner username
     * @return optional containing the data storage response if found
     */
    public Optional<DataStorageResponse> getDataById(Long id, String owner) {
        logger.debug("Retrieving data with ID: {} for owner: {}", id, owner);
        
        return dataStorageService.getDataById(id, owner)
                .map(DataStorageResponse::new);
    }

    /**
     * Get all data storage entries for an owner.
     * 
     * @param owner the owner username
     * @return list of data storage responses
     */
    public List<DataStorageResponse> getAllData(String owner) {
        logger.debug("Retrieving all data for owner: {}", owner);
        
        return dataStorageService.getAllData(owner)
                .stream()
                .map(DataStorageResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Search data storage entries by tags.
     * 
     * @param tags the list of tags to search for
     * @param owner the owner username
     * @return list of data storage responses matching the tags
     */
    public List<DataStorageResponse> searchByTags(List<String> tags, String owner) {
        logger.debug("Searching data by tags: {} for owner: {}", tags, owner);
        
        return dataStorageService.searchByTags(tags, owner)
                .stream()
                .map(DataStorageResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Search data storage entries by tags and/or category.
     * Both parameters are optional.
     * Category supports wildcard patterns using '*' (equivalent to SQL '%').
     * 
     * @param tags the list of tags to search for (optional)
     * @param categoryPath the category path to filter by (optional, supports wildcards like "work/*" or "*project*")
     * @param owner the owner username
     * @return list of data storage responses matching the criteria
     */
    public List<DataStorageResponse> searchData(List<String> tags, String categoryPath, String owner) {
        logger.debug("Searching data with tags: {} and category: {} for owner: {}", tags, categoryPath, owner);
        
        // Check if category pattern contains wildcards
        boolean hasWildcard = categoryPath != null && categoryPath.contains("*");
        
        if (hasWildcard) {
            // Use pattern matching search for wildcards
            logger.debug("Wildcard pattern detected in category: {}", categoryPath);
            return dataStorageService.searchDataByPattern(tags, categoryPath, owner)
                    .stream()
                    .map(DataStorageResponse::new)
                    .collect(Collectors.toList());
        } else {
            // Resolve category if provided (exact match)
            Category category = null;
            if (categoryPath != null && !categoryPath.trim().isEmpty()) {
                // Try to find existing category, don't create new one for search
                category = categoryService.findByPath(categoryPath);
                if (category == null) {
                    logger.debug("Category not found: {}, returning empty results", categoryPath);
                    return List.of(); // Category doesn't exist, no results
                }
            }
            
            return dataStorageService.searchData(tags, category, owner)
                    .stream()
                    .map(DataStorageResponse::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Update data storage entry.
     * 
     * @param id the data storage ID
     * @param request the data storage request with updated data
     * @param owner the owner username
     * @return optional containing the updated data storage response if found
     */
    public Optional<DataStorageResponse> updateData(Long id, DataStorageRequest request, String owner) {
        logger.info("Updating data with ID: {} for owner: {}", id, owner);
        logger.info("New content: {}", request.getContent());
        logger.info("New tags: {}", request.getTags());
        logger.info("New category: {}", request.getCategory());
        
        // Get or create category if provided
        Category category = null;
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            category = categoryService.getOrCreateCategory(request.getCategory(), owner);
            logger.info("Category resolved: {}", category != null ? category.getPath() : "null");
        }

        // Validate content against the category's JSON schema, if one is defined
        categoryService.validateContentAgainstSchema(category, request.getContent());

        DataStorage updatedData = new DataStorage(request.getContent(), request.getTags(), owner, category);
        updatedData.setQuickSearchValues(categoryService.extractQuickSearchValues(category, request.getContent()));
        
        return dataStorageService.updateData(id, updatedData, owner)
                .map(data -> {
                    logger.info("Successfully updated data with ID: {}", id);
                    return new DataStorageResponse(data);
                });
    }

    /**
     * Delete (hide) data storage entry.
     * 
     * @param id the data storage ID
     * @param owner the owner username
     * @return true if the data was deleted, false otherwise
     */
    public boolean deleteData(Long id, String owner) {
        logger.info("Deleting data with ID: {} for owner: {}", id, owner);
        
        boolean deleted = dataStorageService.hideData(id, owner);
        
        if (deleted) {
            logger.info("Successfully deleted data with ID: {}", id);
        } else {
            logger.warn("Failed to delete data with ID: {} - not found or access denied", id);
        }
        
        return deleted;
    }

    /**
     * Get all data storage entries accessible by the user (owned + shared via categories + global).
     * 
     * @param username the username
     * @return list of accessible data storage responses
     */
    public List<DataStorageResponse> getAllAccessibleData(String username) {
        logger.debug("Retrieving all accessible data for user: {}", username);
        
        return dataStorageService.getAllAccessibleData(username)
                .stream()
                .map(DataStorageResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Search data storage entries accessible by user (owned + shared) by tags and/or category.
     * Both parameters are optional.
     * Category supports wildcard patterns using '*' (equivalent to SQL '%').
     * 
     * @param tags the list of tags to search for (optional)
     * @param categoryPath the category path to filter by (optional, supports wildcards like "work/*" or "*project*")
     * @param username the username
     * @return list of data storage responses matching the criteria
     */
    public List<DataStorageResponse> searchAccessibleData(List<String> tags, String categoryPath, String username) {
        logger.debug("Searching accessible data with tags: {} and category: {} for user: {}", tags, categoryPath, username);
        
        // Check if category pattern contains wildcards
        boolean hasWildcard = categoryPath != null && categoryPath.contains("*");
        
        if (hasWildcard) {
            // Use pattern matching search for wildcards
            logger.debug("Wildcard pattern detected in category: {}", categoryPath);
            return dataStorageService.searchAccessibleDataByPattern(tags, categoryPath, username)
                    .stream()
                    .map(DataStorageResponse::new)
                    .collect(Collectors.toList());
        } else {
            // Resolve category if provided (exact match)
            Category category = null;
            if (categoryPath != null && !categoryPath.trim().isEmpty()) {
                // Try to find existing category, don't create new one for search
                category = categoryService.findByPath(categoryPath);
                if (category == null) {
                    logger.debug("Category not found: {}, returning empty results", categoryPath);
                    return List.of(); // Category doesn't exist, no results
                }
            }
            
            return dataStorageService.searchAccessibleData(tags, category, username)
                    .stream()
                    .map(DataStorageResponse::new)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Search data entries accessible by a user (owned + shared + global), within a single category,
     * using a pseudo-SQL filter over that category's quick-search labels, e.g.
     * {@code status IN ["shipped", "cancelled"] AND total EQUALS "42.5"}.
     *
     * @param categoryPath the category whose quick-search labels the filter refers to
     * @param filter the quick-search filter expression
     * @param username the requesting user
     * @return list of data storage responses matching the filter
     * @throws IllegalArgumentException if the category doesn't exist, the filter is malformed,
     *         or it references a label that isn't configured on the category
     */
    public List<DataStorageResponse> searchByQuickSearch(String categoryPath, String filter, String username) {
        logger.debug("Quick-search filtering category: {} with filter: {} for user: {}", categoryPath, filter, username);

        if (categoryPath == null || categoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Category is required for quick-search filtering");
        }

        Category category = categoryService.findByPath(categoryPath);
        if (category == null) {
            throw new IllegalArgumentException("Category not found: " + categoryPath);
        }

        QuickSearchExpression expression = QuickSearchFilterParser.parse(filter);

        return quickSearchQueryService.search(category, expression, username)
                .stream()
                .map(DataStorageResponse::new)
                .collect(Collectors.toList());
    }
}
