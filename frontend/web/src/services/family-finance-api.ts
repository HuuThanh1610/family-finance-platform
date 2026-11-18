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
  getFamily(): Promise<Family> {
    if (USE_MOCK) return mock(demoFamily)
    return apiRequest('/api/families/current')
  },
}

export const dashboardApi = {
  getSummary(): Promise<DashboardSummary> {
    if (USE_MOCK) return mock(dashboardSummary)
    return apiRequest('/api/dashboard/summary')
  },
}

export const walletApi = {
  listWallets(): Promise<WalletAccount[]> {
    if (USE_MOCK) return mock(wallets)
    return apiRequest('/api/wallets')
  },
  getTransactions(walletId?: string): Promise<WalletTransaction[]> {
    if (USE_MOCK) {
      const rows = walletId ? transactions.filter((t) => t.walletId === walletId) : transactions
      return mock(rows)
    }
    const q = walletId ? `?walletId=${walletId}` : ''
    return apiRequest(`/api/wallets/transactions${q}`)
  },
}

export const paymentApi = {
  listOrders(): Promise<PaymentOrder[]> {
    if (USE_MOCK) return mock(paymentOrders)
    return apiRequest('/api/payments')
  },
  async createTopUp(input: {
    amount: number
    provider: PaymentProviderCode
    description: string
  }): Promise<PaymentOrder> {
    if (USE_MOCK) {
      await delay()
      const provider = getPaymentProvider(input.provider)
      const result = await provider.createPayment({
        amount: input.amount,
        description: input.description,
        idempotencyKey: crypto.randomUUID(),
      })
      const order: PaymentOrder = {
        id: result.orderId,
        amount: input.amount,
        provider: input.provider,
        status: result.status,
        description: input.description,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        note: result.message,
      }
      paymentOrders.unshift(order)
      return structuredClone(order)
    }
    return apiRequest('/api/payments', { method: 'POST', body: input })
  },
}

export const budgetApi = {
  getCurrent(): Promise<Budget> {
    if (USE_MOCK) return mock(budget)
    return apiRequest('/api/budgets/current')
  },
}

export const allowanceApi = {
  listPolicies(): Promise<AllowancePolicy[]> {
    if (USE_MOCK) return mock(allowancePolicies)
    return apiRequest('/api/allowances/policies')
  },
  listRequests(): Promise<AllowanceRequest[]> {
    if (USE_MOCK) return mock(allowanceRequests)
    return apiRequest('/api/allowances/requests')
  },
}

export const expenseApi = {
  list(): Promise<Expense[]> {
    if (USE_MOCK) return mock(expenses)
    return apiRequest('/api/expenses')
  },
}

export const householdApi = {
  listInventory(): Promise<InventoryItem[]> {
    if (USE_MOCK) return mock(inventoryItems)
    return apiRequest('/api/household/inventory')
  },
  listShopping(): Promise<ShoppingListItem[]> {
    if (USE_MOCK) return mock(shoppingList)
    return apiRequest('/api/household/shopping-list')
  },
}

export const notificationApi = {
  list(): Promise<AppNotification[]> {
    if (USE_MOCK) return mock(notifications)
    return apiRequest('/api/notifications')
  },
  preferences(): Promise<NotificationPreference[]> {
    if (USE_MOCK) return mock(notificationPreferences)
    return apiRequest('/api/notifications/preferences')
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
    return apiRequest('/api/analytics/reports')
  },
}
