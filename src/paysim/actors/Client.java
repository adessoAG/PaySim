package paysim.actors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.chrono.ChronoLocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.ArrayList;
import static java.lang.Math.max;

import ec.util.MersenneTwisterFast;
import paysim.base.*;
import paysim.parameters.ActionTypes;
import paysim.parameters.BalancesClients;
import sim.engine.SimState;
import sim.engine.Steppable;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;

import paysim.PaySim;
import paysim.parameters.Parameters;
import paysim.utils.RandomCollection;


public class Client extends SuperActor implements Steppable {


    private static final String CLIENT_IDENTIFIER = "C";
    private static final int MIN_NB_TRANSFER_FOR_FRAUD = 3;
    private static final String CASH_IN = "CASH_IN", CASH_OUT = "CASH_OUT", DEBIT = "DEBIT",
            PAYMENT = "PAYMENT", TRANSFER = "TRANSFER", DEPOSIT = "DEPOSIT";
    private final Bank bank;
    private String place;
    private ParetoDistribution movement;
    private PoissonDistribution activity;
    private ClientProfile clientProfile;
    private double balanceMax = 0;
    private int countTransferTransactions = 0;
    private ArrayList<StandingOrder> standingOrders;

    Client(String name, Bank bank, String place) {
        super(CLIENT_IDENTIFIER + name);
        this.bank = bank;
        this.place = place;
    }

    public Client(String name, Bank bank, Map<String, ClientActionProfile> profile, double initBalance,
                  String place, MersenneTwisterFast random) {
        super(CLIENT_IDENTIFIER + name);
        this.bank = bank;
        this.place = place;
        this.movement = new ParetoDistribution(0.0001, (double) 1);
        this.activity = new PoissonDistribution(0.01);
        this.clientProfile = new ClientProfile(profile, random);
        this.balance = initBalance;
        this.overdraftLimit = pickOverdraftLimit(random);
    }


    public String getPlace() {
        return place;
    }

    public void setStandingOrders(ArrayList<StandingOrder> standingOrders){
        this.standingOrders = standingOrders;
    }

    private int getRandomPlace(double[] distances){
        double randomDistance = movement.sample();
        int indexOfCity = 0;
        double minimum = Math.abs(distances[0] - randomDistance);
        for(int i=1; i < distances.length; i++){
            double tmp = Math.abs(distances[i] - randomDistance);
            if(tmp < minimum){
                minimum = tmp;
                indexOfCity = i;
            }
        }
        return indexOfCity;
    }

    @Override
    public void step(SimState state) {
        PaySim paySim = (PaySim) state;
        MersenneTwisterFast random = paySim.random;

        int step = (int) state.schedule.getSteps();

        for(StandingOrder standingOrder : standingOrders){
            if(standingOrder.getNextStep() == step){
                // important notice -  right now the MAX_AMOUNT of a StandingOrder (10.000) is below the TransferLimit (200.000)
                // so we can set amount directly into  the parameter amount but if this changes we need to do like in makeTransaction
                handleTransfer(paySim, step, standingOrder.getAmount(), standingOrder.getClientTo(), place,
                        standingOrder.getVerwendungszweck());
                LocalDateTime nextDateForTransaction = paySim.getCurrentDate().plusMonths(1);
                standingOrder.setNextStep((int)ChronoUnit.HOURS.between(paySim.getCurrentDate(), nextDateForTransaction));
            }
        }
        int count = activity.sample();
        for (int t = 0; t < count; t++) {
            String action = pickAction(random);
            if(isPreferredTime(random, action, (step % 24)*60)) {
                double amount = pickAmount(random, action);
                String randomPlace = paySim.getCityByIndex(getRandomPlace(paySim.getDistances(place)));
                makeTransaction(paySim, step, action, amount, randomPlace);
            }
        }
    }

    private boolean isPreferredTime(MersenneTwisterFast random, String action, int minutes){
        double randomNum = random.nextDouble();
        double probability = clientProfile.getProfilePerAction(action).getProbabilty(minutes);
        return randomNum <= probability;
    }

    private String pickAction(MersenneTwisterFast random) {
        Map<String, Double> clientProbabilities = clientProfile.getActionProbability();
        RandomCollection<String> actionPicker = new RandomCollection<>(random);

        for (Map.Entry<String, Double> clientEntry : clientProbabilities.entrySet()) {
            String action = clientEntry.getKey();
            double clientProbability = clientEntry.getValue();
            actionPicker.add(clientProbability, action);
        }

        return actionPicker.next();
    }

