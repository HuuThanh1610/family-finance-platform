const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
export const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

export class ApiError extends Error {
  status: number

  constructor(message: string, status = 500) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown
}

/**
 * Centralized HTTP client. When VITE_USE_MOCK=true, feature services
 * short-circuit to mock adapters and do not call this client.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

  if (!response.ok) {
    throw new ApiError(`API error ${response.status}`, response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export function getApiBaseUrl() {
  return API_BASE_URL
}
