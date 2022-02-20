package io.github.aerospike_examples.aero_time_series.benchmark;

import com.aerospike.client.AerospikeClient;
import io.github.aerospike_examples.aero_time_series.Constants;
import io.github.aerospike_examples.aero_time_series.client.DataPoint;

import java.util.*;

/**
 * This runnable will insert data in real time for a specified period
 * It is possible to 'accelerate' time in order to generate inserts at a faster rate
 */
class RealTimeInsertTimeSeriesRunnable extends InsertTimeSeriesRunnable {
    private final int runDurationSeconds;

    private final int accelerationFactor;

    /**
     * Constructor for a runnable that will generate timeSeriesCount time series for us
     * Package level visibility as this will not be used in isolation
     * @param asClient - Aerospike client object
     * @param asNamespace - Aerospike Namespace
     * @param timeSeriesCountPerObject - No of timeseries to generate
     * @param benchmarkClient - Initialise with a benchmarkClient object - some of the config is taken from this
     */
    @SuppressWarnings("SameParameterValue")
    RealTimeInsertTimeSeriesRunnable(AerospikeClient asClient, String asNamespace, String asSet, int timeSeriesCountPerObject, TimeSeriesBenchmarker benchmarkClient){
        this(asClient,asNamespace,asSet,timeSeriesCountPerObject,benchmarkClient,new Random().nextLong());
    }


    /**
     * Constructor for a runnable that will generate timeSeriesCount time series for us
     * Package level visibility as this will not be used in isolation
     * @param asClient - Aerospike Client object
     * @param asNamespace - Aerospike Namespace
     * @param timeSeriesCountPerObject - No of timeseries to generate
     * @param benchmarkClient - Initialise with a benchmarkClient object - some of the config is taken from this
     * @param randomSeed - initialise with a specific seed for deterministic results
     */
    RealTimeInsertTimeSeriesRunnable(AerospikeClient asClient, String asNamespace, String asSet, int timeSeriesCountPerObject, TimeSeriesBenchmarker benchmarkClient, long randomSeed){
        super(asClient, asNamespace, asSet, timeSeriesCountPerObject, benchmarkClient, randomSeed);
        this.runDurationSeconds = benchmarkClient.runDuration;
        this.accelerationFactor = benchmarkClient.accelerationFactor;
    }


    public void run(){
        startTime = System.currentTimeMillis();
        Map<String,Long> lastObservationTimes = new HashMap<>();
        Map<String,Long> nextObservationTimes = new HashMap<>();
        Map<String,Double> lastObservationValues = new HashMap<>();

        for(int i = 0; i< timeSeriesCountPerObject; i++){
            String timeSeriesName = randomTimeSeriesName();
            double observationValue = initTimeSeriesValue();
            lastObservationTimes.put(timeSeriesName,startTime);
            lastObservationValues.put(timeSeriesName,observationValue);
            timeSeriesClient.put(timeSeriesName,new DataPoint(new Date(startTime),observationValue));
            nextObservationTimes.put(timeSeriesName,nextObservationTime(startTime));
        }
        /*
            Put some dummy data in when running in real time mode
            If we don't all the blocks will fill up at the same time which creates a sawtooth effect
            as far as disk use is concerned
            So blocks are primed initially, and the dummy data is removed at the end of the run
         */
        for(String timeSeriesName : lastObservationTimes.keySet()){
            int epochTime = 0;
            // Randomly each initial block to a random extent
            int dummyRecordCount = random.nextInt(timeSeriesClient.getMaxBlockEntryCount());
            DataPoint[] dataPoints = new DataPoint[dummyRecordCount];
            for(int i=0;i<dummyRecordCount;i++){
                // The data points have -ve time and value zero, so they are easily identified
                dataPoints[i] = new DataPoint(new Date(epochTime),0);
                epochTime-=Constants.MILLISECONDS_IN_SECOND;
            }
            timeSeriesClient.put(timeSeriesName,dataPoints);
        }
        while(getSimulationTime() - startTime < (long)runDurationSeconds * Constants.MILLISECONDS_IN_SECOND * accelerationFactor){
            for (String timeSeriesName : nextObservationTimes.keySet()) {
                long nextObservationTime = nextObservationTimes.get(timeSeriesName);
                if (nextObservationTime < getSimulationTime()) {
                    updateCount++;
                    double timeIncrement = (double) (nextObservationTime - lastObservationTimes.get(timeSeriesName)) / Constants.MILLISECONDS_IN_SECOND;
                    double observationValue = simulator.getNextValue(lastObservationValues.get(timeSeriesName), timeIncrement);
                    timeSeriesClient.put(timeSeriesName, new DataPoint(new Date(nextObservationTime), observationValue));
                    lastObservationValues.put(timeSeriesName, observationValue);
                    lastObservationTimes.put(timeSeriesName, nextObservationTime);
                    nextObservationTimes.put(timeSeriesName, nextObservationTime(nextObservationTime));
                }
            }
        }
        isRunning = false;

        // Then remove the dummy records
        for(String timeSeriesName : lastObservationTimes.keySet()) timeSeriesClient.removeDummyRecords(timeSeriesName);
    }

    /**
     * Time may be 'sped up' during our simulation via use of the acceleration factor parameter
     * This function returns the accelerated simulation time represented as a unix epoch, in milliseconds
     *
     * @return current 'simulation time', factoring in acceleration
     */
    private long getSimulationTime(){
        return startTime + (System.currentTimeMillis() - startTime) * accelerationFactor;
    }

}
