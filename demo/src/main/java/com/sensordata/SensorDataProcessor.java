package com.sensordata;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.stream.IntStream;

public class SensorDataProcessor{

    // Senson data and limits.
    public double[][][] data;
    public double[][] limit;

    // constructor
    public SensorDataProcessor(double[][][] data, double[][] limit) {
        this.data = data;
        this.limit = limit;
    }

    // calculate data
    public void calculate(double d) {

        long startTime = System.nanoTime();

        int iLen = data.length;
        int jLen = data[0].length;
        int kLen = data[0][0].length;
        double invD = 1.0 / d;

        double[][][] data2 = new double[iLen][jLen][kLen];
        // Threshold comparisons avoid a per-iteration division: (sum/kLen)>10 ≡ sum>10*kLen
        double loThresh = 10.0 * kLen;
        double hiThresh = 50.0 * kLen;
        double invKLen  = 1.0 / kLen;

        // Write racing stats data into a file
        try (BufferedWriter out = new BufferedWriter(new FileWriter("RacingStatsData.txt"), 1 << 16)) {

            // Compute phase: flatten i×j so ForkJoinPool can saturate all cores even when iLen is small
            IntStream.range(0, iLen * jLen).parallel().forEach(ij -> {
                int i = ij / jLen;
                int j = ij % jLen;
                double[] srcRow = data[i][j];
                double[] dstRow = data2[i][j];
                double limitSq = limit[i][j] * limit[i][j];

                // Inlined average: avoids method call overhead and reuses kLen directly
                double sumSrc = 0.0;
                for (int k = 0; k < kLen; k++) sumSrc += srcRow[k];
                double avgSrc = sumSrc * invKLen;

                double runningSum = 0.0;

                for (int k = 0; k < kLen; k++) {
                    double src = srcRow[k];
                    double val = src * invD - limitSq;
                    double rsPlusVal = runningSum + val;

                    if (rsPlusVal > loThresh && rsPlusVal < hiThresh) {
                        dstRow[k] = val;
                        break;
                    } else if (val > src) {
                        dstRow[k] = val;
                        break;
                    }
                    // Write dstRow[k] exactly once — eliminates upfront write + potential overwrite
                    double finalVal = (src * src < val * val && avgSrc < val) ? val * 2 : val;
                    dstRow[k] = finalVal;
                    runningSum += finalVal;
                }
            });

            // Write phase: sequential — BufferedWriter is not thread-safe
            for (int i = 0; i < iLen; i++) {
                for (int j = 0; j < jLen; j++) {
                    out.write(data2[i][j] + "\t");
                }
            }

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