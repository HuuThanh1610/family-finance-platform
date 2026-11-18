import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { LoadingState } from '@/components/common/LoadingState'
import { useAuth } from '@/features/auth/AuthContext'
import { familyApi } from '@/services/family-finance-api'
import { setFamilyRuntimeContext } from '@/services/family-context-store'
import type { Family } from '@/types/domain'

const FAMILY_ID_KEY = 'ff.activeFamilyId'

interface FamilyContextValue {
  family: Family | null
  familyId: string | null
  memberId: string | null
  loading: boolean
  error: string | null
  reload: () => Promise<void>
  setFamilyId: (id: string) => void
}

const FamilyContext = createContext<FamilyContextValue | null>(null)

export function FamilyProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, session } = useAuth()
  const [family, setFamily] = useState<Family | null>(null)
  const [familyId, setFamilyIdState] = useState<string | null>(() => localStorage.getItem(FAMILY_ID_KEY))
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const memberId = useMemo(() => {
    if (!family || !session?.user.id) return null
    return family.members.find((m) => m.userId === session.user.id)?.id ?? null
  }, [family, session?.user.id])

  const syncRuntime = useCallback(
    (nextFamily: Family | null, nextFamilyId: string | null) => {
      const nextMember =
        nextFamily && session?.user.id
          ? (nextFamily.members.find((m) => m.userId === session.user.id)?.id ?? null)
          : null
      setFamilyRuntimeContext({
        familyId: nextFamilyId,
        memberId: nextMember,
        userId: session?.user.id ?? null,
      })
    },
    [session?.user.id],
  )

  const load = useCallback(async () => {
    if (!isAuthenticated) {
      setFamily(null)
      syncRuntime(null, null)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const next = await familyApi.getFamily(familyId ?? undefined)
      const resolvedId = next.id || familyId
      if (resolvedId) {
        localStorage.setItem(FAMILY_ID_KEY, resolvedId)
        setFamilyIdState(resolvedId)
      }
      setFamily(next.id ? next : null)
      syncRuntime(next.id ? next : null, resolvedId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không tải được gia đình')
      setFamily(null)
      syncRuntime(null, familyId)
    } finally {
      setLoading(false)
    }
  }, [familyId, isAuthenticated, syncRuntime])

  useEffect(() => {
    void load()
  }, [load])

  const setFamilyId = useCallback(
    (id: string) => {
      localStorage.setItem(FAMILY_ID_KEY, id)
      setFamilyIdState(id)
    },
    [],
  )

  const value = useMemo<FamilyContextValue>(
    () => ({
      family,
      familyId: family?.id ?? familyId,
      memberId,
      loading,
      error,
      reload: load,
      setFamilyId,
    }),
    [family, familyId, memberId, loading, error, load, setFamilyId],
  )

  if (isAuthenticated && loading && !family) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center p-6">
        <LoadingState label="Đang tải ngữ cảnh gia đình..." />
      </div>
    )
  }

  return <FamilyContext.Provider value={value}>{children}</FamilyContext.Provider>
}

export function useFamily() {
  const ctx = useContext(FamilyContext)
  if (!ctx) throw new Error('useFamily must be used within FamilyProvider')
  return ctx
}