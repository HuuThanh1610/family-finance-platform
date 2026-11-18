import { delay } from '@/lib/utils'
import {
  allowancePolicies,
  allowanceRequests,
  budget,
  dashboardSummary,
  demoFamily,
  expenses,
  inventoryItems,
  monthlyTrends,
  notificationPreferences,
  notifications,
  paymentOrders,
  shoppingList,
  spendingByCategory,
  spendingByMember,
  transactions,
  wallets,
} from '@/mocks/data'
import { USE_MOCK, apiRequest } from '@/services/api-client'
import {
  getActiveFamilyId,
  getActiveMemberId,
  getActiveUserId,
  requireActiveFamilyId,
} from '@/services/family-context-store'
import { getPaymentProvider } from '@/services/payment-provider'
import type {
  AllowancePolicy,
  AllowanceRequest,
  AppNotification,
  Budget,
  CategorySpendPoint,
  DashboardSummary,
  Expense,
  Family,
  FamilyRole,
  InventoryItem,
  MemberSpendPoint,
  MonthlyTrendPoint,
  NotificationPreference,
  NotificationType,
  PaymentOrder,
  PaymentProviderCode,
  ShoppingListItem,
  WalletAccount,
  WalletTransaction,
  WalletType,
} from '@/types/domain'

export interface AnalyticsReport {
  monthlyTrends: MonthlyTrendPoint[]
  spendingByCategory: CategorySpendPoint[]
  spendingByMember: MemberSpendPoint[]
  budget: Budget
}

async function mock<T>(data: T): Promise<T> {
  await delay()
  return structuredClone(data)
}

function familyQuery(prefix: string): string {
  const familyId = getActiveFamilyId() ?? requireActiveFamilyId()
  const join = prefix.includes('?') ? '&' : '?'
  return `${prefix}${join}familyId=${encodeURIComponent(familyId)}`
}

function asFamilyRole(code: string): FamilyRole {
  const roles: FamilyRole[] = ['OWNER', 'PARENT', 'ADULT_MEMBER', 'CHILD', 'VIEWER']
  return roles.includes(code as FamilyRole) ? (code as FamilyRole) : 'VIEWER'
}

function mapWalletType(walletType: string): WalletType {
  if (walletType === 'FAMILY_POOL') return 'FAMILY_SHARED'
  if (walletType === 'MEMBER_POCKET' || walletType === 'MEMBER_WALLET') return 'MEMBER_PERSONAL'
  return walletType as WalletType
}

type BackendWallet = {
  id: string
  familyId: string
  ownerMemberId?: string
  name: string
  currencyCode: 'VND'
  walletType: string
  status: string
  availableBalance: number
  updatedAt: string
}

type BackendLedgerTx = {
  id: string
  description: string
  postedAt: string
  referenceId?: string
  entries: Array<{ entryType: 'DEBIT' | 'CREDIT'; amount: number; memo?: string }>
}

type BackendMember = {
  id: string
  userProfileId: string
  roleCode: string
  nickname?: string | null
  joinedAt?: string
}

type BackendPaymentOrder = {
  id: string
  amount: number
  purpose: string
  status: PaymentOrder['status']
  createdAt: string
  updatedAt: string
}

type CreatePaymentResponse = {
  paymentOrderId: string
  status: PaymentOrder['status']
  payUrl: string
  providerCode: PaymentProviderCode
  order: BackendPaymentOrder
}

export const familyApi = {
  async getFamily(preferredId?: string): Promise<Family> {
    if (USE_MOCK) return mock(demoFamily)
    const families = await apiRequest<Array<{ id: string; name: string }>>('/api/families')
    const first =
      (preferredId ? families.find((f) => f.id === preferredId) : undefined) ?? families[0]
    if (!first) {
      return { id: '', name: 'Chưa có gia đình', members: [] }
    }
    const members = await apiRequest<BackendMember[]>(`/api/families/${first.id}/members`)
    return {
      id: first.id,
      name: first.name,
      members: members.map((m) => ({
        id: m.id,
        userId: m.userProfileId,
        fullName: m.nickname?.trim() || `Thành viên ${m.id.slice(0, 6)}`,
        email: '',
        role: asFamilyRole(m.roleCode),
        joinedAt: m.joinedAt ?? new Date().toISOString(),
        monthlySpend: 0,
        avatarInitials: (m.nickname ?? 'TV').slice(0, 2).toUpperCase(),
      })),
    }
  },
}

