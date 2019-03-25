package paysim;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.apache.tinkerpop.shaded.jackson.databind.deser.impl.CreatorCandidate;
import paysim.base.StandingOrder;
import sim.engine.SimState;

import paysim.parameters.*;

import paysim.actors.Bank;
import paysim.actors.Client;
import paysim.actors.Fraudster;
import paysim.actors.Merchant;

import paysim.base.Transaction;
import paysim.base.ClientActionProfile;
import paysim.base.StepActionProfile;

import paysim.output.Output;

import paysim.utils.CSVReader;

public class PaySim extends SimState {
    private static final double PAYSIM_VERSION = 1.0;
    private static final String[] DEFAULT_ARGS = new String[]{"", "-file", "PaySim.properties", "1"};
    private static final ArrayList<String> verwenundungszwecke = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5"));

    public final String simulationName;
    private int totalTransactionsMade = 0;
    private int stepParticipated = 0;

    private ArrayList<Client> clients = new ArrayList<>();
    private ArrayList<Merchant> merchants = new ArrayList<>();
    private ArrayList<Fraudster> fraudsters = new ArrayList<>();
    private ArrayList<Bank> banks = new ArrayList<>();

    private ArrayList<String[]> cities;
    private HashMap<String, double[]> distanceMatrix;

    private ArrayList<Transaction> transactions = new ArrayList<>();
    private int currentStep;
    private LocalDateTime currentDateTime;

    private Map<ClientActionProfile, Integer> countProfileAssignment = new HashMap<>();


    public static void main(String[] args) {
        System.out.println("PAYSIM: Financial Simulator v" + PAYSIM_VERSION);
        if (args.length < 4) {
            args = DEFAULT_ARGS;
        }
        int nbTimesRepeat = Integer.parseInt(args[3]);
        String propertiesFile = "";
        for (int x = 0; x < args.length - 1; x++) {
            if (args[x].equals("-file")) {
                propertiesFile = args[x + 1];
            }
        }
        Parameters.initParameters(propertiesFile);
        for (int i = 0; i < nbTimesRepeat; i++) {
            PaySim p = new PaySim();
            p.runSimulation();
        }
    }

    
    
    public PaySim() {
        super(Parameters.getSeed());
        BalancesClients.setRandom(random);
        Parameters.clientsProfiles.setRandom(random);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date currentTime = new Date();
        simulationName = "PS_" + dateFormat.format(currentTime) + "_" + seed();

        File simulationFolder = new File(Parameters.outputPath + simulationName);
        simulationFolder.mkdirs();

        Output.initOutputFilenames(simulationName);
        Output.writeParameters(seed());

        cities = CSVReader.read("paramFiles/citiesFiltered.csv");
        distanceMatrix = generateDistanceMatrix();

        currentDateTime = Parameters.startDate;

    }

    private void runSimulation() {
        System.out.println();
        System.out.println("Starting PaySim Running for " + Parameters.nbSteps + " steps.");
        long startTime = System.currentTimeMillis();
        super.start();

        initCounters();
        initActors();

        while ((currentStep = (int) schedule.getSteps()) < Parameters.nbSteps) {
            if (!schedule.step(this))
                break;

            addHourToCurrentDate();

            writeOutputStep();

            System.out.println("Step :"+ currentStep);
        }

        System.out.println();
        System.out.println("Finished running " + currentStep + " steps ");

        finish();

        double total = System.currentTimeMillis() - startTime;
        total = total / 1000 / 60;
        System.out.println("It took: " + total + " minutes to execute the simulation");
        System.out.println("Simulation name: " + simulationName);
        System.out.println();
    }

    private void initCounters() {
        for (String action : ActionTypes.getActions()) {
            for (ClientActionProfile clientActionProfile : Parameters.clientsProfiles.getProfilesFromAction(action)) {
                countProfileAssignment.put(clientActionProfile, 0);
            }
        }
    }

    private void initActors() {
        System.out.println("Init - Seed " + seed());

        //Add the merchants
        System.out.println("NbMerchants: " + (int) (Parameters.nbMerchants * Parameters.multiplier));
        for (int i = 0; i < Parameters.nbMerchants * Parameters.multiplier; i++) {
            Merchant m = new Merchant(generateId());
            merchants.add(m);
        }

        //Add the fraudsters
        System.out.println("NbFraudsters: " + (int) (Parameters.nbFraudsters * Parameters.multiplier));
        for (int i = 0; i < Parameters.nbFraudsters * Parameters.multiplier; i++) {
            Fraudster f = new Fraudster(generateId());
            fraudsters.add(f);
            schedule.scheduleRepeating(f);
        }

        //Add the banks
        System.out.println("NbBanks: " + Parameters.nbBanks);
        for (int i = 0; i < Parameters.nbBanks; i++) {
            Bank b = new Bank(generateId());
            banks.add(b);
        }

        //Add the clients
        System.out.println("NbClients: " + (int) (Parameters.nbClients * Parameters.multiplier));
        for (int i = 0; i < Parameters.nbClients * Parameters.multiplier; i++) {
            Client c = new Client(generateId(),
                    pickRandomBank(),
                    pickNextClientProfile(),
                    BalancesClients.pickNextBalance(random),
                    pickRandomCity(), random,
                    Parameters.stepsProfiles.getTotalTargetCount());
            clients.add(c);
        }

        //Schedule clients to act at each step of the simulation
        //and add StandingOrders for each Client
        for (Client c : clients) {
            c.setStandingOrders(generateStandingOrders(c.getName()));
            schedule.scheduleRepeating(c);
        }
    }

