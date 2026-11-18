export interface JwtClaims {
  sub?: string
  email?: string
  name?: string
  preferred_username?: string
  exp?: number
}

export function parseJwtPayload(token: string): JwtClaims {
  const parts = token.split('.')
  if (parts.length < 2) {
    throw new Error('JWT không hợp lệ')
  }
  const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
  const json = atob(padded)
  return JSON.parse(json) as JwtClaims
}

export function tokenExpiresAtMs(token: string): number | undefined {
  const exp = parseJwtPayload(token).exp
  if (exp === undefined) return undefined
  return exp * 1000
}