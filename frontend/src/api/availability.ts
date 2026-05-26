import { apiConfig } from '../config';
import type { AvailabilitySlot, CreateAvailabilitySlotPayload } from '../types';
import { requestJson } from './client';

export function listOpenSlots(offerId: number): Promise<AvailabilitySlot[]> {
  return requestJson<AvailabilitySlot[]>(
    `${apiConfig.availabilityBaseUrl}/api/v1/offers/${offerId}/availability`,
  );
}

export function listAdminSlots(
  offerId: number,
  accessToken: string,
): Promise<AvailabilitySlot[]> {
  return requestJson<AvailabilitySlot[]>(
    `${apiConfig.availabilityBaseUrl}/api/v1/admin/offers/${offerId}/availability`,
    { accessToken },
  );
}

export function createAvailabilitySlot(
  offerId: number,
  payload: CreateAvailabilitySlotPayload,
  accessToken: string,
): Promise<AvailabilitySlot> {
  return requestJson<AvailabilitySlot>(
    `${apiConfig.availabilityBaseUrl}/api/v1/admin/offers/${offerId}/availability`,
    {
      method: 'POST',
      body: payload,
      accessToken,
    },
  );
}

export function cancelAvailabilitySlot(
  slotId: number,
  accessToken: string,
): Promise<AvailabilitySlot> {
  return requestJson<AvailabilitySlot>(
    `${apiConfig.availabilityBaseUrl}/api/v1/admin/availability/${slotId}`,
    {
      method: 'DELETE',
      accessToken,
    },
  );
}