    private double pickAmount(MersenneTwisterFast random, String action) {
        ClientActionProfile clientAmountProfile = clientProfile.getProfilePerAction(action);

        double average = clientAmountProfile.getAvgAmount();
        double std = clientAmountProfile.getStdAmount();

        double amount = -1;
        while (amount <= 0) {
            amount = random.nextGaussian() * std + average;
        }

        return amount;
    }


    private void makeTransaction(PaySim state, int step, String action, double amount, String randomPlace) {
        switch (action) {
            case CASH_IN:
                handleCashIn(state, step, amount, randomPlace);
                break;
            case CASH_OUT:
                handleCashOut(state, step, amount, randomPlace);
                break;
            case DEBIT:
                handleDebit(state, step, amount, randomPlace);
                break;
            case PAYMENT:
                handlePayment(state, step, amount, randomPlace);
                break;
            case TRANSFER:
                Client clientTo = state.pickRandomClient(getName());
                String randomVerwendungszweck = state.pickRandomVerwendungszweck();
                double reducedAmount = amount;
                boolean lastTransferFailed = false;
                while (reducedAmount > Parameters.transferLimit && !lastTransferFailed) {
                    lastTransferFailed = handleTransfer(state, step, Parameters.transferLimit, clientTo,
                            randomPlace, randomVerwendungszweck);
                    reducedAmount -= Parameters.transferLimit;
                }
                if (reducedAmount > 0 && !lastTransferFailed) {
                    handleTransfer(state, step, reducedAmount, clientTo, randomPlace, randomVerwendungszweck);
                }
                break;
            case DEPOSIT:
                handleDeposit(state, step, amount, randomPlace);
                break;
            default:
                throw new UnsupportedOperationException("Action not implemented in Client");
        }
    }

    private void handleCashIn(PaySim paysim, int step, double amount, String randomPlace) {
        Merchant merchantTo = paysim.pickRandomMerchant();
        String nameOrig = this.getName();
        String nameDest = merchantTo.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = merchantTo.getBalance();

        this.deposit(amount);

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = merchantTo.getBalance();

        String verwendungszweck = CASH_IN + "_" + nameOrig + "_" + nameDest;

        Transaction t = new Transaction(step, CASH_IN, amount, nameOrig, randomPlace, paysim.getCurrentDate().plusMinutes(paysim.random.nextInt(60)),
                verwendungszweck, oldBalanceOrig, newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);
        paysim.getTransactions().add(t);
    }

