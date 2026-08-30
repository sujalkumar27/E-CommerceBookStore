// bookApi.js — API calls for the book catalogue and categories.
// Endpoints:
//   GET /api/books           - paginated + filtered book list
//   GET /api/books/:id       - full detail for one book
//   GET /api/categories      - all available categories

import axiosClient from './axiosClient';

/**
 * Get a paginated, filterable list of books.
 * @param {Object} params - Query parameters (all optional):
 *   search, categoryId, publisher, minPrice, maxPrice, available, page, size
 * @returns Promise<{ content: BookSummary[], totalPages, totalElements, ... }>
 */
export const getBooks = (params = {}) =>
  axiosClient.get('/api/books', { params });

/**
 * Get full details for one book by its UUID.
 * @param {string} id - Book UUID
 * @returns Promise<BookDetail>
 */
export const getBookById = (id) =>
  axiosClient.get(`/api/books/${id}`);

/**
 * Get all book categories (for the filter panel).
 * @returns Promise<Category[]>
 */
export const getCategories = () =>
  axiosClient.get('/api/categories');
