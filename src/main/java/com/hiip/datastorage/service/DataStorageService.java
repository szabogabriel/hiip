package com.hiip.datastorage.service;

import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.DataStorage;
import com.hiip.datastorage.repository.DataStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DataStorageService {

    @Autowired
    private DataStorageRepository dataStorageRepository;

    public DataStorage createData(DataStorage dataStorage) {
        return dataStorageRepository.save(dataStorage);
    }

    public Optional<DataStorage> getDataById(Long id, String owner) {
        Optional<DataStorage> data = dataStorageRepository.findById(id);
        if (data.isPresent() && data.get().getOwner().equals(owner) && !data.get().isHidden()) {
            return data;
        }
        return Optional.empty();
    }

    public List<DataStorage> getAllData(String owner) {
        return dataStorageRepository.findByOwnerAndHiddenFalse(owner);
    }

    public List<DataStorage> searchByTags(List<String> tags, String owner) {
        return dataStorageRepository.findByTagsInAndOwnerAndHiddenFalse(tags, owner);
    }

    /**
     * Search data by tags and/or category.
     * Both parameters are optional.
     * 
     * @param tags the list of tags to search for (optional)
     * @param category the category to filter by (optional)
     * @param owner the owner username
     * @return list of data storage entries matching the criteria
     */
    public List<DataStorage> searchData(List<String> tags, Category category, String owner) {
        // Both tags and category provided
        if (tags != null && !tags.isEmpty() && category != null) {
            return dataStorageRepository.findByTagsAndCategoryAndOwnerAndHiddenFalse(tags, category, owner);
        }
        // Only tags provided
        else if (tags != null && !tags.isEmpty()) {
            return dataStorageRepository.findByTagsInAndOwnerAndHiddenFalse(tags, owner);
        }
        // Only category provided
        else if (category != null) {
            return dataStorageRepository.findByCategoryAndOwnerAndHiddenFalse(category, owner);
        }
        // Neither provided - return all data
        else {
            return dataStorageRepository.findByOwnerAndHiddenFalse(owner);
        }
    }

    /**
     * Search data by tags and/or category pattern with wildcards.
     * Converts '*' wildcards to SQL '%' for LIKE queries.
     * 
     * @param tags the list of tags to search for (optional)
     * @param categoryPattern the category pattern with wildcards (e.g., "work/*" or "*project*")
     * @param owner the owner username
     * @return list of data storage entries matching the criteria
     */
    public List<DataStorage> searchDataByPattern(List<String> tags, String categoryPattern, String owner) {
        // Convert '*' to SQL '%' wildcard
        String sqlPattern = categoryPattern.replace("*", "%");
        
        // Both tags and category pattern provided
        if (tags != null && !tags.isEmpty()) {
            return dataStorageRepository.findByTagsAndCategoryPathLikeAndOwnerAndHiddenFalse(tags, sqlPattern, owner);
        }
        // Only category pattern provided
        else {
            return dataStorageRepository.findByCategoryPathLikeAndOwnerAndHiddenFalse(sqlPattern, owner);
        }
    }

    public Optional<DataStorage> updateData(Long id, DataStorage updatedData, String owner) {
        Optional<DataStorage> existingData = dataStorageRepository.findById(id);
        if (existingData.isPresent() && existingData.get().getOwner().equals(owner) && !existingData.get().isHidden()) {
            DataStorage data = existingData.get();
            data.setContent(updatedData.getContent());
            data.setTags(updatedData.getTags());
            data.setCategory(updatedData.getCategory());
            data.setQuickSearchValues(updatedData.getQuickSearchValues());
            return Optional.of(dataStorageRepository.save(data));
        }
        return Optional.empty();
    }

    public boolean hideData(Long id, String owner) {
        Optional<DataStorage> existingData = dataStorageRepository.findById(id);
        if (existingData.isPresent() && existingData.get().getOwner().equals(owner)) {
            DataStorage data = existingData.get();
            data.setHidden(true);
            dataStorageRepository.save(data);
            return true;
        }
        return false;
    }

    /**
     * Get all data accessible by the user (owned + shared via categories + global categories).
     * 
     * @param username the username
     * @return list of all accessible data storage entries
     */
    public List<DataStorage> getAllAccessibleData(String username) {
        return dataStorageRepository.findAccessibleByUser(username);
    }

    /**
     * Search data accessible by user (owned + shared) by tags and/or category.
     * Both parameters are optional.
     * 
     * @param tags the list of tags to search for (optional)
     * @param category the category to filter by (optional)
     * @param username the username
     * @return list of data storage entries matching the criteria
     */
    public List<DataStorage> searchAccessibleData(List<String> tags, Category category, String username) {
        // Both tags and category provided
        if (tags != null && !tags.isEmpty() && category != null) {
            return dataStorageRepository.findByTagsAndCategoryAccessibleByUser(tags, category, username);
        }
        // Only tags provided
        else if (tags != null && !tags.isEmpty()) {
            return dataStorageRepository.findByTagsAccessibleByUser(tags, username);
        }
        // Only category provided
        else if (category != null) {
            return dataStorageRepository.findByCategoryAccessibleByUser(category, username);
        }
        // Neither provided - return all accessible data
        else {
            return dataStorageRepository.findAccessibleByUser(username);
        }
    }

    /**
     * Search data accessible by user (owned + shared) by tags and/or category pattern with wildcards.
     * Converts '*' wildcards to SQL '%' for LIKE queries.
     * 
     * @param tags the list of tags to search for (optional)
     * @param categoryPattern the category pattern with wildcards (e.g., "work/*" or "*project*")
     * @param username the username
     * @return list of data storage entries matching the criteria
     */
    public List<DataStorage> searchAccessibleDataByPattern(List<String> tags, String categoryPattern, String username) {
        // Convert '*' to SQL '%' wildcard
        String sqlPattern = categoryPattern.replace("*", "%");
        
        // Both tags and category pattern provided
        if (tags != null && !tags.isEmpty()) {
            return dataStorageRepository.findByTagsAndCategoryPathLikeAccessibleByUser(tags, sqlPattern, username);
        }
        // Only category pattern provided
        else {
            return dataStorageRepository.findByCategoryPathLikeAccessibleByUser(sqlPattern, username);
        }
    }
}
