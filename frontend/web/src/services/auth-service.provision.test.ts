import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const exchangeAuthorizationCode = vi.fn()
const tokensNeedRefresh = vi.fn(() => false)
const refreshTokens = vi.fn()
const beginLoginRedirect = vi.fn()
const buildLogoutRedirect = vi.fn(() => 'http://logout')

vi.mock('@/services/keycloak-oidc', () => ({
  exchangeAuthorizationCode: (code: string, state: string | null) =>
    exchangeAuthorizationCode(code, state),
  tokensNeedRefresh: (expiresAt?: number) => tokensNeedRefresh(expiresAt),
  refreshTokens: (refreshToken: string) => refreshTokens(refreshToken),
  beginLoginRedirect: () => beginLoginRedirect(),
  buildLogoutRedirect: (idToken: string) => buildLogoutRedirect(idToken),
}))

import { authService } from '@/services/auth-service'
import {
  configureApiAuth,
  getAccessToken,
  setAccessToken,
  setUnauthorizedHandler,
} from '@/services/api-client'

/** Minimal JWT: header.payload.sig with sub claim (base64url). */
function jwtWithSub(sub: string): string {
  const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  const payload = btoa(JSON.stringify({ sub, email: 'owner@example.local', name: 'Owner' }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
  return `${header}.${payload}.sig`
}

describe('authService.completeOidcLogin provisioning', () => {
  beforeEach(() => {
    localStorage.clear()
    setUnauthorizedHandler(null)
    configureApiAuth({
      ensureAccessToken: () => authService.ensureAccessToken(),
      tryRefreshSession: () => authService.tryRefreshSession(),
    })
    vi.stubGlobal('fetch', vi.fn())
    exchangeAuthorizationCode.mockReset()
  })

  afterEach(() => {
    vi.mocked(fetch).mockReset()
    localStorage.clear()
    setAccessToken(null)
  })

  it('sends Bearer on /profiles/me before AuthSession is persisted', async () => {
    const accessToken = jwtWithSub('kc-sub-1')
    exchangeAuthorizationCode.mockResolvedValue({
      accessToken,
      refreshToken: 'rt',
      idToken: 'id',
      expiresAt: Date.now() + 60_000,
    })
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          id: '11111111-1111-4111-8111-111111111111',
          email: 'owner@example.local',
          displayName: 'Owner',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )

    await authService.completeOidcLogin('code', 'state')

    expect(fetchMock.mock.calls[0]?.[0]).toEqual(expect.stringContaining('/api/v1/profiles/me'))
    const firstCall = fetchMock.mock.calls[0]
    expect(firstCall).toBeDefined()
    const headers = new Headers((firstCall![1] as RequestInit).headers)
    expect(headers.get('Authorization')).toBe(`Bearer ${accessToken}`)
  })

  it('on 404 provisions with the same Bearer token', async () => {
    const accessToken = jwtWithSub('kc-sub-2')
    exchangeAuthorizationCode.mockResolvedValue({
      accessToken,
      refreshToken: 'rt',
      idToken: 'id',
      expiresAt: Date.now() + 60_000,
    })
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: 'missing' }), { status: 404 }))
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            id: '22222222-2222-4222-8222-222222222222',
            keycloakSub: 'kc-sub-2',
            email: 'owner@example.local',
            displayName: 'Owner',
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
      )

    await authService.completeOidcLogin('code', 'state')

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(String(fetchMock.mock.calls[1]?.[0])).toContain('/api/v1/profiles/provision')
    const provisionCall = fetchMock.mock.calls[1]
    expect(provisionCall).toBeDefined()
    const provisionHeaders = new Headers((provisionCall![1] as RequestInit).headers)
    expect(provisionHeaders.get('Authorization')).toBe(`Bearer ${accessToken}`)
    expect((provisionCall![1] as RequestInit).method).toBe('POST')
  })

  it('does not provision when /profiles/me returns 401', async () => {
    const accessToken = jwtWithSub('kc-sub-3')
    exchangeAuthorizationCode.mockResolvedValue({
      accessToken,
      refreshToken: 'rt',
      idToken: 'id',
      expiresAt: Date.now() + 60_000,
    })
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(new Response('unauthorized', { status: 401 }))

    await expect(authService.completeOidcLogin('code', 'state')).rejects.toMatchObject({
      status: 401,
    })
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain('/api/v1/profiles/me')
    expect(getAccessToken()).toBeNull()
  })

  it('ensureAccessToken falls back to temporary ff.accessToken', async () => {
    setAccessToken('pre-session-token')
    await expect(authService.ensureAccessToken()).resolves.toBe('pre-session-token')
  })
})
