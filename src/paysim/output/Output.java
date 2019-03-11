package paysim.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.*;
import java.io.BufferedReader;
import java.io.FileReader;

import paysim.PaySim;
import paysim.base.StepActionProfile;
import paysim.base.ClientActionProfile;
import paysim.base.Transaction;
import paysim.actors.Fraudster;
import paysim.parameters.Parameters;
import paysim.parameters.StepsProfiles;
import paysim.utils.DatabaseHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.sql.Time;



public class Output {
    public static final int PRECISION_OUTPUT = 2;
    public static final String OUTPUT_SEPARATOR = ",", EOL_CHAR = System.lineSeparator();
    private static String filenameGlobalSummary, filenameParameters, filenameSummary, filenameRawLog,
            filenameStepAggregate, filenameClientProfiles, filenameFraudsters;


    public enum mode_of_payment{

        CASH_IN, Auszahlung
    }

    public enum payment_method
    {
        Automat, Filiale, Geschaeft
    }

    public enum type_of_payment{

        Giro_Card, Kredit_Karte, Bar
    }



    public static boolean getRandomBoolean() {
        Random random = new Random();
        return random.nextBoolean();
    }

    public static void incrementalWriteRawLog(int step, ArrayList<Transaction> transactions) {
        String rawLogHeader = "step,action,amount,nameOrig,place,datetime,oldBalanceOrig,newBalanceOrig,nameDest,oldBalanceDest,newBalanceDest,isFraud,isFlaggedFraud,isUnauthorizedOverdraft";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameRawLog, true));
            if (step == 0) {
                writer.write(rawLogHeader);
                writer.newLine();
            }
            for (Transaction t : transactions) {
                writer.write(t.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void incrementalWriteStepAggregate(int step, ArrayList<Transaction> transactions) {
        String stepAggregateHeader = "action,month,day,hour,count,sum,avg,std,step";
        Map<String, StepActionProfile> stepRecord = Aggregator.generateStepAggregate(step, transactions);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameStepAggregate, true));
            if (step == 0) {
                writer.write(stepAggregateHeader);
                writer.newLine();
            }
            for (StepActionProfile actionRecord : stepRecord.values()) {
                writer.write(actionRecord.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void writeFraudsters(ArrayList<Fraudster> fraudsters) {
        String fraudsterHeader = "name,nbVictims,profit";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameFraudsters));
            writer.write(fraudsterHeader);
            writer.newLine();
            for (Fraudster f : fraudsters) {
                writer.write(f.toString());
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LocalDate writeDate_time(){

        Random random = new Random();
        int minDay = (int) LocalDate.of(2018, 1, 1).toEpochDay();
        int maxDay = (int) LocalDate.of(2019, 1, 1).toEpochDay();
        long randomDay = minDay + random.nextInt(maxDay - minDay);

        LocalDate randomBirthDate = LocalDate.ofEpochDay(randomDay);

        return randomBirthDate;

    }

        //Nicht-Online
    public static String writeAction1(){

        String[] action = new String[5];
        action[0] = "CASH-IN";
        action[1] = "CASH-OUT";
        action[2] = "DEBIT";
        action[3] = "PAYMENT";
        action[4] = "TRANSFER";



        int r = new Random().nextInt(action.length);
        return  action[r];
    }


        // Online
    public static String writeAction2(){

        String[] action = new String[3];
        action[0] = "DEBIT";
        action[1] = "PAYMENT";
        action[2] = "TRANSFER";


        int r = new Random().nextInt(action.length);
        return  action[r];
    }

    public static Time writeVirtual_time(){

            final Random random = new Random();

             final int millisInDay = 24*60*60*1000;
             Time time;
             return new Time((long)random.nextInt(millisInDay));


        }

    public static String write_location(){

          String fileName= "paramFiles/Staedte.csv";
          File file= new File(fileName);

          // this gives you a 2-dimensional array of strings
          List<List<String>> lines = new ArrayList<>();
          Scanner inputStream;

          try{
              inputStream = new Scanner(file);

              while(inputStream.hasNext()){
                  String line= inputStream.next();
                  String[] values = line.split("\\n");
                  // this adds the currently parsed line to the 2-dimensional string array
                  lines.add(Arrays.asList(values));
              }

              inputStream.close();
          }catch (FileNotFoundException e) {
              e.printStackTrace();
          }

                int max = 15000;
                int min = 0;
                Random r= new Random();
                return  lines.get(r.nextInt(max-min)+1).get(0);


    }

    public static mode_of_payment writeM1(){

       int pick = new Random().nextInt(mode_of_payment.values().length);
       return mode_of_payment.values()[pick];
    }

    public static payment_method writeM2(){
        int pick = new Random().nextInt(payment_method.values().length);
        return payment_method.values()[pick];
    }

    public static type_of_payment writeM3(){

        int pick = new Random().nextInt(type_of_payment.values().length);
        return type_of_payment.values()[pick];
    }

    public static String write_ip(){

        Random r = new Random();
        String x =  r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
                return x;

    }

/*
    public static float money_amount(){


        Random r = new Random();



        String result= r.nextFloat(500.0f) + "," + r.nextFloat(99.0f);

        return  Float.parseFloat(result);
    }
*/


    public static void writeParameters(long seed) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameParameters));
            writer.write(Parameters.toString(seed));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeClientsProfiles(Map<ClientActionProfile, Integer> countPerClientActionProfile, int numberClients) {
        String clientsProfilesHeader = "action,high,low,total,freq";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filenameClientProfiles));
            writer.write(clientsProfilesHeader);
            writer.newLine();

            for (Map.Entry<ClientActionProfile, Integer> counterActionProfile : countPerClientActionProfile.entrySet()) {
                ClientActionProfile clientActionProfile = counterActionProfile.getKey();
                String action = clientActionProfile.getAction();
                int count = counterActionProfile.getValue();

                double probability = ((double) count) / numberClients;

                writer.write(action + "," + clientActionProfile.getMinCount() + "," + clientActionProfile.getMaxCount() + ","
                        + count + "," + fastFormatDouble(5, probability));
                writer.newLine();
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeSummarySimulation(PaySim paySim) {
        StringBuilder errorSummary = new StringBuilder();
        StepsProfiles simulationStepsProfiles = new StepsProfiles(Output.filenameStepAggregate, 1 / Parameters.multiplier, Parameters.nbSteps);
        double totalErrorRate = SummaryBuilder.buildSummary(Parameters.stepsProfiles, simulationStepsProfiles, errorSummary);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(Output.filenameSummary));
            writer.write(errorSummary.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String summary = paySim.simulationName + "," + Parameters.nbSteps + "," + paySim.getTotalTransactions() + "," +
                paySim.getClients().size() + "," + totalErrorRate;
        writeGlobalSummary(summary);

        System.out.println("Nb of clients: " + paySim.getClients().size() + " - Nb of steps with transactions: " + paySim.getStepParticipated());
        //System.out.println("time:"+ " " + writeVirtual_time());
    }

    private static void writeGlobalSummary(String summary) {
        String header = "name,steps,nbTransactions,nbClients,totalError";
        File f = new File(filenameGlobalSummary);
        boolean fileExists = f.exists();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(f, true));
            if (!fileExists) {
                writer.write(header);
                writer.newLine();
            }
            writer.write(summary);
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeDatabaseLog(String dbUrl, String dbUser, String dbPassword,
                                        ArrayList<Transaction> transactions, String simulatorName) {
        DatabaseHandler handler = new DatabaseHandler(dbUrl, dbUser, dbPassword);
        for (Transaction t : transactions) {
            handler.insert(simulatorName, t);
        }
    }

    //See https://stackoverflow.com/a/10554128
    private static final int[] POW10 = {1, 10, 100, 1000, 10000, 100000, 1000000};

    public static String fastFormatDouble(int precision, double val) {
        StringBuilder sb = new StringBuilder();
        if (val < 0) {
            sb.append('-');
            val = -val;
        }
        int exp = POW10[precision];
        long lval = (long) (val * exp + 0.5);
        sb.append(lval / exp).append('.');
        long fval = lval % exp;
        for (int p = precision - 1; p > 0 && fval < POW10[p]; p--) {
            sb.append('0');
        }
        sb.append(fval);
        return sb.toString();
    }

    public static String formatBoolean(boolean bool) {
        return bool ? "1" : "0";
    }

    public static void initOutputFilenames(String simulatorName) {
        String outputBaseString = Parameters.outputPath + simulatorName + "//" + simulatorName;
        filenameGlobalSummary = Parameters.outputPath + "summary.csv";

        filenameParameters = outputBaseString + "_PaySim.properties";
        filenameSummary = outputBaseString + "_Summary.txt";

        filenameRawLog = outputBaseString + "_rawLog.csv";
        filenameStepAggregate = outputBaseString + "_aggregatedTransactions.csv";
        filenameClientProfiles = outputBaseString + "_clientsProfiles.csv";
        filenameFraudsters = outputBaseString + "_fraudsters.csv";
    }
}

