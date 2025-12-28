package com.example.demo.controller;

import com.example.demo.service.UserService;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.User;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Order;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final AppointmentRepository appointmentRepository;
    private final OrderRepository orderRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            if (!isAdmin()) {
                return new ResponseEntity<>(Map.of("message", "Unauthorized"), HttpStatus.FORBIDDEN);
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userService.getAllUsers().size());
            stats.put("totalPatients", userService.getUsersByRole("patient").size());
            stats.put("totalDoctors", userService.getUsersByRole("doctor").size());
            stats.put("totalAppointments", appointmentRepository.findAll().size());
            stats.put("todayAppointments", appointmentRepository.findByDate(LocalDate.now()).size());
            stats.put("totalOrders", orderRepository.findAll().size());
            
            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            if (!isAdmin()) {
                return new ResponseEntity<>(Map.of("message", "Unauthorized"), HttpStatus.FORBIDDEN);
            }
            
            List<User> users = userService.getAllUsers();
            return new ResponseEntity<>(users.stream()
                    .map(this::convertToDTO).collect(Collectors.toList()), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody UserDTO dto) {
        try {
            if (!isAdmin()) {
                return new ResponseEntity<>(Map.of("message", "Unauthorized"), HttpStatus.FORBIDDEN);
            }
            
            User user = userService.updateUser(id, dto);
            return new ResponseEntity<>(convertToDTO(user), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            if (!isAdmin()) {
                return new ResponseEntity<>(Map.of("message", "Unauthorized"), HttpStatus.FORBIDDEN);
            }
            
            userService.deleteUser(id);
            return new ResponseEntity<>(Map.of("message", "User deleted successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    protected String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (String) auth.getPrincipal();
    }

    protected String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (String) auth.getDetails();
    }

    protected boolean isAdmin() {
        return "admin".equals(getCurrentUserRole());
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        dto.setPhone(user.getPhone());
        dto.setPhoneVerified(user.getPhoneVerified());
        dto.setAddress(user.getAddress());
        dto.setSpecialization(user.getSpecialization());
        dto.setLicense(user.getLicense());
        dto.setImageUrl(user.getImageUrl());
        return dto;
    }
}
