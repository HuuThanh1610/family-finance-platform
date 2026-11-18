const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/** Mock chỉ bật khi VITE_USE_MOCK=true (chuỗi). Mặc định: API thật. */
export const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'false') === 'true'

const TOKEN_KEY = 'ff.accessToken'

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
  /** Bỏ qua refresh/retry (tránh vòng lặp). */
  skipAuthRetry?: boolean
}

type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler | null = null
let ensureAccessTokenFn: (() => Promise<string | null>) | null = null
let tryRefreshSessionFn: (() => Promise<boolean>) | null = null

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler
}

export function configureApiAuth(hooks: {
  ensureAccessToken: () => Promise<string | null>
  tryRefreshSession: () => Promise<boolean>
}) {
  ensureAccessTokenFn = hooks.ensureAccessToken
  tryRefreshSessionFn = hooks.tryRefreshSession
}

export function setAccessToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

async function ensureFreshAccessToken(): Promise<string | null> {
  if (USE_MOCK) return getAccessToken()
  if (ensureAccessTokenFn) return ensureAccessTokenFn()
  return getAccessToken()
}

/**
 * HTTP client tập trung. Feature services dùng mock adapter khi USE_MOCK=true.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  const token = await ensureFreshAccessToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

  if (response.status === 401 && !options.skipAuthRetry && !USE_MOCK && tryRefreshSessionFn) {
    const refreshed = await tryRefreshSessionFn()
    if (refreshed) {
      return apiRequest<T>(path, { ...options, skipAuthRetry: true })
    }
    setAccessToken(null)
    unauthorizedHandler?.()
    throw new ApiError('Phiên đăng nhập hết hạn', 401)
  }

  if (response.status === 401) {
    setAccessToken(null)
    unauthorizedHandler?.()
    throw new ApiError('Phiên đăng nhập hết hạn', 401)
  }

  if (response.status === 403) {
    throw new ApiError('Bạn không có quyền thực hiện thao tác này', 403)
  }

  if (!response.ok) {
    let detail = `API error ${response.status}`
    try {
      const payload = (await response.json()) as { message?: string; detail?: string }
      if (payload.message) detail = payload.message
      else if (payload.detail) detail = payload.detail
    } catch {
      /* ignore */
    }
    throw new ApiError(detail, response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) {
    return undefined as T
  }

  return (await response.json()) as T
}

export function getApiBaseUrl() {
  return API_BASE_URL
}