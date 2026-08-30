package com.bookstore.controller;

import com.bookstore.dto.book.BookDetailDto;
import com.bookstore.dto.book.BookSummaryDto;
import com.bookstore.exception.GlobalExceptionHandler;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Category;
import com.bookstore.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ============================================================
 * BookControllerTest — Integration Tests for Catalogue Endpoints
 * ============================================================
 *
 * All catalogue endpoints are public — no JWT needed (guests can browse).
 * TestSecurityConfig disables auth/CSRF entirely in the test slice.
 */
@WebMvcTest(BookController.class)
@Import(GlobalExceptionHandler.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class BookControllerTest extends BaseControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  BookService bookService;

    private final UUID BOOK_ID = UUID.randomUUID();
    private final UUID CAT_ID  = UUID.randomUUID();

    private BookSummaryDto makeSummary(String title) {
        return new BookSummaryDto(
                UUID.randomUUID(), title, "Test Author", "Test Publisher",
                new BookSummaryDto.CategoryInfo(CAT_ID, "Fiction"),
                new BigDecimal("499.00"), true, null, 2020
        );
    }

    private BookDetailDto makeDetail() {
        return new BookDetailDto(
                BOOK_ID, "Clean Code", "Robert Martin", "978-0132350884",
                "A handbook of agile software craftsmanship.",
                new BigDecimal("799.00"), true, null, 2008, "Prentice Hall",
                new BookDetailDto.CategoryInfo(CAT_ID, "Technology", 5),
                5, List.of(makeSummary("Related Book"))
        );
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/books
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/books: no params → 200 OK with paginated content")
    void getBooks_noParams_returns200() throws Exception {
        var page = new PageImpl<>(List.of(makeSummary("Clean Code")), PageRequest.of(0, 20), 1);
        when(bookService.getBooks(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/books: no auth required (public endpoint)")
    void getBooks_noAuth_returns200() throws Exception {
        var page = new PageImpl<BookSummaryDto>(List.of(), PageRequest.of(0, 20), 0);
        when(bookService.getBooks(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/books?search=clean: passes search term to service")
    void getBooks_withSearchParam_returns200() throws Exception {
        var page = new PageImpl<>(List.of(makeSummary("Clean Code")), PageRequest.of(0, 20), 1);
        when(bookService.getBooks(eq("clean"), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/books").param("search", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
    }

    @Test
    @DisplayName("GET /api/books: no results → 200 OK with empty content array")
    void getBooks_noResults_returns200Empty() throws Exception {
        var empty = new PageImpl<BookSummaryDto>(List.of(), PageRequest.of(0, 20), 0);
        when(bookService.getBooks(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(empty);

        mockMvc.perform(get("/api/books").param("search", "zzznomatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/books?available=true: passes available=true to service")
    void getBooks_availableFilter_passedToService() throws Exception {
        var page = new PageImpl<>(List.of(makeSummary("In Stock Book")), PageRequest.of(0, 20), 1);
        when(bookService.getBooks(any(), any(), any(), any(), any(), eq(true), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/books").param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("In Stock Book"));
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/books/{id}
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/books/{id}: known ID → 200 OK with detail + relatedBooks")
    void getBookById_found_returns200() throws Exception {
        when(bookService.getBookById(BOOK_ID)).thenReturn(makeDetail());

        mockMvc.perform(get("/api/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.relatedBooks[0].title").value("Related Book"));
    }

    @Test
    @DisplayName("GET /api/books/{id}: unknown ID → 404 Not Found")
    void getBookById_notFound_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(bookService.getBookById(unknownId))
                .thenThrow(new ResourceNotFoundException("Book not found with ID: " + unknownId));

        mockMvc.perform(get("/api/books/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/categories
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/categories: returns list of categories")
    void getAllCategories_returns200WithList() throws Exception {
        when(bookService.getAllCategories())
                .thenReturn(List.of(new Category("Fiction", 3), new Category("Technology", 5)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fiction"))
                .andExpect(jsonPath("$[1].name").value("Technology"));
    }
}
