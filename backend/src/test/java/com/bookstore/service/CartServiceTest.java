package com.bookstore.service;

import com.bookstore.dto.cart.AddItemRequest;
import com.bookstore.dto.cart.CartDto;
import com.bookstore.dto.cart.UpdateItemRequest;
import com.bookstore.exception.ForbiddenException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.model.CartItem;
import com.bookstore.model.Category;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 * CartServiceTest — Unit Tests for Shopping Cart Logic
 * ============================================================
 *
 * WHAT WE ARE TESTING:
 *   getCart()    → returns CartDto with correct totals
 *   addItem()    → adds a new item OR increases quantity if duplicate
 *   updateItem() → updates quantity; rejects another user's item
 *   removeItem() → deletes item; rejects another user's item
 *
 * KEY SCENARIOS TO COVER:
 *   - getCart with 0 items → empty CartDto, total = 0
 *   - getCart with 2 items → correct lineTotals and cartTotal
 *   - addItem for a new book → new CartItem saved
 *   - addItem for an existing book → quantity is increased, not duplicated
 *   - addItem with unknown bookId → ResourceNotFoundException
 *   - updateItem owned by user → quantity updated
 *   - updateItem owned by OTHER user → ForbiddenException
 *   - removeItem owned by user → deleted
 *   - removeItem owned by OTHER user → ForbiddenException
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private BookRepository bookRepository;

    @InjectMocks
    private CartService cartService;

    // ── Test fixtures ──
    private User alice;
    private User bob;
    private Book book;
    private CartItem aliceItem;

    @BeforeEach
    void setUp() {
        alice = makeUser("alice@test.com");
        bob   = makeUser("bob@test.com");

        Category fiction = new Category("Fiction", 3);
        setId(fiction, UUID.randomUUID());

        book = new Book();
        setId(book, UUID.randomUUID());
        book.setTitle("Clean Code");
        book.setAuthor("Robert Martin");
        book.setPrice(new BigDecimal("799.00"));
        book.setStock(5);
        book.setCategory(fiction);
        book.setPublisher("Prentice Hall");

        aliceItem = new CartItem(alice, book, 2);
        setId(aliceItem, UUID.randomUUID());
    }

    // ─────────────────────────────────────────────────────────
    // getCart()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCart: empty cart returns cartTotal of zero")
    void getCart_empty_returnsZeroTotal() {
        when(cartRepository.findAllByUserOrderByAddedAtAsc(alice)).thenReturn(List.of());

        CartDto result = cartService.getCart(alice);

        assertThat(result.items()).isEmpty();
        assertThat(result.cartTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getCart: two items → correct lineTotals and cartTotal")
    void getCart_twoItems_returnsCorrectTotals() {
        // Second item: same book, quantity 1 → lineTotal = 799.00
        CartItem item2 = new CartItem(alice, book, 1);
        setId(item2, UUID.randomUUID());

        when(cartRepository.findAllByUserOrderByAddedAtAsc(alice))
                .thenReturn(List.of(aliceItem, item2));

        CartDto result = cartService.getCart(alice);

        // aliceItem: 799.00 × 2 = 1598.00
        // item2:     799.00 × 1 =  799.00
        // cartTotal:              2397.00
        assertThat(result.items()).hasSize(2);
        assertThat(result.cartTotal()).isEqualByComparingTo(new BigDecimal("2397.00"));
    }

    // ─────────────────────────────────────────────────────────
    // addItem()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("addItem: new book → creates a new CartItem row")
    void addItem_newBook_savesNewCartItem() {
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        // Book is NOT yet in the cart
        when(cartRepository.findByUserAndBookId(alice, book.getId())).thenReturn(Optional.empty());
        // After adding, return cart with the new item
        when(cartRepository.findAllByUserOrderByAddedAtAsc(alice)).thenReturn(List.of(aliceItem));

        AddItemRequest request = new AddItemRequest(book.getId(), 2);
        CartDto result = cartService.addItem(alice, request);

        // A new CartItem must have been saved
        verify(cartRepository).save(any(CartItem.class));
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("addItem: duplicate book → quantities are summed, no new row saved")
    void addItem_existingBook_sumsQuantity() {
        // Arrange: book is already in the cart with quantity=2
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(cartRepository.findByUserAndBookId(alice, book.getId()))
                .thenReturn(Optional.of(aliceItem));
        when(cartRepository.findAllByUserOrderByAddedAtAsc(alice)).thenReturn(List.of(aliceItem));

        AddItemRequest request = new AddItemRequest(book.getId(), 3);
        cartService.addItem(alice, request);

        // Quantity should now be 2 + 3 = 5
        assertThat(aliceItem.getQuantity()).isEqualTo(5);
        // No NEW CartItem row should be saved (we updated the existing one)
        verify(cartRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addItem: unknown bookId → ResourceNotFoundException")
    void addItem_unknownBook_throwsResourceNotFoundException() {
        UUID unknownBookId = UUID.randomUUID();
        when(bookRepository.findById(unknownBookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(alice, new AddItemRequest(unknownBookId, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────
    // updateItem()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateItem: owner updates quantity successfully")
    void updateItem_owner_updatesQuantity() {
        when(cartRepository.findById(aliceItem.getId())).thenReturn(Optional.of(aliceItem));
        when(cartRepository.findAllByUserOrderByAddedAtAsc(alice)).thenReturn(List.of(aliceItem));

        cartService.updateItem(alice, aliceItem.getId(), new UpdateItemRequest(5));

        assertThat(aliceItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("updateItem: non-owner → ForbiddenException")
    void updateItem_nonOwner_throwsForbiddenException() {
        // aliceItem belongs to alice, but bob is trying to update it
        when(cartRepository.findById(aliceItem.getId())).thenReturn(Optional.of(aliceItem));

        assertThatThrownBy(() -> cartService.updateItem(bob, aliceItem.getId(), new UpdateItemRequest(5)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("updateItem: item not found → ResourceNotFoundException")
    void updateItem_notFound_throwsResourceNotFoundException() {
        UUID unknownItemId = UUID.randomUUID();
        when(cartRepository.findById(unknownItemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItem(alice, unknownItemId, new UpdateItemRequest(3)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────
    // removeItem()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeItem: owner removes item successfully")
    void removeItem_owner_deletesItem() {
        when(cartRepository.findById(aliceItem.getId())).thenReturn(Optional.of(aliceItem));
        when(cartRepository.findAllByUserOrderByAddedAtAsc(alice)).thenReturn(List.of());

        CartDto result = cartService.removeItem(alice, aliceItem.getId());

        verify(cartRepository).delete(aliceItem);
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("removeItem: non-owner → ForbiddenException, delete never called")
    void removeItem_nonOwner_throwsForbiddenException() {
        when(cartRepository.findById(aliceItem.getId())).thenReturn(Optional.of(aliceItem));

        assertThatThrownBy(() -> cartService.removeItem(bob, aliceItem.getId()))
                .isInstanceOf(ForbiddenException.class);

        verify(cartRepository, never()).delete(any());
    }

    // ─────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────

    private User makeUser(String email) {
        User u = new User("Test User", email, "hash");
        setId(u, UUID.randomUUID());
        return u;
    }

    private void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set id on " + entity.getClass().getSimpleName(), e);
        }
    }
}

