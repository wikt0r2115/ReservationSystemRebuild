export const apiConfig = {
  offerBaseUrl: import.meta.env.VITE_OFFER_API_BASE ?? '/offer-api',
  availabilityBaseUrl: import.meta.env.VITE_AVAILABILITY_API_BASE ?? '/availability-api',
  bookingBaseUrl: import.meta.env.VITE_BOOKING_API_BASE ?? '/booking-api',
  customerUsername: import.meta.env.VITE_CUSTOMER_USERNAME ?? 'customer',
  customerPassword: import.meta.env.VITE_CUSTOMER_PASSWORD ?? 'customer123',
};
