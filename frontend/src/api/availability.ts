import { apiConfig } from '../config';
import type { AvailabilitySlot, CreateAvailabilitySlotPayload } from '../types';
import { requestJson } from './client';
import {
  mockCreateAvailabilitySlot,
  mockListAdminSlots,
  mockListOpenSlots,
} from './mockApi';

export function listOpenSlots(offerId: number): Promise<AvailabilitySlot[]> {
  if (apiConfig.useMockApi) {
    return mockListOpenSlots(offerId);
  }

  return requestJson<AvailabilitySlot[]>(
    `${apiConfig.availabilityBaseUrl}/api/v1/offers/${offerId}/availability`,
  );
}

export function listAdminSlots(
  offerId: number,
  accessToken: string,
): Promise<AvailabilitySlot[]> {
  if (apiConfig.useMockApi) {
    return mockListAdminSlots(offerId);
  }

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
  if (apiConfig.useMockApi) {
    return mockCreateAvailabilitySlot(offerId, payload);
  }

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
