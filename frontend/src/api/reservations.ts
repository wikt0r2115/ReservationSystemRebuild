import { apiConfig } from '../config';
import type { CreateReservationPayload, Reservation } from '../types';
import { requestJson } from './client';

export function createReservation(
  payload: CreateReservationPayload,
  accessToken: string,
): Promise<Reservation> {
  return requestJson<Reservation>(`${apiConfig.bookingBaseUrl}/api/v1/reservations`, {
    method: 'POST',
    body: payload,
    accessToken,
  });
}

export function cancelReservation(reservationId: number, accessToken: string): Promise<Reservation> {
  return requestJson<Reservation>(`${apiConfig.bookingBaseUrl}/api/v1/reservations/${reservationId}`, {
    method: 'DELETE',
    accessToken,
  });
}

export function listAdminReservations(accessToken: string): Promise<Reservation[]> {
  return requestJson<Reservation[]>(`${apiConfig.bookingBaseUrl}/api/v1/admin/reservations`, {
    accessToken,
  });
}
