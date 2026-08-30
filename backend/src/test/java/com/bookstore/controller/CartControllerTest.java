package com.bookstore.controller;

import com.bookstore.dto.cart.CartDto;
import com.bookstore.exception.ForbiddenException;
import com.bookstore.exception.GlobalExceptionHandler;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ============================================================
 * CartControllerTest — Integration Tests for Cart Endpoints
 * ============================================================
 *
 * @AutoConfigureMockMvc(addFilters = false) disables all Servlet filters
 * (including the JWT filter and CSRF filter) so tests are not blocked
 * by auth/CSRF issues.
 *
 * @WithMockUser provides an authenticated Spring Security principal for
 * tests that need a logged-in user. Combined with any() matchers on the
 * service mock, this gives us a clean test for each endpoint.
 *
 * Service-level ownership (403) is verified by having the mock throw
 * ForbiddenException — the controller passes it to GlobalExceptionHandler.
 */
@WebMvcTest(CartController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest extends BaseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  CartService cartService;

    private CartDto emptyCart;
    private CartDto cartWithItem;
    private UUID itemId;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
        bookId = UUID.randomUUID();

        emptyCart = new CartDto(List.of(), BigDecimal.ZERO);
        cartWithItem = new CartDto(
                List.of(new CartDto.CartItemDto(
                        itemId,
                        new CartDto.BookInfo(bookId, "Clean Code", "Robert Martin",
                                new BigDecimal("799.00"), true, null),
                        2, new BigDecimal("1598.00")
                )),
                new BigDecimal("1598.00")
        );
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/cart
    // ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/cart: authenticated user → 200 OK with cart")
    void getCart_authenticated_returns200() throws Exception {
        when(cartService.getCart(any())).thenReturn(cartWithItem);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartTotal").value(1598.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cart: empty cart → 200 OK with zero total")
    void getCart_emptyCart_returns200WithZeroTotal() throws Exception {
        when(cartService.getCart(any())).thenReturn(emptyCart);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.cartTotal").value(0));
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/cart/items
    // ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /api/cart/items: valid request → 200 OK with updated cart")
    void addItem_validRequest_returns200() throws Exception {
        when(cartService.addItem(any(), any())).thenReturn(cartWithItem);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "bookId": "%s", "quantity": 2 }
                                """.formatted(bookId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].book.title").value("Clean Code"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/cart/items: missing bookId → 400 Bad Request")
    void addItem_missingBookId_returns400() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "quantity": 2 }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/cart/items: quantity = 0 → 400 Bad Request")
    void addItem_zeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "bookId": "%s", "quantity": 0 }
                                """.formatted(bookId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/cart/items: book not found → 404")
    void addItem_bookNotFound_returns404() throws Exception {
        when(cartService.addItem(any(), any()))
                .thenThrow(new ResourceNotFoundException("Book not found"));

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "bookId": "%s", "quantity": 1 }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────
    // PUT /api/cart/items/{itemId}
    // ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("PUT /api/cart/items/{id}: owner updates quantity → 200 OK")
    void updateItem_owner_returns200() throws Exception {
        when(cartService.updateItem(any(), eq(itemId), any())).thenReturn(cartWithItem);

        mockMvc.perform(put("/api/cart/items/{id}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "quantity": 3 }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/cart/items/{id}: wrong user → 403 Forbidden")
    void updateItem_wrongUser_returns403() throws Exception {
        when(cartService.updateItem(any(), eq(itemId), any()))
                .thenThrow(new ForbiddenException("This cart item does not belong to you"));

        mockMvc.perform(put("/api/cart/items/{id}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "quantity": 3 }
                                """))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────
    // DELETE /api/cart/items/{itemId}
    // ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/cart/items/{id}: owner removes item → 200 OK")
    void removeItem_owner_returns200() throws Exception {
        when(cartService.removeItem(any(), eq(itemId))).thenReturn(emptyCart);

        mockMvc.perform(delete("/api/cart/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/cart/items/{id}: item not found → 404")
    void removeItem_notFound_returns404() throws Exception {
        when(cartService.removeItem(any(), any()))
                .thenThrow(new ResourceNotFoundException("Cart item not found"));

        mockMvc.perform(delete("/api/cart/items/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
