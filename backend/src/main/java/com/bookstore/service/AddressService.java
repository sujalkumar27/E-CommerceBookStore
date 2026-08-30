package com.bookstore.service;

import com.bookstore.dto.order.AddressDto;
import com.bookstore.dto.order.AddressRequest;
import com.bookstore.exception.ForbiddenException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Address;
import com.bookstore.model.User;
import com.bookstore.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * AddressService — Manage Customer Delivery Addresses
 * ============================================================
 *
 * WHAT THIS DOES:
 * CRUD operations for a user's saved delivery addresses.
 * A user can have multiple addresses and selects one at checkout.
 *
 * RULES:
 * - A user can only see/edit/delete THEIR OWN addresses
 * - If a user's first address is saved, it auto-becomes the default
 * - When a new address is set as default, the old default is cleared
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;

    /** Get all saved addresses for the logged-in user */
    @Transactional(readOnly = true)
    public List<AddressDto> getAddresses(User user) {
        return addressRepository.findAllByUserOrderByCreatedAtAsc(user)
                .stream().map(this::toDto).toList();
    }

    /** Save a new delivery address */
    public AddressDto addAddress(User user, AddressRequest request) {
        Address address = new Address();
        address.setUser(user);
        mapFields(address, request);

        // If this is the user's first address, auto-set as default
        if (addressRepository.countByUser(user) == 0) {
            address.setDefault(true);
        }

        // If user explicitly wants this as default, clear old default first
        if (request.isDefault()) {
            clearDefaultForUser(user);
            address.setDefault(true);
        }

        return toDto(addressRepository.save(address));
    }

    /** Update an existing address — ownership check first */
    public AddressDto updateAddress(User user, UUID addressId, AddressRequest request) {
        Address address = findOwnedAddress(user, addressId);
        mapFields(address, request);

        if (request.isDefault()) {
            clearDefaultForUser(user);
            address.setDefault(true);
        }

        return toDto(addressRepository.save(address));
    }

    /** Delete an address — ownership check first */
    public void deleteAddress(User user, UUID addressId) {
        Address address = findOwnedAddress(user, addressId);
        addressRepository.delete(address);
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /** Find an address and verify it belongs to this user */
    private Address findOwnedAddress(User user, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("This address does not belong to you");
        }
        return address;
    }

    /** Copy request fields onto an Address entity */
    private void mapFields(Address address, AddressRequest request) {
        address.setFullName(request.fullName());
        address.setPhone(request.phone());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPincode(request.pincode());
    }

    /** Remove default flag from all of this user's addresses */
    private void clearDefaultForUser(User user) {
        addressRepository.findAllByUserOrderByCreatedAtAsc(user)
                .forEach(a -> a.setDefault(false));
    }

    /** Convert Address entity → AddressDto */
    public AddressDto toDto(Address address) {
        return new AddressDto(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.isDefault()
        );
    }
}
