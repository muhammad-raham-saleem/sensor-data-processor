package com.sensordata;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class SensorDataProcessor {

    // Senson data and limits.
    public double[][][] data;
    public double[][] limit;

    // constructor
    public SensorDataProcessor(double[][][] data, double[][] limit) {
        this.data = data;
        this.limit = limit;
    }

    // calculates average of sensor data
    private double average(double[] array) {
        int i = 0;
        double val = 0;
        for (i = 0; i < array.length; i++) {
            val += array[i];
        }

        return val / array.length;
    }

    // calculate data
    public void calculate(double d) {

        long startTime = System.nanoTime();

        int i, j, k = 0;
        double[][][] data2 = new double[data.length][data[0].length][data[0][0].length];

        BufferedWriter out;

        // Write racing stats data into a file
        try {
            out = new BufferedWriter(new FileWriter("RacingStatsData.txt"));

            for (i = 0; i < data.length; i++) {
                for (j = 0; j < data[0].length; j++) {
                    double limSq = limit[i][j] * limit[i][j];// caching this to reduce the number of times it gets
                                                             // recomputed. the value doesnt change in the k loop so its
                                                             // unecesary
                    double avgdata1 = average(data[i][j]);// caching avgdata 1 as it stays the same and does not need to
                                                          // be recomputed in k loop
                    for (k = 0; k < data[0][0].length; k++) {
                        data2[i][j][k] = data[i][j][k] / d - limSq;
                        double avgdata2 = average(data2[i][j]);// compute once per k after updating data2[i][j][k]

                        if (avgdata2 > 10 && avgdata2 < 50)
                            break;
                        else if (data2[i][j][k] > data[i][j][k]) // Math.max(a,b) > a is equivalent to b > a, removed unnecessary call
                            break;
                        else if (Math.pow(Math.abs(data[i][j][k]), 3) < Math.pow(Math.abs(data2[i][j][k]), 3)
                                && avgdata1 < data2[i][j][k]) // REMOVED && (i + 1) * (j + 1) > 0 because its alwasy
                                                              // true
                            data2[i][j][k] *= 2;
                        else
                            continue;
                    }
                }
            }

            for (i = 0; i < data2.length; i++) {
                for (j = 0; j < data2[0].length; j++) {
                    out.write(data2[i][j] + "\t");
                }
            }

            out.close();

            long endTime = System.nanoTime();
            long elapsedMs = (endTime - startTime) / 1_000_000;
            System.out.println("calculate() completed in " + elapsedMs + " ms");

        } catch (Exception e) {
            System.out.println("Error= " + e);
            long endTime = System.nanoTime();
            long elapsedMs = (endTime - startTime) / 1_000_000;
            System.out.println("calculate() failed after " + elapsedMs + " ms");
        }
    }

}