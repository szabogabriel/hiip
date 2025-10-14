package com.hiip.datastorage.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hiip.datastorage.dto.DataStorageRequest;
import com.hiip.datastorage.dto.DataStorageResponse;
import com.hiip.datastorage.entity.DataStorage;

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
        
        DataStorage dataStorage = new DataStorage(request.getContent(), request.getTags(), owner);
        DataStorage saved = dataStorageService.createData(dataStorage);
        
        logger.info("Saved data with ID: {}, tags: {}", saved.getId(), saved.getTags());
        
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
        
        DataStorage updatedData = new DataStorage(request.getContent(), request.getTags(), owner);
        
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
}
