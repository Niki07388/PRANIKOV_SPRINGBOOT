package com.example.demo.service;

import com.example.demo.entity.HealthRecord;
import com.example.demo.dto.HealthRecordDTO;
import com.example.demo.repository.HealthRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthRecordService {
    private final HealthRecordRepository healthRecordRepository;

    public HealthRecord createHealthRecord(HealthRecordDTO dto) {
        HealthRecord record = new HealthRecord();
        record.setId(UUID.randomUUID().toString());
        record.setPatientId(dto.getPatientId());
        record.setDoctorId(dto.getDoctorId());
        record.setType(dto.getType());
        record.setDescription(dto.getDescription());
        record.setAttachments(dto.getAttachments());
        record.setDate(LocalDate.now());

        return healthRecordRepository.save(record);
    }

    public HealthRecord getHealthRecordById(String id) {
        return healthRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Health record not found"));
    }

    public List<HealthRecord> getHealthRecordsByPatient(String patientId) {
        return healthRecordRepository.findByPatientId(patientId);
    }

    public List<HealthRecord> getHealthRecordsByDoctor(String doctorId) {
        return healthRecordRepository.findByDoctorId(doctorId);
    }

    public List<HealthRecord> getAllHealthRecords() {
        return healthRecordRepository.findAll();
    }

    public HealthRecord updateHealthRecord(String id, HealthRecordDTO dto) {
        HealthRecord record = getHealthRecordById(id);

        if (dto.getType() != null) {
            record.setType(dto.getType());
        }
        if (dto.getDescription() != null) {
            record.setDescription(dto.getDescription());
        }
        if (dto.getAttachments() != null) {
            record.setAttachments(dto.getAttachments());
        }

        return healthRecordRepository.save(record);
    }

    public void deleteHealthRecord(String id) {
        healthRecordRepository.deleteById(id);
    }
}
