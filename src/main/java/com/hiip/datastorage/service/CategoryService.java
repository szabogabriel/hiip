package com.hiip.datastorage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.CategoryShare;
import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.repository.CategoryRepository;
import com.hiip.datastorage.repository.CategoryShareRepository;
import com.hiip.datastorage.repository.UserRepository;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing categories and their hierarchical structure with user ownership and sharing
 */
@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private static final JsonSchemaFactory SCHEMA_FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    /**
     * Top-level schema keyword holding the list of quick-search mappings (up to
     * {@link Category#QUICK_SEARCH_COLUMN_COUNT}), each either a plain JSON Path string or an
     * object of the form {@code {"path": "$.foo", "label": "Foo"}}. Unknown keywords like this
     * are silently ignored during schema validation.
     */
    private static final String QUICK_SEARCH_SCHEMA_KEY = "x-quick-search";

    /**
     * Quick-search labels must be a single word so they can be referenced unambiguously in
     * quick-search filter expressions (see {@code QuickSearchFilterParser}).
     */
    private static final Pattern QUICK_SEARCH_LABEL_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private static final Configuration JSON_PATH_CONFIG = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider())
            .options(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryShareRepository categoryShareRepository;

    @Autowired
    private UserRepository userRepository;

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
        Optional<Category> existingCategory = categoryRepository.findByPathLike(normalizedPath);
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
            Optional<Category> categoryAtThisLevel = categoryRepository.findByPathLike(pathSoFar);

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
        return categoryRepository.findByPathLike(normalizedPath);
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

        // Replace '*' with '%' for wildcard support
        normalized = normalized.replace('*', '%');
        
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
     * Share a category with a user (by username or email)
     * This method accepts either a username or email and resolves it to the username for storage.
     * 
     * @param categoryId The ID of the category to share
     * @param usernameOrEmail The username or email of the user to share with
     * @param canRead Whether the user can read the category
     * @param canWrite Whether the user can write to the category
     * @return The created or updated CategoryShare
     * @throws IllegalArgumentException if category or user not found
     */
    @Transactional
    public CategoryShare shareCategory(Long categoryId, String usernameOrEmail, boolean canRead, boolean canWrite) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        if (!category.isPresent()) {
            throw new IllegalArgumentException("Category not found");
        }

        // Resolve username from either username or email
        String username = resolveUsername(usernameOrEmail);

        // Check if already shared
        Optional<CategoryShare> existing = categoryShareRepository
                .findByCategoryAndSharedWithUsername(category.get(), username);
        
        if (existing.isPresent()) {
            // Update existing share
            CategoryShare share = existing.get();
            share.setCanRead(canRead);
            share.setCanWrite(canWrite);
            logger.info("Updated category share: category {} shared with user {} (input: {})", 
                       categoryId, username, usernameOrEmail);
            return categoryShareRepository.save(share);
        } else {
            // Create new share
            CategoryShare share = new CategoryShare(category.get(), username, canRead, canWrite);
            logger.info("Created category share: category {} shared with user {} (input: {})", 
                       categoryId, username, usernameOrEmail);
            return categoryShareRepository.save(share);
        }
    }

    /**
     * Resolve a username from either a username or email address.
     * First tries to find by username, then by email.
     * 
     * @param usernameOrEmail The username or email to resolve
     * @return The username
     * @throws IllegalArgumentException if user not found
     */
    private String resolveUsername(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Username or email cannot be empty");
        }

        String identifier = usernameOrEmail.trim();

        // First, try to find by username
        Optional<User> userByUsername = userRepository.findByUsername(identifier);
        if (userByUsername.isPresent()) {
            return userByUsername.get().getUsername();
        }

        // If not found, try to find by email
        Optional<User> userByEmail = userRepository.findByEmail(identifier);
        if (userByEmail.isPresent()) {
            return userByEmail.get().getUsername();
        }

        // User not found
        throw new IllegalArgumentException("User with username or email '" + identifier + "' not found");
    }

    /**
     * Remove sharing for a user (by username or email)
     * This method accepts either a username or email and resolves it to the username.
     * 
     * @param categoryId The ID of the category to unshare
     * @param usernameOrEmail The username or email of the user to remove sharing from
     * @throws IllegalArgumentException if user not found
     */
    @Transactional
    public void unshareCategory(Long categoryId, String usernameOrEmail) {
        Optional<Category> category = categoryRepository.findById(categoryId);
        if (category.isPresent()) {
            // Resolve username from either username or email
            String username = resolveUsername(usernameOrEmail);
            categoryShareRepository.deleteByCategoryAndSharedWithUsername(category.get(), username);
            logger.info("Removed category share: category {} unshared from user {} (input: {})", 
                       categoryId, username, usernameOrEmail);
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

    /**
     * Create a new category explicitly with the given parameters.
     * Unlike getOrCreateCategory which creates the full path hierarchy,
     * this method creates only the specified category.
     * 
     * @param name The category name
     * @param path The complete category path (if null, will be generated from name and parent)
     * @param parentId The ID of the parent category (null for root categories)
     * @param createdBy The username of the user creating the category
     * @param isGlobal Whether this should be a global category
     * @return The created category entity
     * @throws IllegalArgumentException if validation fails or user lacks permission
     */
    @Transactional
    public Category createCategory(String name, String path, Long parentId, String createdBy, boolean isGlobal) {
        return createCategory(name, path, parentId, createdBy, isGlobal, null);
    }

    /**
     * Create a new category explicitly with the given parameters, optionally attaching a
     * JSON schema that will be used to validate all data entries created under it.
     *
     * @param name The category name
     * @param path The complete category path (if null, will be generated from name and parent)
     * @param parentId The ID of the parent category (null for root categories)
     * @param createdBy The username of the user creating the category
     * @param isGlobal Whether this should be a global category
     * @param jsonSchema Optional JSON schema definition used to validate entries in this category
     * @return The created category entity
     * @throws IllegalArgumentException if validation fails or user lacks permission
     */
    @Transactional
    public Category createCategory(String name, String path, Long parentId, String createdBy, boolean isGlobal,
                                    JsonNode jsonSchema) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        validateSchemaDefinition(jsonSchema);

        List<QuickSearchField> quickSearchFields = extractQuickSearchFields(jsonSchema);

        Category parent = null;
        String finalPath;

        // If parent ID is provided, fetch the parent and check permissions
        if (parentId != null) {
            parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
            
            // Check if user has write permission on the parent category
            if (!parent.hasWritePermission(createdBy)) {
                throw new IllegalArgumentException(
                    "You don't have permission to create a child category under '" + parent.getPath() + "'. " +
                    "You must be the owner or have write access to the parent category."
                );
            }
        }

        // Determine the path
        if (path != null && !path.trim().isEmpty()) {
            finalPath = normalizePath(path);
        } else {
            // Generate path from name and parent
            if (parent != null) {
                finalPath = parent.getPath() + "/" + name.trim();
            } else {
                finalPath = name.trim();
            }
        }

        // Check if category with this path already exists
        Optional<Category> existing = categoryRepository.findByPathLike(finalPath);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Category with path '" + finalPath + "' already exists");
        }

        // Create and save the new category
        Category newCategory = new Category(name.trim(), finalPath, parent, createdBy, isGlobal);
        newCategory.setJsonSchema(jsonSchema);
        newCategory.setQuickSearchPaths(quickSearchFields.stream().map(QuickSearchField::path).collect(Collectors.toList()));
        newCategory.setQuickSearchLabels(quickSearchFields.stream().map(QuickSearchField::label).collect(Collectors.toList()));
        newCategory = categoryRepository.save(newCategory);
        logger.info("Created new category: {} with path: {} for user: {} (global: {})", 
                   name, finalPath, createdBy, isGlobal);

        return newCategory;
    }

    /**
     * Validate that the given JSON node is a well-formed JSON schema.
     *
     * @param schema The JSON schema definition to validate (may be null, which is allowed since schemas are optional)
     * @throws IllegalArgumentException if the schema is malformed
     */
    public void validateSchemaDefinition(JsonNode schema) {
        if (schema == null || schema.isNull()) {
            return;
        }
        try {
            SCHEMA_FACTORY.getSchema(schema);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON schema: " + e.getMessage());
        }
    }

    /**
     * Validate a data entry's content against the JSON schema defined on its category, if any.
     *
     * @param category The category the content belongs to (may be null)
     * @param content The content to validate
     * @throws IllegalArgumentException if the content does not satisfy the category's schema
     */
    public void validateContentAgainstSchema(Category category, JsonNode content) {
        if (category == null || category.getJsonSchema() == null) {
            return;
        }

        JsonSchema schema = SCHEMA_FACTORY.getSchema(category.getJsonSchema());
        Set<ValidationMessage> errors = schema.validate(content);
        if (!errors.isEmpty()) {
            String messages = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Content does not match the schema defined for category '" +
                    category.getPath() + "': " + messages);
        }
    }

    /**
     * A single quick-search mapping declared in a schema: where to read the value from (JSON Path)
     * and, optionally, a human-readable label describing it.
     */
    public record QuickSearchField(String path, String label) {
    }

    /**
     * Extract the quick-search mappings declared in a schema's "{@value #QUICK_SEARCH_SCHEMA_KEY}" array,
     * capped at {@link Category#QUICK_SEARCH_COLUMN_COUNT} entries. Each array entry may be either a plain
     * JSON Path string (no label), or an object of the form {@code {"path": "$.foo", "label": "Foo"}}.
     *
     * @param schema The JSON schema definition (may be null)
     * @return The configured quick-search fields, in order (empty if none configured)
     * @throws IllegalArgumentException if the keyword is present but malformed
     */
    public List<QuickSearchField> extractQuickSearchFields(JsonNode schema) {
        if (schema == null || schema.isNull()) {
            return List.of();
        }

        JsonNode quickSearchNode = schema.get(QUICK_SEARCH_SCHEMA_KEY);
        if (quickSearchNode == null || quickSearchNode.isNull()) {
            return List.of();
        }
        if (!quickSearchNode.isArray()) {
            throw new IllegalArgumentException("'" + QUICK_SEARCH_SCHEMA_KEY + "' must be an array");
        }

        List<QuickSearchField> fields = new ArrayList<>();
        for (JsonNode entry : quickSearchNode) {
            fields.add(parseQuickSearchEntry(entry));
            if (fields.size() == Category.QUICK_SEARCH_COLUMN_COUNT) {
                logger.warn("More than {} quick-search entries defined; ignoring the rest", Category.QUICK_SEARCH_COLUMN_COUNT);
                break;
            }
        }
        return fields;
    }

    private QuickSearchField parseQuickSearchEntry(JsonNode entry) {
        // Plain string entry: just a JSON Path, no label
        if (entry.isTextual()) {
            return new QuickSearchField(entry.asText(), null);
        }

        // Object entry: { "path": "$.foo", "label": "Foo" } (label optional)
        if (entry.isObject()) {
            JsonNode pathNode = entry.get("path");
            if (pathNode == null || !pathNode.isTextual()) {
                throw new IllegalArgumentException("'" + QUICK_SEARCH_SCHEMA_KEY + "' entries must define a 'path' string");
            }
            JsonNode labelNode = entry.get("label");
            if (labelNode != null && !labelNode.isNull() && !labelNode.isTextual()) {
                throw new IllegalArgumentException("'" + QUICK_SEARCH_SCHEMA_KEY + "' entry 'label' must be a string");
            }
            String label = (labelNode != null && labelNode.isTextual()) ? labelNode.asText() : null;
            if (label != null && !QUICK_SEARCH_LABEL_PATTERN.matcher(label).matches()) {
                throw new IllegalArgumentException("'" + QUICK_SEARCH_SCHEMA_KEY + "' entry 'label' must be a single " +
                        "word with no spaces (letters, digits, underscore, starting with a letter or underscore): '" +
                        label + "'");
            }
            return new QuickSearchField(pathNode.asText(), label);
        }

        throw new IllegalArgumentException("'" + QUICK_SEARCH_SCHEMA_KEY +
                "' entries must be a JSON Path string or an object with 'path' and 'label'");
    }

    /**
     * Resolve the quick-search column values for a data entry's content using its category's
     * configured JSON Path expressions. Paths that don't resolve produce a null value.
     *
     * @param category The category the content belongs to (may be null)
     * @param content The content to extract values from
     * @return The resolved quick-search values, in column order (empty if no paths are configured)
     */
    public List<String> extractQuickSearchValues(Category category, JsonNode content) {
        if (category == null || content == null) {
            return List.of();
        }

        List<String> paths = category.getQuickSearchPaths();
        if (paths.isEmpty()) {
            return List.of();
        }

        DocumentContext context = JsonPath.using(JSON_PATH_CONFIG).parse(content);
        List<String> values = new ArrayList<>();
        for (String path : paths) {
            JsonNode value = context.read(path, JsonNode.class);
            values.add(quickSearchValueToString(value));
        }
        return values;
    }

    private String quickSearchValueToString(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        return value.isValueNode() ? value.asText() : value.toString();
    }
}
