import { apiConfig } from '../config';
import type {
  AuthTokenResponse,
  LoginCredentials,
  RegisterCustomerPayload,
  UserAccountResponse,
} from '../types';
import { requestJson } from './client';
import { mockLogin, mockRegisterCustomer } from './mockApi';

export function registerCustomer(payload: RegisterCustomerPayload): Promise<UserAccountResponse> {
  if (apiConfig.useMockApi) {
    return mockRegisterCustomer(payload);
  }

  return requestJson<UserAccountResponse>(`${apiConfig.authBaseUrl}/api/v1/auth/register`, {
    method: 'POST',
    body: payload,
  });
}

export function login(credentials: LoginCredentials): Promise<AuthTokenResponse> {
  if (apiConfig.useMockApi) {
    return mockLogin(credentials);
  }

  return requestJson<AuthTokenResponse>(`${apiConfig.authBaseUrl}/api/v1/auth/login`, {
    method: 'POST',
    body: credentials,
  });
}
