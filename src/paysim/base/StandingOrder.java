package paysim.base;

import java.util.Random;
import paysim.actors.Client;

public class StandingOrder {
    /**
     * The class is a data storage for standing orders. The class Client will save a list of standing orders to be able
     * to perform continous transactions based on the standing order.
     * The attributes nextStep and amount will generate by the class because they do depend on other actors. We want to
     * avoid the same verwendungszweck for every StandingOrder for the same Client. Thus PaySim will pick a
     * appropriate verwendungszeck.
     * Since its function is data storage aside from getter and setter there are no real methods.
     */

    private static final int BOUND_IN_HOUR = 720;
    private static final double MAX_AMOUNT = 10000;

    private double amount;
    private Client clientTo;
    private int nextStep;
    private String verwendungszweck;

    public StandingOrder(Client clientTo, String verwendungszweck){
        this.clientTo = clientTo;
        this.verwendungszweck = verwendungszweck;
        this.nextStep = new Random().nextInt(BOUND_IN_HOUR);
        this.amount = new Random().nextDouble() * MAX_AMOUNT;
    }

    public int getNextStep() {
        return nextStep;
    }

    public Client getClientTo() {
        return clientTo;
    }

    public double getAmount() {
        return amount;
    }

    public void setNextStep(int steps) {
        nextStep += steps;
    }

    public String getVerwendungszweck() {
        return verwendungszweck;
    }
}
