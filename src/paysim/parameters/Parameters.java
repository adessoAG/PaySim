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
    public static String maxOccurrencesPerClient, initialBalancesDistribution,
            overdraftLimits, clientsProfilesFile, transactionsTypes;
    public static String outputPath;

    public static ClientsProfiles clientsProfiles;

    public static double movementShape, activityMean;

    public static void initParameters(String propertiesFile) {
        loadPropertiesFile(propertiesFile);

        ActionTypes.loadActionTypes(transactionsTypes);
        BalancesClients.initBalanceClients(initialBalancesDistribution);
        BalancesClients.initOverdraftLimits(overdraftLimits);
        clientsProfiles = new ClientsProfiles(clientsProfilesFile);
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
            maxOccurrencesPerClient = parameters.getProperty("maxOccurrencesPerClient");
            initialBalancesDistribution = parameters.getProperty("initialBalancesDistribution");
            overdraftLimits = parameters.getProperty("overdraftLimits");
            clientsProfilesFile = parameters.getProperty("clientsProfiles");

            movementShape = Double.parseDouble(parameters.getProperty("movementShape"));
            activityMean = Double.parseDouble(parameters.getProperty("activityMean"));

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
        properties.add("movementShape=" + movementShape);
        properties.add("activityMean=" + activityMean);
        properties.add("transactionsTypes=" + transactionsTypes);
        properties.add("clientsProfilesFile=" + clientsProfilesFile);
        properties.add("initialBalancesDistribution=" + initialBalancesDistribution);
        properties.add("maxOccurrencesPerClient=" + maxOccurrencesPerClient);
        properties.add("outputPath=" + outputPath);

        return String.join(Output.EOL_CHAR, properties);
    }
}