    private void handleCashOut(PaySim paysim, int step, double amount, String randomPlace) {
        Merchant merchantTo = paysim.pickRandomMerchant();
        String nameOrig = this.getName();
        String nameDest = merchantTo.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = merchantTo.getBalance();


        boolean isUnauthorizedOverdraft = this.withdraw(amount);

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = merchantTo.getBalance();

        String verwendungszweck = CASH_OUT + "_" + nameOrig + "_" + nameDest;

        Transaction t = new Transaction(step, CASH_OUT, amount, nameOrig, randomPlace, paysim.getCurrentDate().plusMinutes(paysim.random.nextInt(60)),
                verwendungszweck, oldBalanceOrig, newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

        t.setUnauthorizedOverdraft(isUnauthorizedOverdraft);
        t.setFraud(this.isFraud());
        paysim.getTransactions().add(t);
    }

        private  void handleDebit(PaySim paysim, int step, double amount, String randomPlace) {
        String nameOrig = this.getName();
        String nameDest = this.bank.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = this.bank.getBalance();

        boolean isUnauthorizedOverdraft = this.withdraw(amount);

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = this.bank.getBalance();

        String verwendungszweck = DEBIT + "_" + nameOrig + "_" + nameDest;

        Transaction t = new Transaction(step, DEBIT, amount, nameOrig, randomPlace, paysim.getCurrentDate().plusMinutes(paysim.random.nextInt(60)),
                verwendungszweck, oldBalanceOrig, newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

        t.setUnauthorizedOverdraft(isUnauthorizedOverdraft);
        paysim.getTransactions().add(t);
    }

    private void handlePayment(PaySim paysim, int step, double amount, String randomPlace) {
        Merchant merchantTo = paysim.pickRandomMerchant();

        String nameOrig = this.getName();
        String nameDest = merchantTo.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = merchantTo.getBalance();

        boolean isUnauthorizedOverdraft = this.withdraw(amount);
        if (!isUnauthorizedOverdraft) {
            merchantTo.deposit(amount);
        }

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = merchantTo.getBalance();

        String verwendungszweck = PAYMENT + "_" + nameOrig + "_" + nameDest;

        Transaction t = new Transaction(step, PAYMENT, amount, nameOrig, randomPlace, paysim.getCurrentDate().plusMinutes(paysim.random.nextInt(60)),
                verwendungszweck, oldBalanceOrig, newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

        t.setUnauthorizedOverdraft(isUnauthorizedOverdraft);
        paysim.getTransactions().add(t);
    }

    boolean handleTransfer(PaySim paysim, int step, double amount, Client clientTo, String place, LocalDateTime dateTime, String verwendungszweck) {
        String nameOrig = this.getName();
        String nameDest = clientTo.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = clientTo.getBalance();

        boolean transferFailed;
        if (!isDetectedAsFraud(amount)) {
            boolean isUnauthorizedOverdraft = this.withdraw(amount);
            transferFailed = isUnauthorizedOverdraft;
            if (!isUnauthorizedOverdraft) {
                clientTo.deposit(amount);
            }

            double newBalanceOrig = this.getBalance();
            double newBalanceDest = clientTo.getBalance();

            Transaction t = new Transaction(step, TRANSFER, amount, nameOrig, place, dateTime,
                    verwendungszweck, oldBalanceOrig, newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

            t.setUnauthorizedOverdraft(isUnauthorizedOverdraft);
            t.setFraud(this.isFraud());
            paysim.getTransactions().add(t);
        } else { // create the transaction but don't move any money as the transaction was detected as fraudulent
            transferFailed = true;
            double newBalanceOrig = this.getBalance();
            double newBalanceDest = clientTo.getBalance();

            Transaction t = new Transaction(step, TRANSFER, amount, nameOrig, place, dateTime,
                    verwendungszweck, oldBalanceOrig, newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

            t.setFlaggedFraud(true);
            t.setFraud(this.isFraud());
            paysim.getTransactions().add(t);
        }
        return transferFailed;
    }

    private boolean handleTransfer(PaySim paysim, int step, double amount, Client clientTo, String place, String verwendungszweck) {
        return handleTransfer(paysim, step, amount, clientTo, place,
                paysim.getCurrentDate().plusMinutes(paysim.random.nextInt(60)), verwendungszweck);
    }

    private void handleDeposit(PaySim paysim, int step, double amount, String randomPlace) {
        String nameOrig = this.getName();
        String nameDest = this.bank.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = this.bank.getBalance();

        this.deposit(amount);

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = this.bank.getBalance();

        String verwendungszweck = DEPOSIT + "_" + nameOrig + "_" + nameDest;

        Transaction t = new Transaction(step, DEPOSIT, amount, nameOrig, randomPlace, paysim.getCurrentDate().plusMinutes(paysim.random.nextInt(60)),
                verwendungszweck, oldBalanceOrig, newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

        paysim.getTransactions().add(t);
    }

    private boolean isDetectedAsFraud(double amount) {
        boolean isFraudulentAccount = false;
        if (this.countTransferTransactions >= MIN_NB_TRANSFER_FOR_FRAUD) {
            if (this.balanceMax - this.balance - amount > Parameters.transferLimit * 2.5) {
                isFraudulentAccount = true;
            }
        } else {
            this.countTransferTransactions++;
            this.balanceMax = max(this.balanceMax, this.balance);
        }
        return isFraudulentAccount;
    }

    private double pickOverdraftLimit(MersenneTwisterFast random){
        double averageTransaction = 0, stdTransaction = 0;

        for (String action: ActionTypes.getActions()){
            double actionProbability = clientProfile.getActionProbability().get(action);
            ClientActionProfile actionProfile = clientProfile.getProfilePerAction(action);
            averageTransaction += actionProfile.getAvgAmount() * actionProbability;
            stdTransaction += Math.pow(actionProfile.getStdAmount() * actionProbability, 2);
        }
        stdTransaction = Math.sqrt(stdTransaction);

        double randomizedMeanTransaction = random.nextGaussian() * stdTransaction + averageTransaction;

        return BalancesClients.getOverdraftLimit(randomizedMeanTransaction);
    }
}
