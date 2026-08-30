// recommendationApi.js — Fetches personalised book recommendations.
// Endpoint: GET /api/recommendations
//
// The backend returns books based on the user's order history.
// Requires authentication (the JWT is attached by the axiosClient interceptor).

import axiosClient from './axiosClient';

/**
 * Get personalised book recommendations for the logged-in user.
 * @returns Promise<BookSummary[]>
 */
export const getRecommendations = () =>
  axiosClient.get('/api/recommendations');
