import { apiConfig } from '../config';
import type { CreateReservationPayload, Reservation } from '../types';
import { requestJson } from './client';
import {
  mockCancelReservation,
  mockConfirmReservation,
  mockCreateReservation,
  mockListAdminReservations,
  mockRejectReservation,
} from './mockApi';

export function createReservation(
  payload: CreateReservationPayload,
  accessToken: string,
): Promise<Reservation> {
  if (apiConfig.useMockApi) {
    return mockCreateReservation(payload);
  }

  return requestJson<Reservation>(`${apiConfig.bookingBaseUrl}/api/v1/reservations`, {
    method: 'POST',
    body: payload,
    accessToken,
  });
}

export function cancelReservation(reservationId: number, accessToken: string): Promise<Reservation> {
  if (apiConfig.useMockApi) {
    return mockCancelReservation(reservationId);
  }

  return requestJson<Reservation>(`${apiConfig.bookingBaseUrl}/api/v1/reservations/${reservationId}`, {
    method: 'DELETE',
    accessToken,
  });
}

export function listAdminReservations(accessToken: string): Promise<Reservation[]> {
  if (apiConfig.useMockApi) {
    return mockListAdminReservations();
  }

  return requestJson<Reservation[]>(`${apiConfig.bookingBaseUrl}/api/v1/admin/reservations`, {
    accessToken,
  });
}

export function confirmReservation(reservationId: number, accessToken: string): Promise<Reservation> {
  if (apiConfig.useMockApi) {
    return mockConfirmReservation(reservationId);
  }

  return requestJson<Reservation>(
    `${apiConfig.bookingBaseUrl}/api/v1/admin/reservations/${reservationId}/confirm`,
    {
      method: 'POST',
      accessToken,
    },
  );
}

export function rejectReservation(reservationId: number, accessToken: string): Promise<Reservation> {
  if (apiConfig.useMockApi) {
    return mockRejectReservation(reservationId);
  }

  return requestJson<Reservation>(
    `${apiConfig.bookingBaseUrl}/api/v1/admin/reservations/${reservationId}/reject`,
    {
      method: 'POST',
      accessToken,
    },
  );
}
