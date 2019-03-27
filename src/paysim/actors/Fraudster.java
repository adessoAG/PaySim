package paysim.actors;

import java.time.LocalDateTime;
import java.util.ArrayList;

import sim.engine.SimState;
import sim.engine.Steppable;

import paysim.PaySim;
import paysim.parameters.Parameters;
import paysim.output.Output;

public class Fraudster extends SuperActor implements Steppable {
    private static final String FRAUDSTER_IDENTIFIER = "C";
    private double profit = 0;
    private int nbVictims = 0;

    public Fraudster(String name) {
        super(FRAUDSTER_IDENTIFIER + name);
    }

    @Override
    public void step(SimState state) {
        PaySim paysim = (PaySim) state;
        int step = (int) state.schedule.getSteps();
        if (paysim.random.nextDouble() < Parameters.fraudProbability) {
            Client c = paysim.pickRandomClient(getName());
            String fraudCity = paysim.pickRandomCity();
            c.setFraud(true);
            double balance = c.getBalance();
            if (balance > 0) {
                int nbTransactions = (int) Math.ceil(balance / Parameters.transferLimit);
                for (int i = 0; i < nbTransactions; i++) {
                    boolean transferFailed;
                    Mule muleClient = new Mule(paysim.generateId(), paysim.pickRandomBank(), fraudCity);
                    String verwendungszweck = paysim.pickRandomVerwendungszweck();
                    LocalDateTime dateTime = paysim.getCurrentDate().plusMinutes(paysim.random.nextInt(60));
                    muleClient.setFraud(true);
                    if (balance > Parameters.transferLimit) {
                        transferFailed = c.handleTransfer(paysim, step, Parameters.transferLimit, muleClient, fraudCity,
                                dateTime, verwendungszweck);
                        balance -= Parameters.transferLimit;
                    } else {
                        transferFailed = c.handleTransfer(paysim, step, balance, muleClient, fraudCity, dateTime,
                                verwendungszweck);
                        balance = 0;
                    }

                    profit += muleClient.getBalance();
                    muleClient.fraudulentCashOut(paysim, step, muleClient.getBalance(), dateTime);
                    nbVictims++;
                    paysim.getClients().add(muleClient);
                    if (transferFailed)
                        break;
                }
            }
            c.setFraud(false);
        }
    }

    @Override
    public String toString() {
        ArrayList<String> properties = new ArrayList<>();

        properties.add(getName());
        properties.add(Integer.toString(nbVictims));
        properties.add(Output.fastFormatDouble(Output.PRECISION_OUTPUT, profit));

        return String.join(Output.OUTPUT_SEPARATOR, properties);
    }
}
