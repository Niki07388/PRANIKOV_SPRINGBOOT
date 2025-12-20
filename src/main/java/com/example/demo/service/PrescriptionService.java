package com.example.demo.service;

import com.example.demo.entity.Prescription;
import com.example.demo.dto.PrescriptionDTO;
import com.example.demo.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionService {
    private final PrescriptionRepository prescriptionRepository;

    public Prescription createPrescription(PrescriptionDTO dto) {
        Prescription prescription = new Prescription();
        prescription.setId(UUID.randomUUID().toString());
        prescription.setPatientId(dto.getPatientId());
        prescription.setDoctorId(dto.getDoctorId());
        prescription.setMedication(dto.getMedication());
        prescription.setDosage(dto.getDosage());
        prescription.setFrequency(dto.getFrequency());
        prescription.setDuration(dto.getDuration());
        prescription.setNotes(dto.getNotes());
        prescription.setDate(LocalDate.now());

        return prescriptionRepository.save(prescription);
    }

    public Prescription getPrescriptionById(String id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found"));
    }

    public List<Prescription> getPrescriptionsByPatient(String patientId) {
        return prescriptionRepository.findByPatientId(patientId);
    }

    public List<Prescription> getPrescriptionsByDoctor(String doctorId) {
        return prescriptionRepository.findByDoctorId(doctorId);
    }

    public List<Prescription> getAllPrescriptions() {
        return prescriptionRepository.findAll();
    }

    public Prescription updatePrescription(String id, PrescriptionDTO dto) {
        Prescription prescription = getPrescriptionById(id);

        if (dto.getMedication() != null) {
            prescription.setMedication(dto.getMedication());
        }
        if (dto.getDosage() != null) {
            prescription.setDosage(dto.getDosage());
        }
        if (dto.getFrequency() != null) {
            prescription.setFrequency(dto.getFrequency());
        }
        if (dto.getDuration() != null) {
            prescription.setDuration(dto.getDuration());
        }
        if (dto.getNotes() != null) {
            prescription.setNotes(dto.getNotes());
        }

        return prescriptionRepository.save(prescription);
    }

    public void deletePrescription(String id) {
        prescriptionRepository.deleteById(id);
    }

    public Prescription refillPrescription(String id) {
        Prescription prescription = getPrescriptionById(id);
        
        if (prescription.getRefillsRemaining() == null || prescription.getRefillsRemaining() <= 0) {
            throw new RuntimeException("No refills remaining");
        }

        prescription.setRefillsRemaining(prescription.getRefillsRemaining() - 1);
        return prescriptionRepository.save(prescription);
    }
}
