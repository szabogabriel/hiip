package com.hiip.datastorage.service;

import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.CategoryShare;
import com.hiip.datastorage.repository.CategoryRepository;
import com.hiip.datastorage.repository.CategoryShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing categories and their hierarchical structure with user ownership and sharing
 */
@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryShareRepository categoryShareRepository;

    /**
     * Get or create a category by its path.
     * If the category doesn't exist, it will be created along with all parent categories.
     * 
     * @param categoryPath The complete category path (e.g., "electronics/computers/laptops")
     * @param createdBy The username of the user creating the category
     * @return The category entity
     */
    @Transactional
    public Category getOrCreateCategory(String categoryPath, String createdBy) {
        return getOrCreateCategory(categoryPath, createdBy, false);
    }

    /**
     * Find a category by its path without creating it.
     * 
     * @param categoryPath The complete category path
     * @return The category entity or null if not found
     */
    public Category findByPath(String categoryPath) {
        if (categoryPath == null || categoryPath.trim().isEmpty()) {
            return null;
        }

        String normalizedPath = normalizePath(categoryPath);
        if (normalizedPath.isEmpty()) {
            return null;
        }

        return categoryRepository.findByPath(normalizedPath).orElse(null);
    }

    /**
     * Get or create a category by its path with global flag.
     * 
     * @param categoryPath The complete category path
     * @param createdBy The username of the user creating the category
     * @param isGlobal Whether this should be a global category
     * @return The category entity
     */
    @Transactional
    public Category getOrCreateCategory(String categoryPath, String createdBy, boolean isGlobal) {
        if (categoryPath == null || categoryPath.trim().isEmpty()) {
            return null;
        }

        // Normalize the path: trim, remove leading/trailing slashes, remove duplicate slashes
        String normalizedPath = normalizePath(categoryPath);
        
        if (normalizedPath.isEmpty()) {
            return null;
        }

        // Check if category already exists
        Optional<Category> existingCategory = categoryRepository.findByPath(normalizedPath);
        if (existingCategory.isPresent()) {
            return existingCategory.get();
        }

        // Split the path into segments
        String[] segments = normalizedPath.split("/");
        Category parent = null;
        StringBuilder currentPath = new StringBuilder();

        // Create each level of the hierarchy if it doesn't exist
        for (String segment : segments) {
            if (segment.trim().isEmpty()) {
                continue;
            }

            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(segment.trim());

            String pathSoFar = currentPath.toString();
            Optional<Category> categoryAtThisLevel = categoryRepository.findByPath(pathSoFar);

            if (categoryAtThisLevel.isPresent()) {
                parent = categoryAtThisLevel.get();
            } else {
                // Create new category at this level
                Category newCategory = new Category(segment.trim(), pathSoFar, parent, createdBy, isGlobal);
                newCategory = categoryRepository.save(newCategory);
                logger.info("Created new category: {} with path: {} for user: {} (global: {})", 
                           segment, pathSoFar, createdBy, isGlobal);
                parent = newCategory;
            }
        }

        return parent;
    }

    /**
     * Get all categories in a flat list
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Get all root categories (categories without parents)
     */
    public List<Category> getRootCategories() {
        return categoryRepository.findRootCategories();
    }

    /**
     * Get a category by its path
     */
    public Optional<Category> getCategoryByPath(String path) {
        String normalizedPath = normalizePath(path);
        return categoryRepository.findByPath(normalizedPath);
    }

    /**
     * Get all categories that start with a given path prefix
     */
    public List<Category> getCategoriesByPathPrefix(String pathPrefix) {
        String normalizedPrefix = normalizePath(pathPrefix);
        return categoryRepository.findByPathStartingWith(normalizedPrefix);
    }

    /**
     * Get direct children of a category
     */
    public List<Category> getChildCategories(Category parent) {
        return categoryRepository.findByParentOrderByName(parent);
    }

    /**
     * Delete a category (will fail if it has children due to constraints)
     */
    @Transactional
    public boolean deleteCategory(Long id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            categoryRepository.delete(category.get());
            return true;
        }
        return false;
    }

    /**
     * Normalize a category path by:
     * - Trimming whitespace
     * - Removing leading and trailing slashes
     * - Replacing multiple consecutive slashes with a single slash
     * - Trimming whitespace from each segment
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        // Trim and remove leading/trailing slashes
        String normalized = path.trim().replaceAll("^/+|/+$", "");
        
        // Replace multiple slashes with single slash
        normalized = normalized.replaceAll("/+", "/");
        
        // Trim each segment
        String[] segments = normalized.split("/");
        List<String> trimmedSegments = new ArrayList<>();
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                trimmedSegments.add(trimmed);
            }
        }
        
        return String.join("/", trimmedSegments);
    }

    /**
     * Check if a category path exists
     */
    public boolean categoryExists(String path) {
        String normalizedPath = normalizePath(path);
        return categoryRepository.existsByPath(normalizedPath);
    }

    /**
     * Get categories accessible by a specific user
     * (global categories, owned categories, or shared categories)
     */
    public List<Category> getCategoriesAccessibleByUser(String username) {
        return categoryRepository.findAccessibleByUser(username);
    }

    /**
     * Get categories where user has write permission
     */
    public List<Category> getCategoriesWritableByUser(String username) {
        return categoryRepository.findWritableByUser(username);
    }

    /**
     * Share a category with a user
     */
    @Transactional
    public CategoryShare shareCategory(Long categoryId, String sharedWithUsername, boolean canRead, boolean canWrite) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        if (!category.isPresent()) {
            throw new IllegalArgumentException("Category not found");
        }

        // Check if already shared
        Optional<CategoryShare> existing = categoryShareRepository
                .findByCategoryAndSharedWithUsername(category.get(), sharedWithUsername);
        
        if (existing.isPresent()) {
            // Update existing share
            CategoryShare share = existing.get();
            share.setCanRead(canRead);
            share.setCanWrite(canWrite);
            return categoryShareRepository.save(share);
        } else {
            // Create new share
            CategoryShare share = new CategoryShare(category.get(), sharedWithUsername, canRead, canWrite);
            return categoryShareRepository.save(share);
        }
    }

    /**
     * Remove sharing for a user
     */
    @Transactional
    public void unshareCategory(Long categoryId, String username) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        if (category.isPresent()) {
            categoryShareRepository.deleteByCategoryAndSharedWithUsername(category.get(), username);
        }
    }

    /**
     * Get all shares for a category
     */
    public List<CategoryShare> getCategoryShares(Long categoryId) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        return category.map(categoryShareRepository::findByCategory).orElse(new ArrayList<>());
    }

    /**
     * Check if user can access a category
     */
    public boolean canUserAccessCategory(Long categoryId, String username) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        return category.map(c -> c.isAccessibleBy(username)).orElse(false);
    }

    /**
     * Check if user has write permission on a category
     */
    public boolean canUserWriteCategory(Long categoryId, String username) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        return category.map(c -> c.hasWritePermission(username)).orElse(false);
    }
}
