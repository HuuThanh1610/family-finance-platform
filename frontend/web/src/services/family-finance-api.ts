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
  InventoryItem,
  MemberSpendPoint,
  MonthlyTrendPoint,
  NotificationPreference,
  PaymentOrder,
  PaymentProviderCode,
  ShoppingListItem,
  WalletAccount,
  WalletTransaction,
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

export const familyApi = {
  async getFamily(): Promise<Family> {
    if (USE_MOCK) return mock(demoFamily)
    const families = await apiRequest<Array<{ id: string; name: string }>>('/api/families')
    const first = families[0]
    if (!first) {
      return { id: '', name: 'Chưa có gia đình', members: [] }
    }
    const members = await apiRequest<
      Array<{
        id: string
        userProfileId: string
        role: Family['members'][number]['role']
        joinedAt?: string
      }>
    >(`/api/families/${first.id}/members`)
    return {
      id: first.id,
      name: first.name,
      members: members.map((m) => ({
        id: m.id,
        userId: m.userProfileId,
        fullName: m.userProfileId.slice(0, 8),
        email: '',
        role: m.role,
        joinedAt: m.joinedAt ?? new Date().toISOString(),
        monthlySpend: 0,
        avatarInitials: 'TV',
      })),
    }
  },
}

export const dashboardApi = {
  getSummary(): Promise<DashboardSummary> {
    if (USE_MOCK) return mock(dashboardSummary)
    return apiRequest('/api/dashboard/summary')
  },
}

type BackendWallet = {
  id: string
  familyId: string
  ownerMemberId?: string
  name: string
  currencyCode: 'VND'
  walletType: WalletAccount['type']
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

export const walletApi = {
  async listWallets(familyId?: string): Promise<WalletAccount[]> {
    if (USE_MOCK) return mock(wallets)
    // Backend currently exposes get-by-id; list by family requires gateway aggregation later.
    // Fallback: return empty until caller provides known wallet ids from family bootstrap.
    void familyId
    return []
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
      type: w.walletType,
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
    if (!walletId) return []
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
      const provider = getPaymentProvider(input.provider === 'FAKE_PAYMENT' ? 'DEMO_PROVIDER' : input.provider)
      const result = await provider.createPayment({
        amount: input.amount,
        description: input.description,
        idempotencyKey: crypto.randomUUID(),
      })
      const order: PaymentOrder = {
        id: result.orderId,
        amount: input.amount,
        provider: input.provider,
        status: result.status === 'SUCCESS' ? 'COMPLETED' : 'PENDING_PROVIDER',
        description: input.description,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        note: result.message,
      }
      paymentOrders.unshift(order)
      return structuredClone(order)
    }
    if (!input.familyId || !input.walletId) {
      throw new Error('familyId và walletId là bắt buộc khi gọi API thật')
    }
    const providerCode = input.provider === 'DEMO_PROVIDER' ? 'FAKE_PAYMENT' : input.provider
    const created = await apiRequest<CreatePaymentResponse>('/api/v1/payments', {
      method: 'POST',
      body: {
        familyId: input.familyId,
        payerMemberId: input.payerMemberId,
        walletId: input.walletId,
        amount: input.amount,
        purpose: input.description,
        providerCode,
      },
    })
    return {
      id: created.paymentOrderId,
      amount: input.amount,
      provider: created.providerCode,
      status: created.status,
      description: input.description,
      createdAt: created.order.createdAt,
      updatedAt: created.order.updatedAt,
      payUrl: created.payUrl,
      note: created.payUrl ? 'Chuyển hướng tới cổng thanh toán sandbox' : undefined,
    }
  },
}

export const budgetApi = {
  getCurrent(): Promise<Budget> {
    if (USE_MOCK) return mock(budget)
    return apiRequest('/api/v1/budgets/current')
  },
}

export const allowanceApi = {
  listPolicies(): Promise<AllowancePolicy[]> {
    if (USE_MOCK) return mock(allowancePolicies)
    return apiRequest('/api/v1/allowances/policies')
  },
  listRequests(): Promise<AllowanceRequest[]> {
    if (USE_MOCK) return mock(allowanceRequests)
    return apiRequest('/api/v1/allowances/requests')
  },
}

export const expenseApi = {
  list(): Promise<Expense[]> {
    if (USE_MOCK) return mock(expenses)
    return apiRequest('/api/v1/expenses')
  },
}

export const householdApi = {
  listInventory(): Promise<InventoryItem[]> {
    if (USE_MOCK) return mock(inventoryItems)
    return apiRequest('/api/v1/household/inventory')
  },
  listShopping(): Promise<ShoppingListItem[]> {
    if (USE_MOCK) return mock(shoppingList)
    return apiRequest('/api/v1/household/shopping-lists')
  },
}

export const notificationApi = {
  list(): Promise<AppNotification[]> {
    if (USE_MOCK) return mock(notifications)
    return apiRequest('/api/v1/notifications')
  },
  preferences(): Promise<NotificationPreference[]> {
    if (USE_MOCK) return mock(notificationPreferences)
    return apiRequest('/api/v1/notifications/preferences')
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
    return apiRequest('/api/v1/analytics/reports')
  },
}
