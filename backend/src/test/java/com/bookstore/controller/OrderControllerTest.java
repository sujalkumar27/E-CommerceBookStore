package com.bookstore.controller;

import com.bookstore.dto.cart.CartDto;
import com.bookstore.dto.order.OrderDto;
import com.bookstore.exception.BusinessRuleException;
import com.bookstore.exception.ConflictException;
import com.bookstore.exception.ForbiddenException;
import com.bookstore.exception.GlobalExceptionHandler;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.OrderStatus;
import com.bookstore.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ============================================================
 * OrderControllerTest — Integration Tests for Order Endpoints
 * ============================================================
 *
 * @AutoConfigureMockMvc(addFilters = false) disables all servlet filters.
 * @WithMockUser injects an authenticated principal for each test.
 * Service exceptions are mapped to HTTP status codes by GlobalExceptionHandler.
 */
@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest extends BaseControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  OrderService orderService;

    private UUID orderId;
    private OrderDto.OrderSummaryDto confirmedSummary;
    private CartDto emptyCart;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        Instant now = Instant.now();
        confirmedSummary = new OrderDto.OrderSummaryDto(
                orderId, now, now, OrderStatus.CONFIRMED,
                2, new BigDecimal("1598.00"), now.plusSeconds(172_800), true
        );
        emptyCart = new CartDto(List.of(), BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/orders
    // ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/orders: authenticated → 200 OK with order list")
    void getOrders_authenticated_returns200() throws Exception {
        when(orderService.getOrderHistory(any())).thenReturn(List.of(confirmedSummary));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/orders: empty history → 200 OK with empty array")
    void getOrders_empty_returns200EmptyList() throws Exception {
        when(orderService.getOrderHistory(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/orders/{id}
    // ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/orders/{id}: found and owned → 200 OK")
    void getOrderById_found_returns200() throws Exception {
        when(orderService.getOrderById(any(), eq(orderId))).thenReturn(buildDetailDto());

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/orders/{id}: not found → 404")
    void getOrderById_notFound_returns404() throws Exception {
        when(orderService.getOrderById(any(), any()))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/orders/{id}: belongs to other user → 403")
    void getOrderById_forbidden_returns403() throws Exception {
        when(orderService.getOrderById(any(), eq(orderId)))
                .thenThrow(new ForbiddenException("This order does not belong to you"));

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/orders/{id}/cancel
    // ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders/{id}/cancel: within window → 200 OK, status CANCELLED")
    void cancelOrder_withinWindow_returns200() throws Exception {
        OrderDto.OrderSummaryDto cancelled = new OrderDto.OrderSummaryDto(
                orderId, Instant.now(), Instant.now(), OrderStatus.CANCELLED,
                2, new BigDecimal("1598.00"), null, false
        );
        when(orderService.cancelOrder(any(), eq(orderId))).thenReturn(cancelled);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders/{id}/cancel: already cancelled → 409 Conflict")
    void cancelOrder_alreadyCancelled_returns409() throws Exception {
        when(orderService.cancelOrder(any(), eq(orderId)))
                .thenThrow(new ConflictException("This order is already cancelled"));

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders/{id}/cancel: window expired → 400 Bad Request")
    void cancelOrder_windowExpired_returns400() throws Exception {
        when(orderService.cancelOrder(any(), eq(orderId)))
                .thenThrow(new BusinessRuleException("The 48-hour cancellation window has expired."));

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders/{id}/cancel: belongs to other user → 403")
    void cancelOrder_forbidden_returns403() throws Exception {
        when(orderService.cancelOrder(any(), eq(orderId)))
                .thenThrow(new ForbiddenException("This order does not belong to you"));

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/orders/{id}/buy-again
    // ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders/{id}/buy-again: valid → 200 OK with updated cart")
    void buyAgain_valid_returns200WithCart() throws Exception {
        when(orderService.buyAgain(any(), eq(orderId))).thenReturn(emptyCart);

        mockMvc.perform(post("/api/orders/{id}/buy-again", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/orders/{id}/buy-again: order not found → 404")
    void buyAgain_notFound_returns404() throws Exception {
        when(orderService.buyAgain(any(), any()))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(post("/api/orders/{id}/buy-again", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private OrderDto.OrderDetailDto buildDetailDto() {
        return new OrderDto.OrderDetailDto(
                orderId, Instant.now(), Instant.now(), OrderStatus.CONFIRMED,
                Instant.now().plusSeconds(172_800), true,
                "CREDIT_CARD", 0,
                new BigDecimal("1598.00"), new BigDecimal("1598.00"),
                null, List.of()
        );
    }
}
