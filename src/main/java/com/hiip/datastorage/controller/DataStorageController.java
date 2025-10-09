package com.hiip.datastorage.controller;

import com.hiip.datastorage.dto.DataStorageRequest;
import com.hiip.datastorage.dto.DataStorageResponse;
import com.hiip.datastorage.entity.DataStorage;
import com.hiip.datastorage.service.DataStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data")
public class DataStorageController {

    @Autowired
    private DataStorageService dataStorageService;

    @PostMapping
    public ResponseEntity<DataStorageResponse> createData(
            @RequestBody DataStorageRequest request,
            Authentication authentication) {
        
        String owner = authentication.getName();
        DataStorage dataStorage = new DataStorage(request.getContent(), request.getTags(), owner);
        DataStorage saved = dataStorageService.createData(dataStorage);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(new DataStorageResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataStorageResponse> getDataById(
            @PathVariable Long id,
            Authentication authentication) {
        
        String owner = authentication.getName();
        return dataStorageService.getDataById(id, owner)
                .map(data -> ResponseEntity.ok(new DataStorageResponse(data)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<DataStorageResponse>> getAllData(Authentication authentication) {
        String owner = authentication.getName();
        List<DataStorageResponse> data = dataStorageService.getAllData(owner)
                .stream()
                .map(DataStorageResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(data);
    }

    @GetMapping("/search")
    public ResponseEntity<List<DataStorageResponse>> searchByTags(
            @RequestParam List<String> tags,
            Authentication authentication) {
        
        String owner = authentication.getName();
        List<DataStorageResponse> data = dataStorageService.searchByTags(tags, owner)
                .stream()
                .map(DataStorageResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(data);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataStorageResponse> updateData(
            @PathVariable Long id,
            @RequestBody DataStorageRequest request,
            Authentication authentication) {
        
        String owner = authentication.getName();
        DataStorage updatedData = new DataStorage(request.getContent(), request.getTags(), owner);
        
        return dataStorageService.updateData(id, updatedData, owner)
                .map(data -> ResponseEntity.ok(new DataStorageResponse(data)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hideData(
            @PathVariable Long id,
            Authentication authentication) {
        
        String owner = authentication.getName();
        boolean hidden = dataStorageService.hideData(id, owner);
        
        return hidden ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
