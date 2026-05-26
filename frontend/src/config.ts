export const apiConfig = {
  offerBaseUrl: import.meta.env.VITE_OFFER_API_BASE ?? '/offer-api',
  availabilityBaseUrl: import.meta.env.VITE_AVAILABILITY_API_BASE ?? '/availability-api',
  bookingBaseUrl: import.meta.env.VITE_BOOKING_API_BASE ?? '/booking-api',
  authBaseUrl: import.meta.env.VITE_AUTH_API_BASE ?? '/auth-api',
  customerEmail: import.meta.env.VITE_CUSTOMER_EMAIL ?? 'jan@example.com',
  customerPassword: import.meta.env.VITE_CUSTOMER_PASSWORD ?? 'customer123',
  adminEmail: import.meta.env.VITE_ADMIN_EMAIL ?? 'admin@example.com',
  adminPassword: import.meta.env.VITE_ADMIN_PASSWORD ?? 'admin123',
};
