package com.example.demo.service;

import com.example.demo.entity.Appointment;
import com.example.demo.entity.User;
import com.example.demo.dto.AppointmentDTO;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public Appointment createAppointment(AppointmentDTO dto) {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID().toString());
        appointment.setPatientId(dto.getPatientId());
        appointment.setDoctorId(dto.getDoctorId());
        appointment.setDate(dto.getDate());
        appointment.setTime(dto.getTime());
        appointment.setReason(dto.getReason());
        appointment.setStatus(dto.getStatus() != null ? dto.getStatus() : "scheduled");
        appointment.setVisitType(dto.getVisitType() != null ? dto.getVisitType() : "in_person");

        return appointmentRepository.save(appointment);
    }

    public Appointment getAppointmentById(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
    }

    public List<Appointment> getAppointmentsByPatient(String patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAppointmentsByDoctor(String doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public List<User> getPatientsByDoctor(String doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        Set<String> patientIds = new HashSet<>();
        
        for (Appointment apt : appointments) {
            patientIds.add(apt.getPatientId());
        }
        
        List<User> patients = new ArrayList<>();
        for (String patientId : patientIds) {
            try {
                User patient = userRepository.findById(patientId).orElse(null);
                if (patient != null && "patient".equals(patient.getRole())) {
                    patients.add(patient);
                }
            } catch (Exception e) {
                // Skip if patient not found
            }
        }
        return patients;
    }

    public Appointment updateAppointment(String id, AppointmentDTO dto) {
        Appointment appointment = getAppointmentById(id);

        if (dto.getDate() != null) {
            appointment.setDate(dto.getDate());
        }
        if (dto.getTime() != null) {
            appointment.setTime(dto.getTime());
        }
        if (dto.getStatus() != null) {
            appointment.setStatus(dto.getStatus());
        }
        if (dto.getVisitType() != null) {
            appointment.setVisitType(dto.getVisitType());
        }
        if (dto.getReason() != null) {
            appointment.setReason(dto.getReason());
        }
        if (dto.getNotes() != null) {
            appointment.setNotes(dto.getNotes());
        }

        return appointmentRepository.save(appointment);
    }

    public void deleteAppointment(String id) {
        appointmentRepository.deleteById(id);
    }

    public Map<String, Object> getAvailability(String doctorId, LocalDate date, Integer bufferMinutes) {
        // Make buffer effectively final to use in lambda
        final int buffer = (bufferMinutes != null) ? bufferMinutes : 10;

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndDate(doctorId, date);
        Set<String> occupied = new HashSet<>();

        for (Appointment apt : appointments) {
            if (apt.getStatus() != null && !apt.getStatus().equals("cancelled")) {
                occupied.add(apt.getTime());
            }
        }

        List<Map<String, Object>> slots = new ArrayList<>();
        int startHour = 9;
        int endHour = 17;
        int stepMinutes = 30;

        Set<Integer> occupiedMinutes = new HashSet<>();
        for (String time : occupied) {
            try {
                String[] parts = time.split(":");
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                occupiedMinutes.add(h * 60 + m);
            } catch (Exception e) {
                // Skip malformed times
            }
        }

        LocalDateTime start = LocalDateTime.of(date, LocalTime.of(startHour, 0));
        LocalDateTime end = LocalDateTime.of(date, LocalTime.of(endHour, 0));

        while (start.isBefore(end)) {
            String timeStr = String.format("%02d:%02d", start.getHour(), start.getMinute());
            final int timeMinutes = start.getHour() * 60 + start.getMinute();

            boolean available = !occupied.contains(timeStr);
            boolean blocked = occupiedMinutes.stream()
                    .anyMatch(om -> Math.abs(timeMinutes - om) <= buffer);

            Map<String, Object> slot = new HashMap<>();
            slot.put("time", timeStr);
            slot.put("available", available && !blocked);
            slots.add(slot);

            start = start.plusMinutes(stepMinutes);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("doctorId", doctorId);
        result.put("date", date.toString());
        result.put("type", "in_person");
        result.put("slots", slots);

        return result;
    }
}