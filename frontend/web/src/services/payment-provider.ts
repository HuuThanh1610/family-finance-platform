import type { PaymentProviderCode, PaymentStatus } from '@/types/domain'

/**
 * Abstraction for future payment provider integration (e.g. MoMo Sandbox).
 * UI and services must depend on this interface — never hardcode provider calls.
 *
 * IMPORTANT: This project does NOT connect to a real payment gateway.
 * No credentials, signing secrets, or live API endpoints are configured.
 */
export interface CreatePaymentRequest {
  amount: number
  description: string
  returnUrl?: string
  idempotencyKey: string
}

export interface CreatePaymentResponse {
  orderId: string
  provider: PaymentProviderCode
  status: PaymentStatus
  /** Present only when a real provider redirect is available */
  checkoutUrl?: string
  message: string
}

export interface PaymentProvider {
  readonly code: PaymentProviderCode
  readonly displayName: string
  createPayment(request: CreatePaymentRequest): Promise<CreatePaymentResponse>
  getPaymentStatus(orderId: string): Promise<PaymentStatus>
}

/** Demo provider used by mock adapters. Does not contact any external API. */
export class DemoPaymentProvider implements PaymentProvider {
  readonly code: PaymentProviderCode = 'DEMO_PROVIDER'
  readonly displayName = 'Demo Provider (không thanh toán thật)'

  async createPayment(request: CreatePaymentRequest): Promise<CreatePaymentResponse> {
    return {
      orderId: `demo-${crypto.randomUUID()}`,
      provider: this.code,
      status: 'PENDING',
      message: `Đã tạo đơn mô phỏng ${request.amount.toLocaleString('vi-VN')}₫. Không có giao dịch thật.`,
    }
  }

  async getPaymentStatus(): Promise<PaymentStatus> {
    return 'PENDING'
  }
}

/**
 * Placeholder for future MoMo Sandbox integration.
 * Owner must implement signing, webhook verification, and credential loading from env.
 */
export class MomoSandboxProvider implements PaymentProvider {
  readonly code: PaymentProviderCode = 'MOMO_SANDBOX'
  readonly displayName = 'MoMo Sandbox (chưa kết nối)'

  async createPayment(request: CreatePaymentRequest): Promise<CreatePaymentResponse> {
    // TODO(owner): implement MoMo create payment with proper HMAC signing.
    // Never hardcode partnerCode/accessKey/secretKey in source.
    return {
      orderId: `momo-mock-${crypto.randomUUID()}`,
      provider: this.code,
      status: 'PENDING',
      message: `MoMo Sandbox chưa được cấu hình. Đơn ${request.amount.toLocaleString('vi-VN')}₫ chỉ là mock UI.`,
    }
  }

  async getPaymentStatus(): Promise<PaymentStatus> {
    // TODO(owner): poll/query MoMo transaction status securely.
    return 'PENDING'
  }
}

export function getPaymentProvider(code: PaymentProviderCode): PaymentProvider {
  if (code === 'MOMO_SANDBOX') return new MomoSandboxProvider()
  return new DemoPaymentProvider()
}
