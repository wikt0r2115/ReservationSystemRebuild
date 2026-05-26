import { apiConfig } from '../config';
import type { CreateReservationPayload, Credentials, Reservation } from '../types';
import { requestJson } from './client';

export function createReservation(
  payload: CreateReservationPayload,
  credentials: Credentials,
): Promise<Reservation> {
  return requestJson<Reservation>(`${apiConfig.bookingBaseUrl}/api/v1/reservations`, {
    method: 'POST',
    body: payload,
    credentials,
  });
}

export function cancelReservation(reservationId: number, credentials: Credentials): Promise<Reservation> {
  return requestJson<Reservation>(`${apiConfig.bookingBaseUrl}/api/v1/reservations/${reservationId}`, {
    method: 'DELETE',
    credentials,
  });
}

export function listAdminReservations(credentials: Credentials): Promise<Reservation[]> {
  return requestJson<Reservation[]>(`${apiConfig.bookingBaseUrl}/api/v1/admin/reservations`, {
    credentials,
  });
}
