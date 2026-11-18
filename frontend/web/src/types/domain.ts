export type FamilyRole = 'OWNER' | 'PARENT' | 'ADULT_MEMBER' | 'CHILD' | 'VIEWER'

export type WalletType = 'FAMILY_SHARED' | 'MEMBER_PERSONAL'

export type LedgerEntryType = 'DEBIT' | 'CREDIT'

export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'

export type PaymentProviderCode = 'MOMO_SANDBOX' | 'DEMO_PROVIDER'

export type AllowanceRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type NotificationType =
  | 'ALLOWANCE'
  | 'BUDGET'
  | 'PAYMENT'
  | 'HOUSEHOLD'
  | 'SYSTEM'
  | 'EXPENSE'

export interface UserProfile {
  id: string
  email: string
  fullName: string
  avatarUrl?: string
  phone?: string
}

export interface FamilyMember {
  id: string
  userId: string
  fullName: string
  email: string
  role: FamilyRole
  joinedAt: string
  monthlySpend: number
  avatarInitials: string
}

export interface Family {
  id: string
  name: string
  members: FamilyMember[]
}

export interface WalletAccount {
  id: string
  name: string
  type: WalletType
  ownerMemberId?: string
  currency: 'VND'
  balance: number
  updatedAt: string
}

export interface WalletTransaction {
  id: string
  walletId: string
  entryType: LedgerEntryType
  amount: number
  description: string
  category?: string
  memberName?: string
  occurredAt: string
  referenceId?: string
}

export interface PaymentOrder {
  id: string
  amount: number
  provider: PaymentProviderCode
  status: PaymentStatus
  description: string
  createdAt: string
  updatedAt: string
  note?: string
}

export interface BudgetCategory {
  id: string
  name: string
  allocated: number
  spent: number
}

export interface Budget {
  id: string
  month: string
  totalAllocated: number
  totalSpent: number
  categories: BudgetCategory[]
}

export interface AllowancePolicy {
  id: string
  childMemberId: string
  childName: string
  monthlyAmount: number
  dayOfMonth: number
  active: boolean
}

export interface AllowanceRequest {
  id: string
  childMemberId: string
  childName: string
  amount: number
  reason: string
  status: AllowanceRequestStatus
  requestedAt: string
  reviewedAt?: string
  reviewerName?: string
  reviewNote?: string
}

export interface Expense {
  id: string
  title: string
  amount: number
  category: string
  memberId: string
  memberName: string
  occurredAt: string
  note?: string
  hasReceipt: boolean
}

export interface InventoryItem {
  id: string
  name: string
  category: string
  quantity: number
  unit: string
  minThreshold: number
  updatedAt: string
}

export interface ShoppingListItem {
  id: string
  name: string
  quantity: number
  unit: string
  assignedTo?: string
  purchased: boolean
}

export interface AppNotification {
  id: string
  type: NotificationType
  title: string
  message: string
  createdAt: string
  read: boolean
}

export interface NotificationPreference {
  type: NotificationType
  email: boolean
  inApp: boolean
  push: boolean
}

export interface MonthlyTrendPoint {
  month: string
  income: number
  expense: number
}

export interface CategorySpendPoint {
  category: string
  amount: number
}

export interface MemberSpendPoint {
  memberName: string
  amount: number
}

export interface DashboardSummary {
  totalBalance: number
  monthlyIncome: number
  monthlyExpense: number
  remainingBudget: number
  recentTransactions: WalletTransaction[]
  monthlySpending: { label: string; amount: number }[]
  spendingByCategory: CategorySpendPoint[]
  memberSpending: MemberSpendPoint[]
  pendingAllowances: AllowanceRequest[]
  lowStockItems: InventoryItem[]
}
