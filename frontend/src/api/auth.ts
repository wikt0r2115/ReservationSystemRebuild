import { apiConfig } from '../config';
import type {
  AuthTokenResponse,
  LoginCredentials,
  RegisterCustomerPayload,
  UserAccountResponse,
} from '../types';
import { requestJson } from './client';

export function registerCustomer(payload: RegisterCustomerPayload): Promise<UserAccountResponse> {
  return requestJson<UserAccountResponse>(`${apiConfig.authBaseUrl}/api/v1/auth/register`, {
    method: 'POST',
    body: payload,
  });
}

export function login(credentials: LoginCredentials): Promise<AuthTokenResponse> {
  return requestJson<AuthTokenResponse>(`${apiConfig.authBaseUrl}/api/v1/auth/login`, {
    method: 'POST',
    body: credentials,
  });
}
