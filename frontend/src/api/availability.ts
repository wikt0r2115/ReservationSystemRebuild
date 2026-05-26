import { apiConfig } from '../config';
import type { AvailabilitySlot, CreateAvailabilitySlotPayload, Credentials } from '../types';
import { requestJson } from './client';

export function listOpenSlots(offerId: number): Promise<AvailabilitySlot[]> {
  return requestJson<AvailabilitySlot[]>(
    `${apiConfig.availabilityBaseUrl}/api/v1/offers/${offerId}/availability`,
  );
}

export function createAvailabilitySlot(
  offerId: number,
  payload: CreateAvailabilitySlotPayload,
  credentials: Credentials,
): Promise<AvailabilitySlot> {
  return requestJson<AvailabilitySlot>(
    `${apiConfig.availabilityBaseUrl}/api/v1/admin/offers/${offerId}/availability`,
    {
      method: 'POST',
      body: payload,
      credentials,
    },
  );
}
