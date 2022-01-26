package io.github.ken_tune.aero_time_series;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.InfoPolicy;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.*;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BenchmarkerTest {
    private boolean doTeardown = false;

    // For the avoidance of doubt and clarity
    int MILLISECONDS_IN_SECOND = 1000;

    @Test
    /**
     * Check that the randomly generated time series name is of the expected length
     */
    public void checkTimeSeriesNameGeneration(){
        TimeSeriesBenchmarker benchmarker =
                new TimeSeriesBenchmarker(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,TimeSeriesBenchmarker.DEFAULT_AVERAGE_OBSERVATION_INTERVAL_SECONDS,
                        TimeSeriesBenchmarker.DEFAULT_RUN_DURATION_SECONDS,TimeSeriesBenchmarker.DEFAULT_ACCELERATION_FACTOR,
                        TimeSeriesBenchmarker.DEFAULT_THREAD_COUNT,TimeSeriesBenchmarker.DEFAULT_TIME_SERIES_COUNT);
        TimeSeriesBenchmarkRunnable benchmarkRunnable = new TimeSeriesBenchmarkRunnable(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,1,benchmarker);
        String timeSeriesName = benchmarkRunnable.randomTimeSeriesName();
        Assert.assertTrue(timeSeriesName.length() == benchmarker.timeSeriesNameLength);
    }

    @Test
    /**
     * Check that time series name generation is random
     * 10,000 samples - are any identical
     */
    public void checkTimeSeriesNamesUnique(){
        TimeSeriesBenchmarker benchmarker =
                new TimeSeriesBenchmarker(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,TimeSeriesBenchmarker.DEFAULT_AVERAGE_OBSERVATION_INTERVAL_SECONDS,
                        TimeSeriesBenchmarker.DEFAULT_RUN_DURATION_SECONDS,TimeSeriesBenchmarker.DEFAULT_ACCELERATION_FACTOR,
                        TimeSeriesBenchmarker.DEFAULT_THREAD_COUNT,TimeSeriesBenchmarker.DEFAULT_TIME_SERIES_COUNT);
        TimeSeriesBenchmarkRunnable benchmarkRunnable = new TimeSeriesBenchmarkRunnable(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,1,benchmarker);

        int randomSampleCount = 10000;
        Set<String> s = new HashSet<>();
        for(int i=0;i<randomSampleCount;i++) s.add(benchmarkRunnable.randomTimeSeriesName());
        Assert.assertTrue(s.size() == randomSampleCount);
    }

    /**
     * Basic test to make sure that the benchmarker runs for the expected amount of time and delivers expected updates
     */
    @Test
    public void checkVanillaBenchmarkDurationAndUpdates(){
        int intervalBetweenUpdates = 1;
        int runDurationSeconds = 10;
        int accelerationFactor = 1;
        int threadCount = 1;
        int timeSeriesCount = 1;
        // Set test tolerance to 20% as we are using small numbers - variability could be slightly higher than 10% at any given time
        int testTolerancePct = 20;
        TimeSeriesBenchmarker benchmarker =
                new TimeSeriesBenchmarker(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,intervalBetweenUpdates,runDurationSeconds,accelerationFactor,
                        threadCount,timeSeriesCount);
        benchmarker.run();
        // Check that run time is within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(runDurationSeconds * MILLISECONDS_IN_SECOND,benchmarker.averageThreadRunTimeMs(),testTolerancePct));
        // Check that expected updates are within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(accelerationFactor * runDurationSeconds * timeSeriesCount,benchmarker.totalUpdateCount(),testTolerancePct));
    }

    /**
     * Check that the acceleration factor is observed
     * This can be seen via the number of updates
     */
    @Test
    public void checkAccelerationFactorObserved(){
        int intervalBetweenUpdates = 1;
        int runDurationSeconds = 10;
        int accelerationFactor = 5;
        int threadCount = 1;
        int timeSeriesCount = 1;
        int testTolerancePct = 5;
        TimeSeriesBenchmarker benchmarker =
                new TimeSeriesBenchmarker(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,intervalBetweenUpdates,runDurationSeconds,accelerationFactor,
                        threadCount,timeSeriesCount);
        benchmarker.run();
        // Check that run time is within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(runDurationSeconds * MILLISECONDS_IN_SECOND,benchmarker.averageThreadRunTimeMs(),testTolerancePct));
        // Check that expected updates are within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(accelerationFactor * runDurationSeconds * timeSeriesCount,benchmarker.totalUpdateCount(),testTolerancePct));
    }

    /**
     * Check that thread count can be set to value other than 1 and still get correct results
     * This can be seen via the number of updates
     */
    @Test
    public void checkTimeSeriesCountObserved(){
        int intervalBetweenUpdates = 1;
        int runDurationSeconds = 10;
        int accelerationFactor = 5;
        int threadCount = 5;
        int timeSeriesCount = 100;
        int testTolerancePct = 5;
        TimeSeriesBenchmarker benchmarker =
                new TimeSeriesBenchmarker(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,intervalBetweenUpdates,runDurationSeconds,accelerationFactor,
                        threadCount,timeSeriesCount);
        benchmarker.run();
        // Check that run time is within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(runDurationSeconds * MILLISECONDS_IN_SECOND,benchmarker.averageThreadRunTimeMs(),testTolerancePct));
        // Check that expected updates are within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(accelerationFactor * runDurationSeconds * timeSeriesCount,benchmarker.totalUpdateCount(),testTolerancePct));
    }

    /**
     * Check that the time series count factor is observed
     * This can be seen via the number of updates
     */
    @Test
    public void checkThreadCountObserved(){
        int intervalBetweenUpdates = 1;
        int runDurationSeconds = 10;
        int accelerationFactor = 5;
        int threadCount = 1;
        int timeSeriesCount = 10;
        int testTolerancePct = 5;
        TimeSeriesBenchmarker benchmarker =
                new TimeSeriesBenchmarker(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,intervalBetweenUpdates,runDurationSeconds,accelerationFactor,
                        threadCount,timeSeriesCount);
        benchmarker.run();
        // Check that run time is within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(runDurationSeconds * MILLISECONDS_IN_SECOND,benchmarker.averageThreadRunTimeMs(),testTolerancePct));
        // Check that expected updates are within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(accelerationFactor * runDurationSeconds * timeSeriesCount,benchmarker.totalUpdateCount(),testTolerancePct));
    }
    /**
     * Check that the interval between updates is observed
     * This can be seen via the number of updates
     */
    @Test
    public void checkUpdateIntervalObserved(){
        int intervalBetweenUpdates = 2;
        int runDurationSeconds = 10;
        int accelerationFactor = 5;
        int threadCount = 1;
        int timeSeriesCount = 10;
        int testTolerancePct = 5;
        TimeSeriesBenchmarker benchmarker =
                new TimeSeriesBenchmarker(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,intervalBetweenUpdates,runDurationSeconds,accelerationFactor,
                        threadCount,timeSeriesCount);
        benchmarker.run();
        // Check that run time is within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(runDurationSeconds * MILLISECONDS_IN_SECOND,benchmarker.averageThreadRunTimeMs(),testTolerancePct));
        // Check that expected updates are within tolerance
        Assert.assertTrue(Utilities.valueInTolerance(accelerationFactor * runDurationSeconds * timeSeriesCount / intervalBetweenUpdates,
                benchmarker.totalUpdateCount(),testTolerancePct));
    }

    /**
     * If we exceed the number of key updates we can accommodate per second, is there a warning message?
     */
    @Test
    public void checkHotKeyCheck() throws Exception{
        int intervalBetweenUpdates = 1;
        int runDurationSeconds = 10;
        int accelerationFactor = 101;
        int threadCount = 1;
        int timeSeriesCount = 1;
        double updatesPerSecond = (double) accelerationFactor * timeSeriesCount / intervalBetweenUpdates;
        TimeSeriesBenchmarker benchmarker =
                new TimeSeriesBenchmarker(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,intervalBetweenUpdates,runDurationSeconds,accelerationFactor,
                        threadCount,timeSeriesCount);
        Vector<String> consoleOutput = runBenchmarkerGetOutput(benchmarker);
        Assert.assertTrue(consoleOutput.get(2).equals(
                String.format("!!! Single key updates per second rate %.3f exceeds max recommended rate %d",updatesPerSecond,Constants.SAFE_SINGLE_KEY_UPDATE_LIMIT_PER_SEC)));
    }

    @Test
    /**
     * Check that we get a warning message if the simulation is underflowing the expected rate
     */
    public void checkUnderflowCheck() throws Exception{
        int intervalBetweenUpdates = 1;
        int runDurationSeconds = 10;
        int accelerationFactor = 100;
        int threadCount = 1;
        int timeSeriesCount = 100;
        TimeSeriesBenchmarker benchmarker =
                new TimeSeriesBenchmarker(TestConstants.AEROSPIKE_HOST,TestConstants.AEROSPIKE_NAMESPACE,intervalBetweenUpdates,runDurationSeconds,accelerationFactor,
                        threadCount,timeSeriesCount);
        Vector<String> consoleOutput = runBenchmarkerGetOutput(benchmarker);
        int warningCount = 0;
        for(int i=0;i<consoleOutput.size();i++){
            System.out.println(consoleOutput.get(i));
            if(consoleOutput.get(i).startsWith(
                    String.format("!!!Update rate should be %.3f, actually",benchmarker.expectedUpdatesPerSecond())) && consoleOutput.get(i).endsWith(" - underflow")) warningCount++;
        }
        // We should get a warning on every second, for rounding reasons the first two may not happen
        Assert.assertTrue(warningCount >= runDurationSeconds -2);
    }


    @Test
    /**
     * Check output is as expected for a test case
     * Should see updates per second / updates per second per time series header
     * Should see a status message every second plus an initial status message
     */
    public void correctMainOutput() throws IOException, ParseException, Utilities.ParseException{
        int intervalBetweenUpdates = 2;
        int runDurationSeconds = 10;
        int accelerationFactor = 5;
        int threadCount = 1;
        int timeSeriesCount = 10;

        // Create the string argument array
        String formatString = String.format("-%s %%s -%s %%s -%s %%d -%s %%d -%s %%d -%s %%d -%s %%d",
                OptionsHelper.BenchmarkerFlags.HOST_FLAG, OptionsHelper.BenchmarkerFlags.NAMESPACE_FLAG, OptionsHelper.BenchmarkerFlags.INTERVAL_BETWEEN_OBSERVATIONS_SECONDS_FLAG,
                OptionsHelper.BenchmarkerFlags.RUN_DURATION_FLAG,OptionsHelper.BenchmarkerFlags.ACCELERATION_FLAG,OptionsHelper.BenchmarkerFlags.THREAD_COUNT_FLAG,
                OptionsHelper.BenchmarkerFlags.TIME_SERIES_COUNT_FLAG);

        String commandLineArguments =
                String.format(formatString, TestConstants.AEROSPIKE_HOST, TestConstants.AEROSPIKE_NAMESPACE, intervalBetweenUpdates,
                        runDurationSeconds,accelerationFactor,threadCount,timeSeriesCount);

        // Initialise the benchmarker using the String[]
        TimeSeriesBenchmarker benchmarker = TimeSeriesBenchmarker.initBenchmarkerFromStringArgs(commandLineArguments.split(" "));
        // Capture the console output
        Vector<String> consoleOutput = runBenchmarkerGetOutput(benchmarker);

        // Check the two header messages
        Assert.assertTrue(consoleOutput.get(0).equals(
                String.format("Updates per second : %.3f",(double)accelerationFactor * timeSeriesCount / intervalBetweenUpdates)));
        Assert.assertTrue(consoleOutput.get(1).equals(
                String.format("Updates per second per time series : %.3f",(double)accelerationFactor * timeSeriesCount / intervalBetweenUpdates / timeSeriesCount)));

        // Check we get the expected number of status messages
        Pattern pattern = Pattern.compile("Run time :  \\d+ seconds, Update count : \\d+, Actual updates per second : \\d+.\\d{3}", Pattern.CASE_INSENSITIVE);

        int runTimeMessageCount = 0;
        for(int i=0;i<consoleOutput.size();i++){
            Matcher matcher = pattern.matcher(consoleOutput.get(i));
            if(matcher.find()) runTimeMessageCount++;
        }

        Assert.assertTrue(runTimeMessageCount >= runDurationSeconds+1);
    }

    @After
    // Truncate the time series set
    public void teardown(){
        AerospikeClient asClient = new AerospikeClient(TestConstants.AEROSPIKE_HOST,Constants.DEFAULT_AEROSPIKE_PORT);
        if(doTeardown) {
            asClient.truncate(new InfoPolicy(), TestConstants.AEROSPIKE_NAMESPACE, Constants.AS_TIME_SERIES_SET, null);
            asClient.truncate(new InfoPolicy(), TestConstants.AEROSPIKE_NAMESPACE, Constants.AS_TIME_SERIES_INDEX_SET, null);
        }
    }

    private static Vector<String> runBenchmarkerGetOutput(TimeSeriesBenchmarker benchmarker) throws IOException{
        // Swap stdout for an internal stream
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        benchmarker.output = new PrintStream(bStream);
        // Run the benchmarker
        System.out.println("This test requires console output to be captured. Running ....");
        benchmarker.run();
        System.out.println("Run complete");
        // Get the output
        ByteArrayInputStream binStream = new ByteArrayInputStream(bStream.toByteArray());
        BufferedReader reader =  new BufferedReader(new InputStreamReader(binStream));
        Vector<String> consoleOutput = new Vector<>();
        while(reader.ready()) consoleOutput.addElement(reader.readLine());
        return consoleOutput;
    }

    /**
     * Utility testing method - return a CommandLine object based on command line arguments
     *
     * @param argsString
     * @return CommandLine object
     */
    private static CommandLine commandLineObjectFromString(String argsString) {
        String[] args = argsString.split(" ");
        CommandLine cmdLine = null;
        try {
            cmdLine = OptionsHelper.getArguments(args);
        } catch (ParseException e) {
            Assert.fail("Shouldn't throw a parse exception");
        }
        return cmdLine;
    }

}
