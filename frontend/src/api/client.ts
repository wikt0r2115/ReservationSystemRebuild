import type { ApiErrorResponse, Credentials } from '../types';

export class ApiRequestError extends Error {
  readonly status: number;
  readonly response: ApiErrorResponse;

  constructor(status: number, response: ApiErrorResponse) {
    super(response.message);
    this.status = status;
    this.response = response;
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'DELETE';
  body?: unknown;
  credentials?: Credentials;
};

export async function requestJson<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers();

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }

  if (options.credentials) {
    headers.set('Authorization', `Basic ${encodeBasicAuth(options.credentials)}`);
  }

  const response = await fetch(url, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  const contentType = response.headers.get('content-type') ?? '';
  const hasJson = contentType.includes('application/json');
  const payload = hasJson ? await response.json() : null;

  if (!response.ok) {
    throw new ApiRequestError(response.status, normalizeError(response.status, payload));
  }

  return payload as T;
}

function encodeBasicAuth(credentials: Credentials): string {
  return window.btoa(`${credentials.username}:${credentials.password}`);
}

function normalizeError(status: number, payload: unknown): ApiErrorResponse {
  if (isApiErrorResponse(payload)) {
    return payload;
  }

  if (status === 401) {
    return {
      code: 'UNAUTHORIZED',
      message: 'Authentication is required',
      details: [],
    };
  }

  if (status === 403) {
    return {
      code: 'FORBIDDEN',
      message: 'Insufficient permissions',
      details: [],
    };
  }

  return {
    code: 'REQUEST_FAILED',
    message: 'Request failed',
    details: [],
  };
}

function isApiErrorResponse(payload: unknown): payload is ApiErrorResponse {
  if (!payload || typeof payload !== 'object') {
    return false;
  }

  const candidate = payload as Partial<ApiErrorResponse>;
  return typeof candidate.code === 'string' && typeof candidate.message === 'string';
}