export const dashboardApi = {
  async getSummary(): Promise<DashboardSummary> {
    if (USE_MOCK) return mock(dashboardSummary)
    requireActiveFamilyId()
    const report = await apiRequest<AnalyticsReport>(familyQuery('/api/v1/analytics/reports'))
    const walletRows = await apiRequest<BackendWallet[]>(familyQuery('/api/v1/wallets'))
    const allowanceRows = await apiRequest<
      Array<{ id: string; requesterMemberId: string; amount: number; reason: string; status: string; createdAt: string }>
    >(familyQuery('/api/v1/allowances/requests'))
    const inventoryRows = await apiRequest<
      Array<{ id: string; catalogItemId: string; quantityOnHand: number; reorderLevel: number; lowStock: boolean; updatedAt: string }>
    >(familyQuery('/api/v1/household/inventory'))

    const totalBalance = walletRows.reduce((sum, w) => sum + (w.availableBalance ?? 0), 0)
    const latestTrend = report.monthlyTrends.at(-1)
    const primaryWallet = walletRows[0]
    const recentTransactions = primaryWallet
      ? await walletApi.getTransactions(primaryWallet.id)
      : []

    const pendingAllowances: AllowanceRequest[] = allowanceRows
      .filter((r) => r.status === 'PENDING')
      .slice(0, 5)
      .map((r) => ({
        id: r.id,
        childMemberId: r.requesterMemberId,
        childName: r.requesterMemberId.slice(0, 8),
        amount: r.amount,
        reason: r.reason,
        status: 'PENDING',
        requestedAt: r.createdAt,
      }))

    const lowStockItems: InventoryItem[] = inventoryRows
      .filter((i) => i.lowStock)
      .slice(0, 5)
      .map((i) => ({
        id: i.id,
        name: i.catalogItemId.slice(0, 8),
        category: 'Gia dụng',
        quantity: Number(i.quantityOnHand),
        unit: 'cái',
        minThreshold: Number(i.reorderLevel),
        updatedAt: i.updatedAt,
      }))

    return {
      totalBalance,
      monthlyIncome: latestTrend?.income ?? 0,
      monthlyExpense: latestTrend?.expense ?? 0,
      remainingBudget: report.budget.totalAllocated - report.budget.totalSpent,
      recentTransactions: recentTransactions.slice(0, 5),
      monthlySpending: report.monthlyTrends.map((p) => ({ label: p.month, amount: p.expense })),
      spendingByCategory: report.spendingByCategory,
      memberSpending: report.spendingByMember,
      pendingAllowances,
      lowStockItems,
    }
  },
}

export const walletApi = {
  async listWallets(familyId?: string): Promise<WalletAccount[]> {
    if (USE_MOCK) return mock(wallets)
    const id = familyId ?? requireActiveFamilyId()
    const rows = await apiRequest<BackendWallet[]>(`/api/v1/wallets?familyId=${encodeURIComponent(id)}`)
    return rows.map((w) => ({
      id: w.id,
      name: w.name,
      type: mapWalletType(w.walletType),
      ownerMemberId: w.ownerMemberId,
      currency: 'VND',
      balance: w.availableBalance,
      updatedAt: w.updatedAt,
    }))
  },
  async getWallet(walletId: string): Promise<WalletAccount> {
    if (USE_MOCK) {
      const found = wallets.find((w) => w.id === walletId)
      if (!found) throw new Error('Wallet not found')
      return mock(found)
    }
    const w = await apiRequest<BackendWallet>(`/api/v1/wallets/${walletId}`)
    return {
      id: w.id,
      name: w.name,
      type: mapWalletType(w.walletType),
      ownerMemberId: w.ownerMemberId,
      currency: 'VND',
      balance: w.availableBalance,
      updatedAt: w.updatedAt,
    }
  },
  async getTransactions(walletId?: string): Promise<WalletTransaction[]> {
    if (USE_MOCK) {
      const rows = walletId ? transactions.filter((t) => t.walletId === walletId) : transactions
      return mock(rows)
    }
    if (!walletId) {
      const familyWallets = await this.listWallets()
      const all = await Promise.all(familyWallets.map((w) => this.getTransactions(w.id)))
      return all.flat().sort((a, b) => b.occurredAt.localeCompare(a.occurredAt))
    }
    const rows = await apiRequest<BackendLedgerTx[]>(`/api/v1/wallets/${walletId}/transactions`)
    return rows.flatMap((tx) =>
      tx.entries.map((e) => ({
        id: `${tx.id}-${e.entryType}`,
        walletId,
        entryType: e.entryType,
        amount: e.amount,
        description: e.memo || tx.description,
        occurredAt: tx.postedAt,
        referenceId: tx.referenceId,
      })),
    )
  },
  transfer(input: {
    sourceWalletId: string
    destinationWalletId: string
    amount: number
    referenceId: string
    idempotencyKey: string
    description?: string
  }) {
    if (USE_MOCK) return mock({ ok: true })
    return apiRequest('/api/v1/wallet-transfers', { method: 'POST', body: input })
  },
}

