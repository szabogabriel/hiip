package com.hiip.datastorage.controller;

import com.hiip.datastorage.dto.DataStorageRequest;
import com.hiip.datastorage.dto.DataStorageResponse;
import com.hiip.datastorage.service.DataStorageFacadeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for data storage operations.
 * This controller handles HTTP requests and delegates business logic to DataStorageFacadeService.
 */
@RestController
@RequestMapping("/api/v1/data")
@Tag(name = "Data Storage", description = "Data storage management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DataStorageController {

    @Autowired
    private DataStorageFacadeService dataStorageFacadeService;

    @PostMapping
    @Operation(
        summary = "Create new data",
        description = "Create a new data storage entry with content and tags"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Data created successfully",
            content = @Content(schema = @Schema(implementation = DataStorageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<DataStorageResponse> createData(
            @RequestBody DataStorageRequest request,
            Authentication authentication) {
        
        String owner = authentication.getName();
        DataStorageResponse response = dataStorageFacadeService.createData(request, owner);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get data by ID",
        description = "Retrieve a specific data storage entry by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data retrieved successfully",
            content = @Content(schema = @Schema(implementation = DataStorageResponse.class))),
        @ApiResponse(responseCode = "404", description = "Data not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<DataStorageResponse> getDataById(
            @PathVariable Long id,
            Authentication authentication) {
        
        String owner = authentication.getName();
        Optional<DataStorageResponse> response = dataStorageFacadeService.getDataById(id, owner);
        
        return response.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(
        summary = "Get all data",
        description = "Retrieve all data storage entries for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Data list retrieved successfully")
    public ResponseEntity<List<DataStorageResponse>> getAllData(Authentication authentication) {
        String owner = authentication.getName();
        List<DataStorageResponse> data = dataStorageFacadeService.getAllData(owner);
        
        return ResponseEntity.ok(data);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search data by tags and/or category",
        description = "Search data storage entries by tags and/or category. Both parameters are optional. " +
                     "Category supports wildcard patterns using '*' (equivalent to SQL '%'). " +
                     "Examples: 'work/*' matches 'work/projects', 'work/notes'; '*project*' matches any path containing 'project'."
    )
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    public ResponseEntity<List<DataStorageResponse>> searchData(
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String category,
            Authentication authentication) {
        
        String owner = authentication.getName();
        List<DataStorageResponse> data = dataStorageFacadeService.searchData(tags, category, owner);
        
        return ResponseEntity.ok(data);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update data",
        description = "Update an existing data storage entry"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data updated successfully",
            content = @Content(schema = @Schema(implementation = DataStorageResponse.class))),
        @ApiResponse(responseCode = "404", description = "Data not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<DataStorageResponse> updateData(
            @PathVariable Long id,
            @RequestBody DataStorageRequest request,
            Authentication authentication) {
        
        String owner = authentication.getName();
        Optional<DataStorageResponse> response = dataStorageFacadeService.updateData(id, request, owner);
        
        return response.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete data",
        description = "Delete (hide) a data storage entry"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Data deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Data not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteData(
            @PathVariable Long id,
            Authentication authentication) {
        
        String owner = authentication.getName();
        boolean deleted = dataStorageFacadeService.deleteData(id, owner);
        
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
