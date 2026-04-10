package com.sensordata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorDataProcessorTest {

    @AfterEach
    void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get("RacingStatsData.txt"));
    }

    /**
     * B1: average(data2[i][j]) > 10 && < 50 → break
     *
     * data[0][0][0]=80, d=2, limit=0 → data2=40, avg=40 → B1 true → break
     */
    @Test
    void testB1_avgInRange_breaks() {
        double[][][] data = {{{80.0}}};
        double[][] limit = {{0.0}};
        new SensorDataProcessor(data, limit).calculate(2.0);
        assertTrue(Files.exists(Paths.get("RacingStatsData.txt")));
    }

    /**
     * B1 first sub-condition false (avg <= 10) → falls to B2.
     * B2: data2 > data → break.
     *
     * data[0][0][0]=5, d=0.5, limit=0 → data2=10, avg=10 (not >10 → B1 false)
     * max(5,10)=10 > 5 → B2 true → break
     */
    @Test
    void testB1FirstCondFalse_thenB2_breaks() {
        double[][][] data = {{{5.0}}};
        double[][] limit = {{0.0}};
        new SensorDataProcessor(data, limit).calculate(0.5);
        assertTrue(Files.exists(Paths.get("RacingStatsData.txt")));
    }

    /**
     * B1 first sub-condition true but second false (avg >= 50) → falls to B2.
     * B2: data2 <= data → false. B3: |data|==|data2| → false → else continue (B4).
     * Also covers the for-loop false condition (normal loop exit).
     *
     * data[0][0][0]=150, d=1, limit=0 → data2=150, avg=150
     * avg>10 true, avg<50 false → B1b falls through
     * max(150,150)=150 > 150? No → B2 false
     * |150|^3 < |150|^3? No → B3 false → else continue (B4)
     * Loop exits normally → for-loop false branch covered
     */
    @Test
    void testB1SecondCondFalse_B2False_B3False_B4Continue() {
        double[][][] data = {{{150.0}}};
        double[][] limit = {{0.0}};
        new SensorDataProcessor(data, limit).calculate(1.0);
        assertTrue(Files.exists(Paths.get("RacingStatsData.txt")));
    }

    /**
     * B3: |data[k]|^3 < |data2[k]|^3 && avg(data[i][j]) < data2[k] → data2 *= 2
     * Also covers B4 (else continue) for subsequent k iterations.
     *
     * data[0][0] = [5.0, -30.0, -30.0], d=1, limit=4 → limit^2=16
     * k=0: data2 = 5-16 = -11; avg(data2)=(-11+0+0)/3≈-3.67 → B1 false
     *       data2=-11 <= data=5 → B2 false
     *       |5|^3=125 < |-11|^3=1331 ✓; avg(data)=(5-30-30)/3≈-18.3 < -11 ✓ → B3 true
     * k=1: data2 = -30-16 = -46 → B2 false; avg(data)=-18.3 < -46? No → B4 continue
     * k=2: data2 = -46 → B4 continue; loop exits normally
     */
    @Test
    void testB3_multipliesData2_andB4_continue() {
        double[][][] data = {{{5.0, -30.0, -30.0}}};
        double[][] limit = {{4.0}};
        new SensorDataProcessor(data, limit).calculate(1.0);
        assertTrue(Files.exists(Paths.get("RacingStatsData.txt")));
    }

    /**
     * B5: Exception/catch block.
     *
     * limit=null → NullPointerException at Math.pow(limit[i][j], 2.0) inside try → catch block runs.
     * calculate() swallows the exception and prints it.
     */
    @Test
    void testB5_exception_catchBlock() {
        double[][][] data = {{{1.0}}};
        new SensorDataProcessor(data, null).calculate(1.0);
        // No assertion needed — just verifying no uncaught exception escapes
    }
}
