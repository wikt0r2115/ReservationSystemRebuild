import { apiConfig } from '../config';
import type { CreateOfferPayload, Credentials, Offer } from '../types';
import { requestJson } from './client';

export function listActiveOffers(): Promise<Offer[]> {
  return requestJson<Offer[]>(`${apiConfig.offerBaseUrl}/api/v1/offers`);
}

export function listAdminOffers(credentials: Credentials): Promise<Offer[]> {
  return requestJson<Offer[]>(`${apiConfig.offerBaseUrl}/api/v1/admin/offers`, { credentials });
}

export function createOffer(payload: CreateOfferPayload, credentials: Credentials): Promise<Offer> {
  return requestJson<Offer>(`${apiConfig.offerBaseUrl}/api/v1/admin/offers`, {
    method: 'POST',
    body: payload,
    credentials,
  });
}
