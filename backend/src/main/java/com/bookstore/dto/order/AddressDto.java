package com.bookstore.dto.order;

import java.time.Instant;
import java.util.UUID;

/**
 * AddressDto — Delivery address returned in API responses.
 * Used in address list, order detail, and purchase confirmation.
 */
public record AddressDto(
        UUID id,
        String fullName,
        String line1,
        String line2,
        String city,
        String state,
        String pincode,
        boolean isDefault
) {}
