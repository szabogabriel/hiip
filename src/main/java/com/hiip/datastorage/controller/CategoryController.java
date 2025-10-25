package com.hiip.datastorage.controller;

import com.hiip.datastorage.dto.CategoryRequest;
import com.hiip.datastorage.dto.CategoryResponse;
import com.hiip.datastorage.dto.CategoryShareRequest;
import com.hiip.datastorage.dto.CategoryShareResponse;
import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.CategoryShare;
import com.hiip.datastorage.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for category operations.
 * Provides endpoints to list and query existing categories, with user-specific filtering and sharing.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Category management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @Operation(
        summary = "Get all categories",
        description = "Retrieve a flat list of all existing categories"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        List<CategoryResponse> response = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(
        summary = "Create a new category",
        description = "Create a new category explicitly. The path can be provided or will be auto-generated from the name and parent. To create a child category, you must own or have write access to the parent category."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Category created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or category already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions on parent category")
    })
    public ResponseEntity<?> createCategory(
            @RequestBody CategoryRequest request,
            Authentication authentication) {
        
        try {
            String currentUser = authentication.getName();
            
            Category category = categoryService.createCategory(
                request.getName(),
                request.getPath(),
                request.getParentId(),
                currentUser,
                request.isGlobal()
            );
            
            CategoryResponse response = convertToResponse(category);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            // Check if it's a permission error
            if (e.getMessage().contains("don't have permission")) {
                return ResponseEntity.status(403).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/roots")
    @Operation(
        summary = "Get root categories",
        description = "Retrieve all root categories (categories without parents)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Root categories retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        List<Category> categories = categoryService.getRootCategories();
        List<CategoryResponse> response = categories.stream()
                .map(this::convertToResponseWithChildren)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tree")
    @Operation(
        summary = "Get category tree",
        description = "Retrieve the complete category hierarchy as a tree structure"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category tree retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<CategoryResponse>> getCategoryTree() {
        List<Category> rootCategories = categoryService.getRootCategories();
        List<CategoryResponse> response = rootCategories.stream()
                .map(this::convertToResponseWithChildren)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search categories by path prefix",
        description = "Find all categories that start with the given path prefix"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories found successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<CategoryResponse>> searchCategories(
            @RequestParam(name = "prefix") String pathPrefix) {
        List<Category> categories = categoryService.getCategoriesByPathPrefix(pathPrefix);
        List<CategoryResponse> response = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get category by ID",
        description = "Retrieve a specific category by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return categoryService.getAllCategories().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convert Category entity to CategoryResponse DTO (without children)
     */
    private CategoryResponse convertToResponse(Category category) {
        CategoryResponse response = new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getPath(),
            category.getParent() != null ? category.getParent().getId() : null,
            category.getCreatedBy(),
            category.isGlobal(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
        
        // Add shares if any
        List<CategoryShareResponse> shares = category.getSharedWith().stream()
                .map(CategoryShareResponse::new)
                .collect(Collectors.toList());
        response.setSharedWith(shares);
        
        return response;
    }

    /**
     * Convert Category entity to CategoryResponse DTO (with children recursively)
     */
    private CategoryResponse convertToResponseWithChildren(Category category) {
        CategoryResponse response = convertToResponse(category);
        
        // Recursively convert children
        List<CategoryResponse> children = category.getChildren().stream()
                .map(this::convertToResponseWithChildren)
                .collect(Collectors.toList());
        response.setChildren(children);
        
        return response;
    }

    @PostMapping("/{id}/share")
    @Operation(
        summary = "Share category with a user",
        description = "Share a category with another user with read/write permissions. Only the category owner can share."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category shared successfully"),
        @ApiResponse(responseCode = "403", description = "User doesn't own this category"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> shareCategory(
            @PathVariable Long id,
            @RequestBody CategoryShareRequest request,
            Authentication authentication) {
        
        String currentUser = authentication.getName();
        
        // Check if user owns this category
        if (!categoryService.canUserWriteCategory(id, currentUser)) {
            return ResponseEntity.status(403).body("You don't have permission to share this category");
        }
        
        CategoryShare share = categoryService.shareCategory(
            id, 
            request.getUsername(), 
            request.isCanRead(), 
            request.isCanWrite()
        );
        
        return ResponseEntity.ok(new CategoryShareResponse(share));
    }

    @DeleteMapping("/{id}/share/{username}")
    @Operation(
        summary = "Unshare category with a user",
        description = "Remove sharing for a specific user. Only the category owner can unshare."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category unshared successfully"),
        @ApiResponse(responseCode = "403", description = "User doesn't own this category"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> unshareCategory(
            @PathVariable Long id,
            @PathVariable String username,
            Authentication authentication) {
        
        String currentUser = authentication.getName();
        
        // Check if user owns this category
        if (!categoryService.canUserWriteCategory(id, currentUser)) {
            return ResponseEntity.status(403).body("You don't have permission to modify sharing for this category");
        }
        
        categoryService.unshareCategory(id, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/shares")
    @Operation(
        summary = "Get all shares for a category",
        description = "List all users this category is shared with. Only accessible by category owner."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Shares retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "User doesn't own this category"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> getCategoryShares(
            @PathVariable Long id,
            Authentication authentication) {
        
        String currentUser = authentication.getName();
        
        // Check if user owns this category
        if (!categoryService.canUserWriteCategory(id, currentUser)) {
            return ResponseEntity.status(403).body("You don't have permission to view shares for this category");
        }
        
        List<CategoryShare> shares = categoryService.getCategoryShares(id);
        List<CategoryShareResponse> response = shares.stream()
                .map(CategoryShareResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-categories")
    @Operation(
        summary = "Get categories accessible by current user",
        description = "Retrieve all categories the user can access (owned, shared, or global)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<CategoryResponse>> getMyCategories(Authentication authentication) {
        String currentUser = authentication.getName();
        List<Category> categories = categoryService.getCategoriesAccessibleByUser(currentUser);
        List<CategoryResponse> response = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
