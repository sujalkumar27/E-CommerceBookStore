package com.bookstore.controller;

import com.bookstore.dto.book.BookSummaryDto;
import com.bookstore.model.User;
import com.bookstore.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * RecommendationController — Personalised Book Recommendations
 * ============================================================
 *
 * WHAT THIS IS:
 * Returns up to 8 book recommendations for the logged-in user.
 * Requires authentication — recommendations are personalised.
 *
 * ENDPOINT:
 *   GET /api/recommendations
 *
 * ALGORITHM (D-010):
 *   1. Same category as past purchases
 *   2. Same author as past purchases
 *   3. Newest catalogue additions (fallback / padding)
 *   Books already ordered are excluded.
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * GET /api/recommendations
     *
     * Returns up to 8 personalised book recommendations.
     *
     * RESPONSE 200:
     * {
     *   "books": [ ...up to 8 BookSummaryDto objects... ]
     * }
     *
     * RESPONSE 401: not logged in
     */
    @GetMapping
    public ResponseEntity<Map<String, List<BookSummaryDto>>> getRecommendations(
            @AuthenticationPrincipal User user) {
        List<BookSummaryDto> recommendations = recommendationService.getRecommendations(user);
        return ResponseEntity.ok(Map.of("books", recommendations));
    }
}
