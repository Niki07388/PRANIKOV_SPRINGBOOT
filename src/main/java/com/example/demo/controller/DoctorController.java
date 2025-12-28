package com.example.demo.controller;

import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.demo.entity.User;
import com.example.demo.dto.UserDTO;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllDoctors() {
        try {
            List<User> doctors = userService.getUsersByRole("doctor");
            return new ResponseEntity<>(doctors.stream()
                    .map(this::convertToDTO).collect(Collectors.toList()), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctor(@PathVariable String id) {
        try {
            User doctor = userService.getUserById(id);
            if (!"doctor".equals(doctor.getRole())) {
                return new ResponseEntity<>(Map.of("message", "Doctor not found"), HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(convertToDTO(doctor), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        dto.setPhone(user.getPhone());
        dto.setSpecialization(user.getSpecialization());
        dto.setLicense(user.getLicense());
        dto.setImageUrl(user.getImageUrl());
        return dto;
    }
}