export const paymentApi = {
  listOrders(): Promise<PaymentOrder[]> {
    if (USE_MOCK) return mock(paymentOrders)
    return apiRequest<BackendPaymentOrder[]>('/api/v1/payments').then((rows) =>
      rows.map((o) => ({
          id: o.id,
          amount: o.amount,
          provider: 'MOMO_SANDBOX',
          status: o.status,
          description: o.purpose,
          createdAt: o.createdAt,
          updatedAt: o.updatedAt,
        })),
    )
  },
  async createTopUp(input: {
    amount: number
    provider: PaymentProviderCode
    description: string
    familyId?: string
    walletId?: string
    payerMemberId?: string
  }): Promise<PaymentOrder & { payUrl?: string }> {
    if (USE_MOCK) {
      await delay()
      const provider = getPaymentProvider(
        input.provider === 'FAKE_PAYMENT' ? 'DEMO_PROVIDER' : input.provider,
      )
      const result = await provider.createPayment({
        amount: input.amount,
        description: input.description,
        idempotencyKey: crypto.randomUUID(),
      })
      const order: PaymentOrder = {
        id: result.orderId,
        amount: input.amount,
        provider: input.provider,
        status: 'PENDING_PROVIDER',
        description: input.description,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        note: result.message,
      }
      paymentOrders.unshift(order)
      return structuredClone(order)
    }
    const familyId = input.familyId ?? requireActiveFamilyId()
    const walletId = input.walletId ?? (await walletApi.listWallets(familyId))[0]?.id
    if (!walletId) {
      throw new Error('Không tìm thấy ví để nạp tiền')
    }
    const payerMemberId = input.payerMemberId ?? getActiveMemberId() ?? undefined
    const providerCode = input.provider === 'DEMO_PROVIDER' ? 'FAKE_PAYMENT' : input.provider
    const created = await apiRequest<CreatePaymentResponse>('/api/v1/payments', {
      method: 'POST',
      body: {
        familyId,
        payerMemberId,
        walletId,
        amount: input.amount,
        purpose: input.description,
        providerCode,
      },
    })
    const backendStatus = created.status ?? created.order.status
    return {
      id: created.paymentOrderId,
      amount: input.amount,
      provider: created.providerCode,
      status: backendStatus,
      description: input.description,
      createdAt: created.order.createdAt,
      updatedAt: created.order.updatedAt,
      payUrl: created.payUrl,
      note: created.payUrl ? 'Chờ xác nhận từ cổng thanh toán / backend' : undefined,
    }
  },
}

export const budgetApi = {
  getCurrent(): Promise<Budget> {
    if (USE_MOCK) return mock(budget)
    return apiRequest<{
      id: string
      periodStart: string
      totalLimit: number
      totalSpent: number
      categories: Array<{ id: string; displayName: string; allocatedAmount: number; spentAmount: number }>
    }>(familyQuery('/api/v1/budgets/current')).then((b) => ({
      id: b.id,
      month: b.periodStart.slice(0, 7),
      totalAllocated: b.totalLimit,
      totalSpent: b.totalSpent,
      categories: b.categories.map((c) => ({
        id: c.id,
        name: c.displayName,
        allocated: c.allocatedAmount,
        spent: c.spentAmount,
      })),
    }))
  },
}

export const allowanceApi = {
  listPolicies(): Promise<AllowancePolicy[]> {
    if (USE_MOCK) return mock(allowancePolicies)
    return apiRequest<
      Array<{ id: string; memberId: string; amount: number; dayOfMonth?: number | null; status: string }>
    >(familyQuery('/api/v1/allowances/policies')).then((rows) =>
      rows.map((p) => ({
        id: p.id,
        childMemberId: p.memberId,
        childName: p.memberId.slice(0, 8),
        monthlyAmount: p.amount,
        dayOfMonth: p.dayOfMonth ?? 1,
        active: p.status === 'ACTIVE',
      })),
    )
  },
  listRequests(): Promise<AllowanceRequest[]> {
    if (USE_MOCK) return mock(allowanceRequests)
    return apiRequest<
      Array<{
        id: string
        requesterMemberId: string
        amount: number
        reason: string
        status: AllowanceRequest['status']
        createdAt: string
        decidedAt?: string | null
      }>
    >(familyQuery('/api/v1/allowances/requests')).then((rows) =>
      rows.map((r) => ({
        id: r.id,
        childMemberId: r.requesterMemberId,
        childName: r.requesterMemberId.slice(0, 8),
        amount: r.amount,
        reason: r.reason,
        status: r.status,
        requestedAt: r.createdAt,
        reviewedAt: r.decidedAt ?? undefined,
      })),
    )
  },
}

