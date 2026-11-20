import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  apiRequest,
  configureApiAuth,
  ensureFreshAccessToken,
  getAccessToken,
  setAccessToken,
  setUnauthorizedHandler,
} from '@/services/api-client'

describe('ensureFreshAccessToken / apiRequest auth header', () => {
  beforeEach(() => {
    localStorage.clear()
    setUnauthorizedHandler(null)
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('missing mock', { status: 500 })),
    )
  })

  afterEach(() => {
    vi.mocked(fetch).mockReset()
  })

  it('falls back to getAccessToken when ensureAccessTokenFn returns null', async () => {
    configureApiAuth({
      ensureAccessToken: async () => null,
      tryRefreshSession: async () => false,
    })
    setAccessToken('temp-oidc-token')
    await expect(ensureFreshAccessToken()).resolves.toBe('temp-oidc-token')
    expect(getAccessToken()).toBe('temp-oidc-token')
  })

  it('sends Bearer from ff.accessToken before AuthSession exists', async () => {
    configureApiAuth({
      ensureAccessToken: async () => null,
      tryRefreshSession: async () => false,
    })
    setAccessToken('callback-access-token')
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ id: 'p1', email: 'a@b.c', displayName: 'A' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    await apiRequest('/api/v1/profiles/me', { skipAuthRetry: true })

    expect(fetchMock).toHaveBeenCalledOnce()
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit
    const headers = new Headers(init.headers)
    expect(headers.get('Authorization')).toBe('Bearer callback-access-token')
  })

  it('prefers ensureAccessTokenFn result over stored token', async () => {
    setAccessToken('stale')
    configureApiAuth({
      ensureAccessToken: async () => 'refreshed-token',
      tryRefreshSession: async () => true,
    })
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    await expect(ensureFreshAccessToken()).resolves.toBe('refreshed-token')
    await apiRequest('/api/v1/profiles/me')

    const firstCall = fetchMock.mock.calls[0]
    expect(firstCall).toBeDefined()
    const headers = new Headers((firstCall![1] as RequestInit).headers)
    expect(headers.get('Authorization')).toBe('Bearer refreshed-token')
  })

  it('retries once after tryRefreshSession succeeds', async () => {
    setAccessToken('expired')
    const tryRefresh = vi.fn(async () => {
      setAccessToken('new-token')
      return true
    })
    configureApiAuth({
      ensureAccessToken: async () => getAccessToken(),
      tryRefreshSession: tryRefresh,
    })
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(new Response('unauthorized', { status: 401 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ id: 'ok' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )

    const result = await apiRequest<{ id: string }>('/api/v1/profiles/me')

    expect(result.id).toBe('ok')
    expect(tryRefresh).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledTimes(2)
    const secondCall = fetchMock.mock.calls[1]
    expect(secondCall).toBeDefined()
    const secondHeaders = new Headers((secondCall![1] as RequestInit).headers)
    expect(secondHeaders.get('Authorization')).toBe('Bearer new-token')
  })

  it('throws ApiError 401 without treating response as success', async () => {
    configureApiAuth({
      ensureAccessToken: async () => getAccessToken(),
      tryRefreshSession: async () => false,
    })
    setAccessToken('bad')
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    vi.mocked(fetch).mockResolvedValue(new Response('nope', { status: 401 }))

    await expect(apiRequest('/api/v1/profiles/me', { skipAuthRetry: true })).rejects.toMatchObject({
      status: 401,
    } satisfies Partial<ApiError>)
    expect(onUnauthorized).toHaveBeenCalledOnce()
    expect(getAccessToken()).toBeNull()
  })
})
