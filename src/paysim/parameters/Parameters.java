package paysim.parameters;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Properties;

import paysim.output.Output;

public class Parameters {
    private static String seedString;
    public static int nbClients, nbMerchants, nbBanks, nbFraudsters, nbSteps;
    public static LocalDateTime startDate;
    public static double multiplier, fraudProbability, transferLimit;
    public static String aggregatedTransactions, maxOccurrencesPerClient, initialBalancesDistribution,
            overdraftLimits, clientsProfilesFile, transactionsTypes;
    public static String outputPath;

    public static StepsProfiles stepsProfiles;
    public static ClientsProfiles clientsProfiles;

    public static void initParameters(String propertiesFile) {
        loadPropertiesFile(propertiesFile);

        ActionTypes.loadActionTypes(transactionsTypes);
        BalancesClients.initBalanceClients(initialBalancesDistribution);
        BalancesClients.initOverdraftLimits(overdraftLimits);
        clientsProfiles = new ClientsProfiles(clientsProfilesFile);
        stepsProfiles = new StepsProfiles(aggregatedTransactions, multiplier, nbSteps);
        ActionTypes.loadMaxOccurrencesPerClient(maxOccurrencesPerClient);
    }

    private static void loadPropertiesFile(String propertiesFile) {
        try {
            Properties parameters = new Properties();
            parameters.load(new FileInputStream(propertiesFile));

            seedString = String.valueOf(parameters.getProperty("seed"));
            nbSteps = Integer.parseInt(parameters.getProperty("nbSteps"));
            multiplier = Double.parseDouble(parameters.getProperty("multiplier"));

            nbClients = Integer.parseInt(parameters.getProperty("nbClients"));
            nbFraudsters = Integer.parseInt(parameters.getProperty("nbFraudsters"));
            nbMerchants = Integer.parseInt(parameters.getProperty("nbMerchants"));
            nbBanks = Integer.parseInt(parameters.getProperty("nbBanks"));

            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("dd.MM.yyyy[ HH:mm]")
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .toFormatter();
            startDate = LocalDateTime.parse(parameters.getProperty("startDate"), formatter);

            fraudProbability = Double.parseDouble(parameters.getProperty("fraudProbability"));
            transferLimit = Double.parseDouble(parameters.getProperty("transferLimit"));

            transactionsTypes = parameters.getProperty("transactionsTypes");
            aggregatedTransactions = parameters.getProperty("aggregatedTransactions");
            maxOccurrencesPerClient = parameters.getProperty("maxOccurrencesPerClient");
            initialBalancesDistribution = parameters.getProperty("initialBalancesDistribution");
            overdraftLimits = parameters.getProperty("overdraftLimits");
            clientsProfilesFile = parameters.getProperty("clientsProfiles");

            outputPath = parameters.getProperty("outputPath");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getSeed() {
        // /!\ MASON seed is using an int internally
        // https://github.com/eclab/mason/blob/66d38fa58fae3e250b89cf6f31bcfa9d124ffd41/mason/sim/engine/SimState.java#L45
        if (seedString.equals("time")) {
            return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        } else {
            return Integer.parseInt(seedString);
        }
    }

    public static String toString(long seed) {
        ArrayList<String> properties = new ArrayList<>();

        properties.add("seed=" + seed);
        properties.add("nbSteps=" + nbSteps);
        properties.add("multiplier=" + multiplier);
        properties.add("nbFraudsters=" + nbFraudsters);
        properties.add("nbMerchants=" + nbMerchants);
        properties.add("startDate=" +  startDate);
        properties.add("fraudProbability=" + fraudProbability);
        properties.add("transferLimit=" + transferLimit);
        properties.add("transactionsTypes=" + transactionsTypes);
        properties.add("aggregatedTransactions=" + aggregatedTransactions);
        properties.add("clientsProfilesFile=" + clientsProfilesFile);
        properties.add("initialBalancesDistribution=" + initialBalancesDistribution);
        properties.add("maxOccurrencesPerClient=" + maxOccurrencesPerClient);
        properties.add("outputPath=" + outputPath);

        return String.join(Output.EOL_CHAR, properties);
    }
}
