package com.hiip.datastorage.service;

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

    public Optional<DataStorage> updateData(Long id, DataStorage updatedData, String owner) {
        Optional<DataStorage> existingData = dataStorageRepository.findById(id);
        if (existingData.isPresent() && existingData.get().getOwner().equals(owner) && !existingData.get().isHidden()) {
            DataStorage data = existingData.get();
            data.setContent(updatedData.getContent());
            data.setTags(updatedData.getTags());
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
}
