package paysim.base;

import org.apache.commons.math3.distribution.NormalDistribution;

public class ClientActionProfile {
    private final String action;
    private final int minCount, maxCount;
    private final double avgAmount, stdAmount;
    private final NormalDistribution timeDistribution;

    public ClientActionProfile(String action, int minCount, int maxCount, double avgAmount, double stdAmount,
                               double avgTime, double stdTime) {
        this.action = action;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.avgAmount = avgAmount;
        this.stdAmount = stdAmount;
        timeDistribution = new NormalDistribution(avgTime, stdTime);
    }

    public String getAction() {
        return action;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public double getAvgAmount() {
        return avgAmount;
    }

    public double getStdAmount() {
        return stdAmount;
    }

    public double getProbabilty(int minutes){
        return timeDistribution.cumulativeProbability(minutes);
    }

}