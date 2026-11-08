package com.fpp.finans.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DebtSimulatorTest {
    private val start = YearMonth.of(2026, 1)
    private fun salary(amount: Double) = Income(id = 1, name = "Maaş", amount = amount, type = IncomeType.REGULAR)

    private fun success(outcome: SimulationOutcome): SimulationResult {
        assertTrue("Beklenen başarı, gelen: $outcome", outcome is SimulationOutcome.Success)
        return (outcome as SimulationOutcome.Success).result
    }

    @Test
    fun interestFreeLoanPaidWithBudget() {
        val data = FinanceData(
            incomes = listOf(salary(10_000.0)),
            debts = listOf(Debt(id = 1, name = "Kredi", type = DebtType.CONSUMER_LOAN, balance = 9_000.0, minPayment = 1_000.0)),
            expenses = listOf(FixedExpense(id = 1, name = "Kira", amount = 7_000.0)),
        )
        // Bütçe = 10.000 - 7.000 = 3.000/ay -> 9.000 TL borç 3 ayda biter.
        val r = success(DebtSimulator.simulate(data, Strategy.AVALANCHE, start))
        assertEquals(3, r.months)
        assertEquals(0.0, r.totalInterest, 0.001)
        assertEquals(9_000.0, r.totalPaid, 0.01)
        assertEquals(YearMonth.of(2026, 4), r.payoffDate)
    }

    @Test
    fun avalancheNeverPaysMoreInterestThanSnowball() {
        val data = FinanceData(
            incomes = listOf(salary(40_000.0)),
            debts = listOf(
                Debt(id = 1, name = "Kart", type = DebtType.CREDIT_CARD, balance = 60_000.0, annualInterestRate = 54.0, cardLimit = 80_000.0),
                Debt(id = 2, name = "Kredi", type = DebtType.CONSUMER_LOAN, balance = 20_000.0, annualInterestRate = 30.0, minPayment = 2_000.0),
                Debt(id = 3, name = "KMH", type = DebtType.OVERDRAFT, balance = 10_000.0, annualInterestRate = 60.0),
            ),
            expenses = listOf(FixedExpense(id = 1, name = "Kira", amount = 10_000.0)),
        )
        val avalanche = success(DebtSimulator.simulate(data, Strategy.AVALANCHE, start))
        val snowball = success(DebtSimulator.simulate(data, Strategy.SNOWBALL, start))
        assertTrue(avalanche.totalInterest <= snowball.totalInterest + 0.01)
        assertEquals(3, avalanche.payoffs.size)
        assertTrue(avalanche.rows.last().totalBalance == 0.0)
    }

    @Test
    fun deficitIsReported() {
        val data = FinanceData(
            incomes = listOf(salary(5_000.0)),
            debts = listOf(Debt(id = 1, name = "Kredi", type = DebtType.CONSUMER_LOAN, balance = 50_000.0, minPayment = 4_000.0)),
            expenses = listOf(FixedExpense(id = 1, name = "Kira", amount = 3_000.0)),
        )
        val outcome = DebtSimulator.simulate(data, Strategy.AVALANCHE, start)
        assertTrue(outcome is SimulationOutcome.Failure)
    }

    @Test
    fun neverEndingPlanFailsInsteadOfLooping() {
        val data = FinanceData(
            incomes = listOf(salary(1_000.0)),
            debts = listOf(Debt(id = 1, name = "Kredi", type = DebtType.CONSUMER_LOAN, balance = 1_000_000.0, annualInterestRate = 120.0, minPayment = 500.0)),
        )
        assertTrue(DebtSimulator.simulate(data, Strategy.AVALANCHE, start) is SimulationOutcome.Failure)
    }

    @Test
    fun installmentStartsOnFirstPaymentDate() {
        val data = FinanceData(
            incomes = listOf(salary(10_000.0)),
            debts = listOf(
                Debt(
                    id = 1, name = "Okul", type = DebtType.INSTALLMENT, balance = 3_000.0, minPayment = 1_000.0,
                    remainingInstallments = 3, firstPaymentDate = LocalDate.of(2026, 4, 15),
                ),
            ),
        )
        val r = success(DebtSimulator.simulate(data, Strategy.SNOWBALL, start))
        // 1. ay = Şubat. Nisan (3. ay), Mayıs ve Haziran taksitleri ödenir: 5. ayda biter.
        assertEquals(5, r.months)
        assertEquals(3_000.0, r.rows[1].totalBalance, 0.01)
        assertEquals(2_000.0, r.rows[2].totalBalance, 0.01)
    }

    @Test
    fun oneTimeIncomeAndRaisesShortenPlan() {
        val debt = Debt(id = 1, name = "Kredi", type = DebtType.CONSUMER_LOAN, balance = 100_000.0, annualInterestRate = 24.0, minPayment = 3_000.0)
        val base = FinanceData(incomes = listOf(salary(8_000.0)), debts = listOf(debt))
        val baseline = success(DebtSimulator.simulate(base, Strategy.AVALANCHE, start))

        val withBonus = base.copy(incomes = base.incomes + Income(id = 2, name = "İkramiye", amount = 30_000.0, type = IncomeType.ONE_TIME))
        assertTrue(success(DebtSimulator.simulate(withBonus, Strategy.AVALANCHE, start)).months < baseline.months)

        val withRaise = base.copy(
            incomes = listOf(Income(id = 1, name = "Maaş", amount = 8_000.0, type = IncomeType.SALARY, raisesPerYear = 2, raisePercent = 20.0)),
        )
        assertTrue(success(DebtSimulator.simulate(withRaise, Strategy.AVALANCHE, start)).months < baseline.months)
    }

    @Test
    fun cashFlowSavingsSplit() {
        val data = FinanceData(
            incomes = listOf(salary(30_000.0)),
            debts = listOf(Debt(id = 1, name = "Kart", type = DebtType.CREDIT_CARD, balance = 10_000.0, annualInterestRate = 50.0, cardLimit = 20_000.0)),
            expenses = listOf(FixedExpense(id = 1, name = "Kira", amount = 10_000.0)),
            savings = listOf(
                SavingGoal(id = 1, name = "Acil", strategy = SavingStrategy.FIXED, monthlyAmount = 2_000.0),
                SavingGoal(id = 2, name = "Tatil", strategy = SavingStrategy.PERCENTAGE, percent = 50.0),
            ),
        )
        val c = FinanceCalculator.cashFlow(data)
        assertEquals(2_000.0, c.minimumPayments, 0.001) // %20 asgari (limit ≤ 50.000)
        assertEquals(18_000.0, c.netSurplus, 0.001)
        assertEquals(8_000.0, c.percentageSavings, 0.001) // (18.000 - 2.000) * %50
        assertEquals(8_000.0, c.extraPower, 0.001)
        assertEquals(10_000.0, c.debtBudget, 0.001)
    }
}
