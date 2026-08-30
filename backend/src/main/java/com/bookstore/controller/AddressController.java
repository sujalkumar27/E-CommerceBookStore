package com.bookstore.controller;

import com.bookstore.dto.order.AddressDto;
import com.bookstore.dto.order.AddressRequest;
import com.bookstore.model.User;
import com.bookstore.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * AddressController — Delivery Address Management Endpoints
 * ============================================================
 *
 * BASE URL: /api/addresses
 * All endpoints require authentication.
 *
 * ENDPOINTS:
 *   GET    /api/addresses        → list all saved addresses
 *   POST   /api/addresses        → add a new address
 *   PUT    /api/addresses/{id}   → update an existing address
 *   DELETE /api/addresses/{id}   → delete an address
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDto>> getAddresses(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(addressService.getAddresses(user));
    }

    @PostMapping
    public ResponseEntity<AddressDto> addAddress(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.addAddress(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> updateAddress(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        addressService.deleteAddress(user, id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