export const expenseApi = {
  list(): Promise<Expense[]> {
    if (USE_MOCK) return mock(expenses)
    return apiRequest<
      Array<{
        id: string
        title: string
        amountTotal: number
        categoryId: string
        paidByMemberId: string
        expenseDate: string
        notes?: string | null
      }>
    >(familyQuery('/api/v1/expenses')).then((rows) =>
      rows.map((e) => ({
        id: e.id,
        title: e.title,
        amount: e.amountTotal,
        category: e.categoryId,
        memberId: e.paidByMemberId,
        memberName: e.paidByMemberId.slice(0, 8),
        occurredAt: e.expenseDate,
        note: e.notes ?? undefined,
        hasReceipt: false,
      })),
    )
  },
}

export const householdApi = {
  listInventory(): Promise<InventoryItem[]> {
    if (USE_MOCK) return mock(inventoryItems)
    return apiRequest<
      Array<{
        id: string
        catalogItemId: string
        quantityOnHand: number
        reorderLevel: number
        lowStock: boolean
        updatedAt: string
      }>
    >(familyQuery('/api/v1/household/inventory')).then((rows) =>
      rows.map((i) => ({
        id: i.id,
        name: i.catalogItemId.slice(0, 8),
        category: 'Gia dụng',
        quantity: Number(i.quantityOnHand),
        unit: 'cái',
        minThreshold: Number(i.reorderLevel),
        updatedAt: i.updatedAt,
      })),
    )
  },
  listShopping(): Promise<ShoppingListItem[]> {
    if (USE_MOCK) return mock(shoppingList)
    return apiRequest<Array<{ id: string; title: string; status: string }>>('/api/shopping-lists').then(
      (rows) =>
        rows.map((l) => ({
          id: l.id,
          name: l.title,
          quantity: 1,
          unit: 'mục',
          purchased: l.status === 'DONE',
        })),
    )
  },
}

export const notificationApi = {
  list(): Promise<AppNotification[]> {
    if (USE_MOCK) return mock(notifications)
    const userId = getActiveUserId()
    if (!userId) return Promise.resolve([])
    const path = familyQuery(`/api/v1/notifications?recipientUserId=${encodeURIComponent(userId)}`)
    return apiRequest<
      Array<{ id: string; title: string; body: string; templateKey: string; read: boolean; createdAt: string }>
    >(path).then((rows) =>
      rows.map((n) => ({
        id: n.id,
        type: (n.templateKey.split('.')[0]?.toUpperCase() ?? 'SYSTEM') as NotificationType,
        title: n.title,
        message: n.body,
        createdAt: n.createdAt,
        read: n.read,
      })),
    )
  },
  preferences(): Promise<NotificationPreference[]> {
    if (USE_MOCK) return mock(notificationPreferences)
    const userId = getActiveUserId()
    if (!userId) return Promise.resolve([])
    return apiRequest<
      Array<{ channel: string; category: string; enabled: boolean }>
    >(`/api/v1/notifications/preferences?userProfileId=${encodeURIComponent(userId)}`).then((rows) =>
      rows.map((p) => ({
        type: (p.category?.toUpperCase() ?? 'SYSTEM') as NotificationType,
        email: p.channel === 'EMAIL' && p.enabled,
        inApp: p.channel === 'IN_APP' && p.enabled,
        push: p.channel === 'PUSH' && p.enabled,
      })),
    )
  },
}

export const analyticsApi = {
  async getReports(): Promise<AnalyticsReport> {
    if (USE_MOCK) {
      await delay()
      return {
        monthlyTrends: structuredClone(monthlyTrends),
        spendingByCategory: structuredClone(spendingByCategory),
        spendingByMember: structuredClone(spendingByMember),
        budget: structuredClone(budget),
      }
    }
    const payload = await apiRequest<{
      monthlyTrends: MonthlyTrendPoint[]
      spendingByCategory: CategorySpendPoint[]
      spendingByMember: MemberSpendPoint[]
      budget: {
        id: string
        month: string
        totalAllocated: number
        totalSpent: number
        categories: Array<{ id: string; name: string; allocated: number; spent: number }>
      }
    }>(familyQuery('/api/v1/analytics/reports'))

    return {
      monthlyTrends: payload.monthlyTrends,
      spendingByCategory: payload.spendingByCategory,
      spendingByMember: payload.spendingByMember,
      budget: {
        id: payload.budget.id,
        month: payload.budget.month,
        totalAllocated: payload.budget.totalAllocated,
        totalSpent: payload.budget.totalSpent,
        categories: payload.budget.categories.map((c) => ({
          id: c.id,
          name: c.name,
          allocated: c.allocated,
          spent: c.spent,
        })),
      },
    }
  },
}