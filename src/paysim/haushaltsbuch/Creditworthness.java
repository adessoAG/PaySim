package paysim.haushaltsbuch;
import ec.util.MersenneTwisterFast;
import paysim.base.ClientActionProfile;
import paysim.base.Transaction;
import paysim.actors.*;
import paysim.parameters.ActionTypes;
import paysim.parameters.BalancesClients;
import paysim.parameters.Parameters;
import paysim.actors.Client;
import sim.engine.*;
import paysim.PaySim;
import paysim.parameters.ActionTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


                /*
                In dieser Klasse werden SIMPLE Funktionen für die Ermittlung der Kreditwürdigkeit eines Kunden bereitgestellt.
                Ziel ist es erst einmal eine simple Funktion ins System einzubinden. Später werden alle anderen Aspekte wie z.B
                Kundenverhalten (zeitliche & örtliche Präferenzen, Transaktionstyp usw.) eingebaut und mittels der Information die Bonität bestimmt.

                */

/*
public class Creditworthness {


    public enum aggregation {
        HIGH,
        MEDIUM,
        LOW
    }
    public MersenneTwisterFast random;
    public Schedule schedule;
    private ArrayList<Client> clients = new ArrayList<>();
    private ArrayList<Bank> banks = new ArrayList<>();
    private Map<ClientActionProfile, Integer> countProfileAssignment = new HashMap<>();
    Client c;





    public String generateId() {
        final String alphabet = "0123456789";
        final int sizeId = 10;
        StringBuilder idBuilder = new StringBuilder(sizeId);

        for (int i = 0; i < sizeId; i++)
            idBuilder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return idBuilder.toString();
    }
    public String generate_client_nr(){

        int max=1000;
        int min=1;
        int range = max-min;

        int rand = (int)(Math.random() * range) + min;

        return String.valueOf(rand);

    }
    public Bank pickRandomBank() {
        return banks.get(random.nextInt(banks.size()));
    }

    private Map<String, ClientActionProfile> pickNextClientProfile() {
        Map<String, ClientActionProfile> profile = new HashMap<>();

        for (String action : ActionTypes.getActions()) {
            ClientActionProfile clientActionProfile = Parameters.clientsProfiles.pickNextActionProfile(action);

            profile.put(action, clientActionProfile);

            int count = countProfileAssignment.get(clientActionProfile);
            countProfileAssignment.put(clientActionProfile, count + 1);
        }
        return profile;
    }


    //default constructor
    public Creditworthness() {}



    //input Types of Payments(type String) from the class Client, amount of transactions,
    //simple sub-method which determines to which class the client belongs to.
    public aggregation aggregate(String cash_in, String cash_out, String debit, String payment) {


        int revenues = 0;
        int expenses = 0;


            revenues += Integer.parseInt(cash_in + debit); //Einzahlungen
            expenses += Integer.parseInt(cash_out + debit + payment);  //Auszahlungen


        int div = (revenues / expenses);
        return (div >= 0 && div <= 1500) ? aggregation.LOW : (div > 1500 && div <= 3000) ? aggregation.MEDIUM : aggregation.HIGH;


    }



    //
    public aggregation assign(int step, String action, double amount, String nameOrig, double oldBalanceOrig,
                                  double newBalanceOrig, String nameDest, double oldBalanceDest, double newBalanceDest, PaySim paySim) {


                 Transaction t = new Transaction(step, action, amount, nameOrig, oldBalanceOrig, newBalanceOrig, nameDest, oldBalanceDest, newBalanceDest);





                    for (int i = 0; i < Parameters.nbClients * Parameters.multiplier; i++) {
                        c = new Client(generateId(),
                                pickRandomBank(),
                                pickNextClientProfile(),
                                BalancesClients.pickNextBalance(random),
                                random,
                                Parameters.stepsProfiles.getTotalTargetCount(),
                                generate_client_nr());
                                //c.handleCashIn(paySim,step,amount);
                                //c.handleDebit(paySim,step,amount);
                                //c.handleCashOut(paySim,step,amount);
                                //c.handlePayment(paySim,step,amount);
                                clients.add(c);

                    }


                    //Schedule clients to act at each step of the simulation
                    for (Client c1 : clients) {
                        schedule.scheduleRepeating(c1);
                        c1.makeTransaction(paySim, t.getStep(), t.getAction(), t.getAmount());

                    }



                    return aggregate(Parameters.transactionsTypes,Parameters.transactionsTypes, Parameters.transactionsTypes, Parameters.transactionsTypes );





    }


}

*/