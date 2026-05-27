import { apiConfig } from '../config';
import type { CreateOfferPayload, Offer } from '../types';
import { requestJson } from './client';
import { mockCreateOffer, mockListActiveOffers, mockListAdminOffers } from './mockApi';

export function listActiveOffers(): Promise<Offer[]> {
  if (apiConfig.useMockApi) {
    return mockListActiveOffers();
  }

  return requestJson<Offer[]>(`${apiConfig.offerBaseUrl}/api/v1/offers`);
}

export function listAdminOffers(accessToken: string): Promise<Offer[]> {
  if (apiConfig.useMockApi) {
    return mockListAdminOffers();
  }

  return requestJson<Offer[]>(`${apiConfig.offerBaseUrl}/api/v1/admin/offers`, { accessToken });
}

export function createOffer(payload: CreateOfferPayload, accessToken: string): Promise<Offer> {
  if (apiConfig.useMockApi) {
    return mockCreateOffer(payload);
  }

  return requestJson<Offer>(`${apiConfig.offerBaseUrl}/api/v1/admin/offers`, {
    method: 'POST',
    body: payload,
    accessToken,
  });
}
