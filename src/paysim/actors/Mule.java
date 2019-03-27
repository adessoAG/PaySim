package paysim.actors;

import paysim.PaySim;
import paysim.base.Transaction;

import java.time.LocalDateTime;

public class Mule extends Client {
    private static final String MULE_IDENTIFIER = "C";

    public Mule(String name, Bank bank, String place) {
        super(MULE_IDENTIFIER + name, bank, place);
        this.overdraftLimit = 0;
    }

    void fraudulentCashOut(PaySim paysim, int step, double amount, LocalDateTime dateTime) {
        String action = "CASH_OUT";

        Merchant merchantTo = paysim.pickRandomMerchant();
        String nameOrig = this.getName();
        String nameDest = merchantTo.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = merchantTo.getBalance();

        this.withdraw(amount);

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = merchantTo.getBalance();

        String verwendungszweck = action + "_" + nameOrig + "_" + nameDest;

        Transaction t = new Transaction(step, action, amount, nameOrig, getPlace(),
                dateTime.plusMinutes(paysim.random.nextInt(30)), verwendungszweck, oldBalanceOrig,
                newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);
        t.setFraud(this.isFraud());
        paysim.getTransactions().add(t);
    }
}
