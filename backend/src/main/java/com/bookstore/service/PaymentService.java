package com.bookstore.service;

import com.bookstore.dto.order.AddressDto;
import com.bookstore.dto.payment.PaymentRequest;
import com.bookstore.dto.payment.PaymentResponse;
import com.bookstore.exception.BusinessRuleException;
import com.bookstore.exception.ForbiddenException;
import com.bookstore.exception.PaymentException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.*;
import com.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * ============================================================
 * PaymentService — Checkout, Payment Simulation, Order Creation
 * ============================================================
 *
 * WHAT THIS DOES:
 * Handles the entire checkout flow in ONE database transaction:
 *
 *   1. Validate cart is not empty
 *   2. Validate delivery address belongs to this user
 *   3. Validate gift points (if any)
 *   4. SIMULATE payment (90% success, 10% failure)
 *   5. On success:
 *      a. Create Order record (status = CONFIRMED)
 *      b. Create OrderItem records (with price snapshot + delivery date)
 *      c. Deduct gift points from user balance
 *      d. Clear the cart
 *      e. Return purchase confirmation data
 *
 * @Transactional:
 * All 5 steps happen in ONE database transaction.
 * If anything fails after payment (e.g. can't save order), the whole
 * thing rolls back — no orphaned data or double-charged users.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressService addressService;

    // Random for payment simulation (not for cryptography — that's fine here)
    private final Random random = new Random();

    /**
     * Initiate payment and create the order.
     *
     * @param user    - the logged-in user
     * @param request - delivery address, payment method, gift points
     * @return PaymentResponse = purchase confirmation data
     * @throws PaymentException      if simulated payment fails
     * @throws BusinessRuleException if cart empty or points invalid
     * @throws ForbiddenException    if address belongs to another user
     */
    public PaymentResponse initiatePayment(User user, PaymentRequest request) {

        // ---- Step 1: Validate cart is not empty ----
        List<CartItem> cartItems = cartRepository.findAllByUserOrderByAddedAtAsc(user);
        if (cartItems.isEmpty()) {
            throw new BusinessRuleException("Your cart is empty. Add books before checking out.");
        }

        // ---- Step 2: Validate delivery address belongs to this user ----
        Address deliveryAddress = addressRepository.findById(request.deliveryAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found"));
        if (!deliveryAddress.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("This delivery address does not belong to you");
        }

        // ---- Step 3: Calculate order total ----
        BigDecimal rawTotal = cartItems.stream()
                .map(item -> item.getBook().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Validate gift points
        int pointsToRedeem = request.giftPointsToRedeem();
        if (pointsToRedeem < 0) {
            throw new BusinessRuleException("Gift points cannot be negative");
        }
        if (pointsToRedeem > user.getGiftPointBalance()) {
            throw new BusinessRuleException(
                    "You only have " + user.getGiftPointBalance() + " gift points available");
        }
        // Points cannot exceed order total (1 point = ₹1)
        if (BigDecimal.valueOf(pointsToRedeem).compareTo(rawTotal) > 0) {
            throw new BusinessRuleException("Gift points cannot exceed the order total");
        }

        // Amount actually charged = total - gift point discount
        BigDecimal amountCharged = rawTotal.subtract(BigDecimal.valueOf(pointsToRedeem));

        // ---- Step 4: Simulate payment ----
        // 90% chance of success, 10% chance of failure
        // Simulates real-world payment gateway behaviour (D-004)
        boolean paymentSuccess = random.nextInt(10) != 0; // 0 = fail, 1-9 = success
        if (!paymentSuccess) {
            throw new PaymentException("Payment failed. Please try again.");
        }

        // ---- Step 5: Payment succeeded — create order in one transaction ----
        Instant now = Instant.now();

        // 5a. Create the Order header
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(rawTotal);
        order.setGiftPointsRedeemed(pointsToRedeem);
        order.setDeliveryAddress(deliveryAddress);
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentConfirmedAt(now);

        // 5b. Create OrderItem for each cart item
        LocalDate orderDate = LocalDate.now();
        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            int offsetDays = book.getCategory().getDeliveryOffsetDays();
            LocalDate deliveryDate = orderDate.plusDays(offsetDays);

            OrderItem orderItem = new OrderItem(
                    order,
                    book,
                    cartItem.getQuantity(),
                    book.getPrice(),        // price snapshot at order time
                    deliveryDate            // order date + category offset (D-005)
            );
            order.getItems().add(orderItem);
        }

        orderRepository.save(order); // saves order + all items (cascade = PERSIST)

        // 5c. Deduct gift points from user balance
        if (pointsToRedeem > 0) {
            user.setGiftPointBalance(user.getGiftPointBalance() - pointsToRedeem);
            userRepository.save(user);
        }

        // 5d. Clear the cart
        cartRepository.deleteAllByUser(user);

        // 5e. Build and return purchase confirmation response
        AddressDto addressDto = addressService.toDto(deliveryAddress);

        List<PaymentResponse.OrderItemInfo> itemInfos = order.getItems().stream()
                .map(oi -> new PaymentResponse.OrderItemInfo(
                        oi.getBook().getId(),
                        oi.getBook().getTitle(),
                        oi.getBook().getAuthor(),
                        oi.getQuantity(),
                        oi.getLineTotal(),
                        oi.getTentativeDeliveryDate()
                )).toList();

        return new PaymentResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentConfirmedAt(),
                order.getCancellationDeadline(),
                rawTotal,
                pointsToRedeem,
                amountCharged,
                user.getGiftPointBalance(),
                itemInfos,
                addressDto
        );
    }
}
