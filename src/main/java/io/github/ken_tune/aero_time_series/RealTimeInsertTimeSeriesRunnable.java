package io.github.ken_tune.aero_time_series;

import java.util.*;

/**
 * This runnable will insert data in real time for a specified period
 * It is possible to 'accelerate' time in order to generate inserts at a faster rate
 */
class RealTimeInsertTimeSeriesRunnable extends InsertTimeSeriesRunnable{
    private int runDurationSeconds;

    private int accelerationFactor;

    /**
     * Constructor for a runnable that will generate timeSeriesCount time series for us
     * Package level visibility as this will not be used in isolation
     * @param asHost - Aerospike Host
     * @param asNamespace - Aerospike Namespace
     * @param timeSeriesCountPerObject - No of timeseries to generate
     * @param benchmarkClient - Initialise with a benchmarkClient object - some of the config is taken from this
     */
    RealTimeInsertTimeSeriesRunnable(String asHost, String asNamespace, int timeSeriesCountPerObject, TimeSeriesBenchmarker benchmarkClient){
        this(asHost,asNamespace,timeSeriesCountPerObject,benchmarkClient,new Random().nextLong());
    }


    /**
     * Constructor for a runnable that will generate timeSeriesCount time series for us
     * Package level visibility as this will not be used in isolation
     * @param asHost - Aerospike Host
     * @param asNamespace - Aerospike Namespace
     * @param timeSeriesCountPerObject - No of timeseries to generate
     * @param benchmarkClient - Initialise with a benchmarkClient object - some of the config is taken from this
     * @param randomSeed - initialise with a specific seed for deterministic results
     */
    RealTimeInsertTimeSeriesRunnable(String asHost, String asNamespace, int timeSeriesCountPerObject, TimeSeriesBenchmarker benchmarkClient, long randomSeed){
        super(asHost, asNamespace, timeSeriesCountPerObject, benchmarkClient, randomSeed);
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
        isRunning = true;
        while(getSimulationTime() - startTime < (long)runDurationSeconds * Constants.MILLISECONDS_IN_SECOND * accelerationFactor){
            Iterator<String> timeSeriesNames = nextObservationTimes.keySet().iterator();
            while(timeSeriesNames.hasNext()){
                String timeSeriesName = timeSeriesNames.next();
                long nextObservationTime = nextObservationTimes.get(timeSeriesName);
                if(nextObservationTime < getSimulationTime()) {
                    updateCount++;
                    double timeIncrement = (double)(nextObservationTime - lastObservationTimes.get(timeSeriesName))/ Constants.MILLISECONDS_IN_SECOND;
                    double observationValue = simulator.getNextValue(lastObservationValues.get(timeSeriesName),timeIncrement);
                    timeSeriesClient.put(timeSeriesName,new DataPoint(new Date(nextObservationTime),observationValue));
                    lastObservationValues.put(timeSeriesName,observationValue);
                    lastObservationTimes.put(timeSeriesName,nextObservationTime);
                    nextObservationTimes.put(timeSeriesName,nextObservationTime(nextObservationTime));
                }
            }
        }
        isFinished = true;
        isRunning = false;
    }

    /**
     * Time may be 'speeded up' during our simulation via use of the acceleration factor paramter
     * This function returns the accelerated simulation time represented as a unix epoch, in milliseconds
     *
     * @return
     */
    private long getSimulationTime(){
        return startTime + (System.currentTimeMillis() - startTime) * accelerationFactor;
    }

}
