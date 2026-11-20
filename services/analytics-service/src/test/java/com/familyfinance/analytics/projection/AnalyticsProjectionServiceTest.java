package com.familyfinance.analytics.projection;

import com.familyfinance.analytics.entity.FamilyMonthlySummary;
import com.familyfinance.analytics.entity.RealizedFinancialPosting;
import com.familyfinance.analytics.messaging.DomainEventMessage;
import com.familyfinance.analytics.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsProjectionServiceTest {

    @Mock FamilyMonthlySummaryRepository monthlySummaryRepository;
    @Mock CategorySpendingSummaryRepository categorySpendingSummaryRepository;
    @Mock MemberSpendingSummaryRepository memberSpendingSummaryRepository;
    @Mock BudgetCategoryActualRepository budgetCategoryActualRepository;
    @Mock WalletBalanceSummaryRepository walletBalanceSummaryRepository;
    @Mock RealizedFinancialPostingRepository realizedFinancialPostingRepository;

    AnalyticsProjectionService service;

    UUID familyId = UUID.randomUUID();
    UUID ledgerTxId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AnalyticsProjectionService(
                monthlySummaryRepository,
                categorySpendingSummaryRepository,
                memberSpendingSummaryRepository,
                budgetCategoryActualRepository,
                walletBalanceSummaryRepository,
                realizedFinancialPostingRepository);
    }

    @Test
    void paymentProviderPaidAloneDoesNotIncreaseIncome() {
        service.apply(event("PaymentProviderPaid", Map.of(
                "familyId", familyId.toString(),
                "amount", 1_000_000L
        )));

        verifyNoInteractions(monthlySummaryRepository, realizedFinancialPostingRepository);
    }

    @Test
    void walletCreditPostedIncreasesIncomeOnce() {
        Instant occurred = Instant.parse("2026-11-15T10:00:00Z");
        LocalDate month = LocalDate.of(2026, 11, 1);
        stubMonthlySave(month);
        when(realizedFinancialPostingRepository.existsByLedgerTransactionId(ledgerTxId)).thenReturn(false);
        when(realizedFinancialPostingRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.apply(creditEvent(occurred, ledgerTxId, 1_000_000L));

        ArgumentCaptor<FamilyMonthlySummary> captor = ArgumentCaptor.forClass(FamilyMonthlySummary.class);
        verify(monthlySummaryRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalIncome()).isEqualTo(1_000_000L);
        verify(realizedFinancialPostingRepository).saveAndFlush(any(RealizedFinancialPosting.class));
    }

    @Test
    void replayedWalletCreditPostedDoesNotDoubleCount() {
        when(realizedFinancialPostingRepository.existsByLedgerTransactionId(ledgerTxId)).thenReturn(true);

        service.apply(creditEvent(Instant.parse("2026-11-15T10:00:00Z"), ledgerTxId, 1_000_000L));

        verify(monthlySummaryRepository, never()).save(any());
        verify(realizedFinancialPostingRepository, never()).saveAndFlush(any());
    }

    @Test
    void differentEventIdsSameLedgerTransactionDoNotDoubleCount() {
        when(realizedFinancialPostingRepository.existsByLedgerTransactionId(ledgerTxId)).thenReturn(true);

        DomainEventMessage second = new DomainEventMessage(
                UUID.randomUUID(),
                "WalletCreditPosted",
                "ledger_transaction",
                ledgerTxId,
                null,
                Instant.parse("2026-11-15T11:00:00Z"),
                Map.of(
                        "familyId", familyId.toString(),
                        "transactionId", ledgerTxId.toString(),
                        "transactionType", "PAYMENT_TOPUP",
                        "amount", 1_000_000L,
                        "currencyCode", "VND",
                        "referenceId", "pay:1"
                )
        );
        service.apply(second);

        verify(monthlySummaryRepository, never()).save(any());
    }

    @Test
    void expensePaymentRequestedDoesNotIncreaseExpenses() {
        service.apply(event("ExpensePaymentRequested", Map.of(
                "familyId", familyId.toString(),
                "amount", 200_000,
                "categoryCode", "FOOD"
        )));

        verifyNoInteractions(monthlySummaryRepository, categorySpendingSummaryRepository,
                realizedFinancialPostingRepository);
    }

    @Test
    void walletDebitPostedExpensePaymentIncreasesExpensesOnce() {
        Instant occurred = LocalDate.of(2026, 11, 18).atStartOfDay().toInstant(ZoneOffset.UTC);
        LocalDate month = LocalDate.of(2026, 11, 1);
        stubMonthlySave(month);
        when(categorySpendingSummaryRepository.findByFamilyIdAndCategoryCodeAndSummaryMonth(
                familyId, "OTHER", month)).thenReturn(Optional.empty());
        when(budgetCategoryActualRepository.findByFamilyIdAndCategoryCodeAndSummaryMonth(
                familyId, "OTHER", month)).thenReturn(Optional.empty());
        when(categorySpendingSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(budgetCategoryActualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(realizedFinancialPostingRepository.existsByLedgerTransactionId(ledgerTxId)).thenReturn(false);
        when(realizedFinancialPostingRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.apply(debitExpenseEvent(occurred, ledgerTxId, 200_000L));

        ArgumentCaptor<FamilyMonthlySummary> captor = ArgumentCaptor.forClass(FamilyMonthlySummary.class);
        verify(monthlySummaryRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalExpense()).isEqualTo(200_000L);
        verify(categorySpendingSummaryRepository).save(any());
        verify(budgetCategoryActualRepository).save(any());
    }

    @Test
    void expensePaymentFailedDoesNotAffectExpenses() {
        service.apply(event("ExpensePaymentFailed", Map.of(
                "familyId", familyId.toString(),
                "amount", 200_000,
                "errorCode", "INSUFFICIENT_FUNDS"
        )));

        verifyNoInteractions(monthlySummaryRepository, realizedFinancialPostingRepository);
    }

    @Test
    void internalWalletTransferPostedDoesNotAffectCashflow() {
        service.apply(event("WalletTransferPosted", Map.of(
                "familyId", familyId.toString(),
                "transactionId", ledgerTxId.toString(),
                "transactionType", "INTERNAL_TRANSFER",
                "amount", 150_000L,
                "currencyCode", "VND"
        )));

        verifyNoInteractions(monthlySummaryRepository, realizedFinancialPostingRepository);
    }

    @Test
    void allowanceDisbursementRequestedDoesNotCountAsExpense() {
        service.apply(event("AllowanceDisbursementRequested", Map.of(
                "familyId", familyId.toString(),
                "amount", 150_000L,
                "requesterMemberId", UUID.randomUUID().toString()
        )));

        verifyNoInteractions(monthlySummaryRepository, realizedFinancialPostingRepository);
    }

    @Test
    void allowanceDisbursementCompletedDoesNotCountAsHouseholdSpending() {
        service.apply(event("AllowanceDisbursementCompleted", Map.of(
                "familyId", familyId.toString(),
                "amount", 150_000L,
                "transactionId", ledgerTxId.toString(),
                "referenceId", "allowance:" + UUID.randomUUID()
        )));

        verifyNoInteractions(monthlySummaryRepository, realizedFinancialPostingRepository);
    }

    @Test
    void unsupportedWalletTransactionTypeIsQuarantined() {
        service.apply(event("WalletDebitPosted", Map.of(
                "familyId", familyId.toString(),
                "transactionId", ledgerTxId.toString(),
                "transactionType", "UNKNOWN_TYPE",
                "amount", 50_000L,
                "currencyCode", "VND"
        )));

        verifyNoInteractions(monthlySummaryRepository);
        verify(realizedFinancialPostingRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidAmountDoesNotMutateProjection() {
        service.apply(event("WalletCreditPosted", Map.of(
                "familyId", familyId.toString(),
                "transactionId", ledgerTxId.toString(),
                "transactionType", "PAYMENT_TOPUP",
                "amount", 0,
                "currencyCode", "VND"
        )));

        verifyNoInteractions(monthlySummaryRepository);
        verify(realizedFinancialPostingRepository, never()).saveAndFlush(any());
    }

    @Test
    void invalidFamilyIdDoesNotMutateProjection() {
        service.apply(event("WalletCreditPosted", Map.of(
                "familyId", "not-a-uuid",
                "transactionId", ledgerTxId.toString(),
                "transactionType", "PAYMENT_TOPUP",
                "amount", 1000,
                "currencyCode", "VND"
        )));

        verifyNoInteractions(monthlySummaryRepository, realizedFinancialPostingRepository);
    }

    @Test
    void invalidCurrencyDoesNotMutateProjection() {
        service.apply(event("WalletCreditPosted", Map.of(
                "familyId", familyId.toString(),
                "transactionId", ledgerTxId.toString(),
                "transactionType", "PAYMENT_TOPUP",
                "amount", 1000,
                "currencyCode", "VN"
        )));

        verifyNoInteractions(monthlySummaryRepository);
        verify(realizedFinancialPostingRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingTransactionIdDoesNotMutateProjection() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("familyId", familyId.toString());
        payload.put("transactionType", "PAYMENT_TOPUP");
        payload.put("amount", 1000);
        payload.put("currencyCode", "VND");

        service.apply(new DomainEventMessage(
                UUID.randomUUID(),
                "WalletCreditPosted",
                "ledger_transaction",
                null,
                null,
                Instant.now(),
                payload
        ));

        verifyNoInteractions(monthlySummaryRepository);
        verify(realizedFinancialPostingRepository, never()).saveAndFlush(any());
    }

    private void stubMonthlySave(LocalDate month) {
        when(monthlySummaryRepository.findByFamilyIdAndSummaryMonth(familyId, month)).thenReturn(Optional.empty());
        when(monthlySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private DomainEventMessage creditEvent(Instant occurred, UUID txId, long amount) {
        return new DomainEventMessage(
                UUID.randomUUID(),
                "WalletCreditPosted",
                "ledger_transaction",
                txId,
                null,
                occurred,
                Map.of(
                        "familyId", familyId.toString(),
                        "transactionId", txId.toString(),
                        "transactionType", "PAYMENT_TOPUP",
                        "amount", amount,
                        "currencyCode", "VND",
                        "referenceId", "pay:" + txId
                )
        );
    }

    private DomainEventMessage debitExpenseEvent(Instant occurred, UUID txId, long amount) {
        return new DomainEventMessage(
                UUID.randomUUID(),
                "WalletDebitPosted",
                "ledger_transaction",
                txId,
                null,
                occurred,
                Map.of(
                        "familyId", familyId.toString(),
                        "transactionId", txId.toString(),
                        "transactionType", "EXPENSE_PAYMENT",
                        "amount", amount,
                        "currencyCode", "VND",
                        "referenceId", "expense:" + txId
                )
        );
    }

    private DomainEventMessage event(String type, Map<String, Object> payload) {
        return new DomainEventMessage(
                UUID.randomUUID(),
                type,
                "test",
                UUID.randomUUID(),
                null,
                Instant.now(),
                payload
        );
    }
}
