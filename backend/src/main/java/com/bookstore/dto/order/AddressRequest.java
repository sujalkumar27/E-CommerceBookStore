package com.bookstore.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * AddressRequest — Used for both creating and updating a delivery address.
 * POST /api/addresses  and  PUT /api/addresses/{id}
 */
public record AddressRequest(

        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Address line 1 is required")
        String line1,

        String line2,   // optional — apartment, suite, landmark

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Pincode is required")
        String pincode,

        boolean isDefault   // should this become the default address?
) {}