    private ArrayList<StandingOrder> generateStandingOrders(String nameOrig) {
        ArrayList<String> listOfVWZ = new ArrayList<>(verwenundungszwecke);
        int number = random.nextInt(listOfVWZ.size());
        int randomVWZ;
        ArrayList<StandingOrder> standingOrders = new ArrayList<>();
        for(int i=0; i < number; i++){
            randomVWZ = random.nextInt(listOfVWZ.size());
            standingOrders.add(new StandingOrder(pickRandomClient(nameOrig), listOfVWZ.remove(randomVWZ)));
        }
        return standingOrders;
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

    public void finish() {
        Output.writeFraudsters(fraudsters);
        Output.writeClientsProfiles(countProfileAssignment, (int) (Parameters.nbClients * Parameters.multiplier));
        Output.writeSummarySimulation(this);
    }

    private void resetVariables() {
        if (transactions.size() > 0) {
            stepParticipated++;
        }
        transactions = new ArrayList<>();
    }

    private void writeOutputStep() {
        ArrayList<Transaction> transactions = getTransactions();

        totalTransactionsMade += transactions.size();

        Output.incrementalWriteRawLog(currentStep, transactions);
        if (Parameters.saveToDB) {
            Output.writeDatabaseLog(Parameters.dbUrl, Parameters.dbUser, Parameters.dbPassword, transactions, simulationName);
        }

        Output.incrementalWriteStepAggregate(currentStep, transactions);
        resetVariables();
    }

    public String generateId() {
        final String alphabet = "0123456789";
        final int sizeId = 10;
        StringBuilder idBuilder = new StringBuilder(sizeId);

        for (int i = 0; i < sizeId; i++)
            idBuilder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return idBuilder.toString();
    }

    private HashMap<String, double[]> generateDistanceMatrix(){
        HashMap<String, double[]> distanceMatrix = new HashMap<>();
        double R = 6371;
        int numberOfCities = cities.size();
        for(int i=0; i < numberOfCities; i++){
            double[] distances = new double[numberOfCities];
            for(int j=0; j < numberOfCities; j++){
                double lat_i = Double.parseDouble(cities.get(i)[6]);
                double lon_i = Double.parseDouble(cities.get(i)[7]);
                double lat_j = Double.parseDouble(cities.get(j)[6]);
                double lon_j = Double.parseDouble(cities.get(j)[7]);
                double phi_1 = Math.toRadians(lat_i);
                double phi_2 = Math.toRadians(lat_j);
                double diff_phi = Math.toRadians(lat_j - lat_i);
                double diff_lambda = Math.toRadians(lon_j - lon_i);
                double a = Math.pow(Math.sin(diff_phi/2), 2) + Math.cos(phi_1)*Math.cos(phi_2)*Math.pow(Math.sin(diff_lambda/2), 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
                distances[j] = R * c;
            }
            distanceMatrix.put(cities.get(i)[3], distances);
        }
        return distanceMatrix;
    }

    public Merchant pickRandomMerchant() {
        return merchants.get(random.nextInt(merchants.size()));
    }

    public Bank pickRandomBank() {
        return banks.get(random.nextInt(banks.size()));
    }

    public Client pickRandomClient(String nameOrig) {
        Client clientDest = null;

        String nameDest = nameOrig;
        while (nameOrig.equals(nameDest)){
            clientDest = clients.get(random.nextInt(clients.size()));
            nameDest = clientDest.getName();
        }
        return clientDest;
    }

    public String pickRandomCity(){
        int random_int = random.nextInt(this.cities.size());
        return cities.get(random_int)[3];
    }

    public String pickRandomVerwendungszweck(){
        return verwenundungszwecke.get(random.nextInt(verwenundungszwecke.size()));
    }

    public int getTotalTransactions() {
        return totalTransactionsMade;
    }

    public int getStepParticipated() {
        return stepParticipated;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public ArrayList<Client> getClients() {
        return clients;
    }

    public double[] getDistances(String place) {
        return distanceMatrix.get(place);
    }

    public String getCityByIndex(int index){
        return cities.get(index)[3];
    }

    public int getStepTargetCount() {
        return Parameters.stepsProfiles.getTargetCount(currentStep);
    }

    public Map<String, Double> getStepProbabilities(){
        return Parameters.stepsProfiles.getProbabilitiesPerStep(currentStep);
    }

    public StepActionProfile getStepAction(String action){
        return Parameters.stepsProfiles.getActionForStep(currentStep, action);
    }

    public LocalDateTime getCurrentDate() {
        return currentDateTime;
    }

    public void addHourToCurrentDate(){
        currentDateTime = currentDateTime.plusHours(1);
    }
}