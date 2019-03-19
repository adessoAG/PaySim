package paysim.actors;

import java.util.Map;

import static java.lang.Math.max;

import ec.util.MersenneTwisterFast;
import paysim.parameters.ActionTypes;
import paysim.parameters.BalancesClients;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.distribution.Binomial;
import org.apache.commons.math3.distribution.ParetoDistribution;

import paysim.PaySim;
import paysim.base.ClientActionProfile;
import paysim.base.ClientProfile;
import paysim.base.StepActionProfile;
import paysim.base.Transaction;
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
    private ClientProfile clientProfile;
    private double clientWeight;
    private double balanceMax = 0;
    private int countTransferTransactions = 0;
    //Kundennummer, by default 0
    private String client_number;


    Client(String name, Bank bank, String place) {
        super(CLIENT_IDENTIFIER + name);
        this.bank = bank;
        this.place = place;
    }

    public Client(String name, Bank bank, Map<String, ClientActionProfile> profile, double initBalance,
                  String place, MersenneTwisterFast random, int totalTargetCount, String client_number) {
        super(CLIENT_IDENTIFIER + name);
        this.bank = bank;
        this.place = place;
        this.movement = new ParetoDistribution(0.0001, (double) 1);
        this.clientProfile = new ClientProfile(profile, random);
        this.clientWeight = ((double) clientProfile.getClientTargetCount()) / totalTargetCount;
        this.balance = initBalance;
        this.overdraftLimit = pickOverdraftLimit(random);
        this.client_number = client_number;
    }


    public String getPlace() {
        return place;
    }

    public int getRandomPlace(double[] distances){
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
        int stepTargetCount = paySim.getStepTargetCount();
        if (stepTargetCount > 0) {
            MersenneTwisterFast random = paySim.random;
            int step = (int) state.schedule.getSteps();
            Map<String, Double> stepActionProfile = paySim.getStepProbabilities();

            int count = pickCount(random, stepTargetCount);
            for (int t = 0; t < count; t++) {
                String action = pickAction(random, stepActionProfile);
                StepActionProfile stepAmountProfile = paySim.getStepAction(action);
                int timeInMinutes = (step % 23)*60 + random.nextInt(60);
                if(isPreferredTime(random, action, (step % 23)*60)) {
                    double amount = pickAmount(random, action, stepAmountProfile);
                    String randomPlace = paySim.getCityByIndex(getRandomPlace(paySim.getDistances(place)));
                    makeTransaction(paySim, step, action, amount, timeInMinutes, randomPlace);
                }
            }
        }
    }

    private boolean isPreferredTime(MersenneTwisterFast random, String action, int minutes){
        double randomNum = random.nextDouble();
        double probability = clientProfile.getProfilePerAction(action).getProbabilty(minutes);
        return randomNum <= probability;
    }

    private int pickCount(MersenneTwisterFast random, int targetStepCount) {
        // B(n,p): n = targetStepCount & p = clientWeight
        Binomial transactionNb = new Binomial(targetStepCount, clientWeight, random);
        return transactionNb.nextInt();
    }

    private String pickAction(MersenneTwisterFast random, Map<String, Double> stepActionProb) {
        Map<String, Double> clientProbabilities = clientProfile.getActionProbability();
        RandomCollection<String> actionPicker = new RandomCollection<>(random);

        for (Map.Entry<String, Double> clientEntry : clientProbabilities.entrySet()) {
            String action = clientEntry.getKey();
            double clientProbability = clientEntry.getValue();
            double finalProbability;

            if (stepActionProb.containsKey(action)) {
                double stepProbability = stepActionProb.get(action);

                finalProbability = (clientProbability + stepProbability) / 2;
            } else {
                finalProbability = clientProbability;
            }
            actionPicker.add(finalProbability, action);
        }

        return actionPicker.next();
    }

    private double pickAmount(MersenneTwisterFast random, String action, StepActionProfile stepAmountProfile) {
        ClientActionProfile clientAmountProfile = clientProfile.getProfilePerAction(action);

        double average, std;
        if (stepAmountProfile != null) {
            // We take the mean between the two distributions
            average = (clientAmountProfile.getAvgAmount() + stepAmountProfile.getAvgAmount()) / 2;
            std = Math.sqrt((Math.pow(clientAmountProfile.getStdAmount(), 2) + Math.pow(stepAmountProfile.getStdAmount(), 2))) / 2;
        } else {
            average = clientAmountProfile.getAvgAmount();
            std = clientAmountProfile.getStdAmount();
        }

        double amount = -1;
        while (amount <= 0) {
            amount = random.nextGaussian() * std + average;
        }

        return amount;
    }


        //war zuvor private die Methode, aufpassen!
    public void makeTransaction(PaySim state, int step, String action, double amount, int timeInMinutes, String randomPlace) {
        switch (action) {
            case CASH_IN:
                handleCashIn(state, step, amount, timeInMinutes, randomPlace);
                break;
            case CASH_OUT:
                handleCashOut(state, step, amount, timeInMinutes, randomPlace);
                break;
            case DEBIT:
                handleDebit(state, step, amount, timeInMinutes, randomPlace);
                break;
            case PAYMENT:
                handlePayment(state, step, amount, timeInMinutes, randomPlace);
                break;
            case TRANSFER:
                Client clientTo = state.pickRandomClient(getName());
                double reducedAmount = amount;
                boolean lastTransferFailed = false;
                while (reducedAmount > Parameters.transferLimit && !lastTransferFailed) {
                    lastTransferFailed = handleTransfer(state, step, Parameters.transferLimit, clientTo, timeInMinutes, randomPlace);
                    reducedAmount -= Parameters.transferLimit;
                }
                if (reducedAmount > 0 && !lastTransferFailed) {
                    handleTransfer(state, step, reducedAmount, clientTo, timeInMinutes, randomPlace);
                }
                break;
            case DEPOSIT:
                handleDeposit(state, step, amount, timeInMinutes, randomPlace);
                break;
            default:
                throw new UnsupportedOperationException("Action not implemented in Client");
        }
    }

        //war private vorher
         public void handleCashIn(PaySim paysim, int step, double amount, int timeInMinutes, String randomPlace) {
        Merchant merchantTo = paysim.pickRandomMerchant();
        String nameOrig = this.getName();
        String nameDest = merchantTo.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = merchantTo.getBalance();

        this.deposit(amount);

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = merchantTo.getBalance();

        Transaction t = new Transaction(step, CASH_IN, amount, nameOrig, randomPlace, timeInMinutes, oldBalanceOrig,
                newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest );
        paysim.getTransactions().add(t);
    }

    public void handleCashOut(PaySim paysim, int step, double amount, int timeInMinutes, String randomPlace) {
        Merchant merchantTo = paysim.pickRandomMerchant();
        String nameOrig = this.getName();
        String nameDest = merchantTo.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = merchantTo.getBalance();


        boolean isUnauthorizedOverdraft = this.withdraw(amount);

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = merchantTo.getBalance();

        Transaction t = new Transaction(step, CASH_OUT, amount, nameOrig, randomPlace, timeInMinutes, oldBalanceOrig,
                newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

        t.setUnauthorizedOverdraft(isUnauthorizedOverdraft);
        t.setFraud(this.isFraud());
        paysim.getTransactions().add(t);
    }

        private  void handleDebit(PaySim paysim, int step, double amount, int timeInMinutes, String randomPlace) {
        String nameOrig = this.getName();
        String nameDest = this.bank.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = this.bank.getBalance();

        boolean isUnauthorizedOverdraft = this.withdraw(amount);

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = this.bank.getBalance();

        Transaction t = new Transaction(step, DEBIT, amount, nameOrig, place, timeInMinutes, oldBalanceOrig,
                newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

        t.setUnauthorizedOverdraft(isUnauthorizedOverdraft);
        paysim.getTransactions().add(t);
    }

    private void handlePayment(PaySim paysim, int step, double amount, int timeInMinutes, String randomPlace) {
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

        Transaction t = new Transaction(step, PAYMENT, amount, nameOrig, place, timeInMinutes, oldBalanceOrig,
                newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

        t.setUnauthorizedOverdraft(isUnauthorizedOverdraft);
        paysim.getTransactions().add(t);
    }

    boolean handleTransfer(PaySim paysim, int step, double amount, Client clientTo, int timeInMinutes, String place) {
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

            Transaction t = new Transaction(step, TRANSFER, amount, nameOrig, place, timeInMinutes, oldBalanceOrig,
                    newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

            t.setUnauthorizedOverdraft(isUnauthorizedOverdraft);
            t.setFraud(this.isFraud());
            paysim.getTransactions().add(t);
        } else { // create the transaction but don't move any money as the transaction was detected as fraudulent
            transferFailed = true;
            double newBalanceOrig = this.getBalance();
            double newBalanceDest = clientTo.getBalance();

            Transaction t = new Transaction(step, TRANSFER, amount, nameOrig, place, timeInMinutes, oldBalanceOrig,
                    newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

            t.setFlaggedFraud(true);
            t.setFraud(this.isFraud());
            paysim.getTransactions().add(t);
        }
        return transferFailed;
    }

    private void handleDeposit(PaySim paysim, int step, double amount, int timeInMinutes, String randomPlace) {
        String nameOrig = this.getName();
        String nameDest = this.bank.getName();
        double oldBalanceOrig = this.getBalance();
        double oldBalanceDest = this.bank.getBalance();

        this.deposit(amount);

        double newBalanceOrig = this.getBalance();
        double newBalanceDest = this.bank.getBalance();

        Transaction t = new Transaction(step, DEPOSIT, amount, nameOrig, randomPlace, timeInMinutes, oldBalanceOrig,
                newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);

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
