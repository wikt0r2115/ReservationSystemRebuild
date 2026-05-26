import { apiConfig } from '../config';
import type { CreateOfferPayload, Offer } from '../types';
import { requestJson } from './client';

export function listActiveOffers(): Promise<Offer[]> {
  return requestJson<Offer[]>(`${apiConfig.offerBaseUrl}/api/v1/offers`);
}

export function listAdminOffers(accessToken: string): Promise<Offer[]> {
  return requestJson<Offer[]>(`${apiConfig.offerBaseUrl}/api/v1/admin/offers`, { accessToken });
}

export function createOffer(payload: CreateOfferPayload, accessToken: string): Promise<Offer> {
  return requestJson<Offer>(`${apiConfig.offerBaseUrl}/api/v1/admin/offers`, {
    method: 'POST',
    body: payload,
    accessToken,
  });
}
