import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientPart2 {

  private static final int NUM_GET_PHASE_1 = 5;
  private static final int NUM_POST_PHASE_1 = 100;
  private static final int NUM_GET_PHASE_2 = 5;
  private static final int NUM_POST_PHASE_2 = 100;
  private static final int NUM_GET_PHASE_3 = 10;
  private static final int NUM_POST_PHASE_3 = 100;

  private static final int START_TIME_PHASE_1 = 1;
  private static final int END_TIME_PHASE_1 = 90;
  private static final int START_TIME_PHASE_2 = 91;
  private static final int END_TIME_PHASE_2 = 360;
  private static final int START_TIME_PHASE_3 = 361;
  private static final int END_TIME_PHASE_3 = 420;

  static class SharedRequestCountAtomic {

    public AtomicInteger numSuccessAtomic = new AtomicInteger(0);
    public AtomicInteger numFailureAtomic = new AtomicInteger(0);
  }

  static final SharedRequestCountAtomic sharedRequestCountAtomic = new SharedRequestCountAtomic();


  public static void main(String[] args) throws Exception {
    InputArguments inputArguments;
    try {
      inputArguments = new InputArguments(args);
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception("Invalid inputs. Program exits.");
    }

    // Read them from inputArguments
    int maxThreads = inputArguments.getMaxThreads();
    int numSkiers = inputArguments.getNumSkiers();
    int numLifts = inputArguments.getNumLifts();
    String skiDayId = inputArguments.getSkiDayNumber();
    String resortID = inputArguments.getResortId();
    String serverAddr = inputArguments.getServerAddress();

    int numThreadForPhase1 = maxThreads / 4;
    CountDownLatch phase1LatchTenPct = new CountDownLatch(numThreadForPhase1 * 10 / 100);
    CountDownLatch phase1LatchAll = new CountDownLatch(numThreadForPhase1);
    int numOfSkierIdsPerThread = numSkiers / numThreadForPhase1;
    int skierIdStart;
    int skierIdEnd = 0;

    BlockingQueue<List<String>> blockingQueue = new LinkedBlockingDeque<>();
    AtomicBoolean allPhaseFinished = new AtomicBoolean(false);
    Runnable blockingQueueConsumer = new BlockingQueueConsumer(blockingQueue, maxThreads,
        allPhaseFinished);
    new Thread(blockingQueueConsumer).start();

    long startTimeInMillSec = System.currentTimeMillis();

    // Phase 1
    for (int i = 0; i < numThreadForPhase1; i++) {
      skierIdStart = skierIdEnd + 1;
      if (i == numThreadForPhase1 - 1) {
        skierIdEnd = numSkiers;
      } else {
        skierIdEnd = skierIdStart + numOfSkierIdsPerThread - 1;
      }
      Runnable th = new TaskForClientPart2(blockingQueue, skierIdStart, skierIdEnd, numLifts,
          START_TIME_PHASE_1,
          END_TIME_PHASE_1,
          resortID, skiDayId, NUM_POST_PHASE_1, NUM_GET_PHASE_1, phase1LatchTenPct,
          phase1LatchAll, serverAddr);
      new Thread(th).start();
    }

    phase1LatchTenPct.await();

    // Phase 2
    int numThreadForPhase2 = maxThreads;
    CountDownLatch phase2LatchTenPct = new CountDownLatch(numThreadForPhase2 * 10 / 100);
    CountDownLatch phase2LatchAll = new CountDownLatch(numThreadForPhase2);
    numOfSkierIdsPerThread = numSkiers / numThreadForPhase2;
    skierIdEnd = 0;

    for (int i = 0; i < numThreadForPhase2; i++) {
      skierIdStart = skierIdEnd + 1;
      if (i == numThreadForPhase2 - 1) {
        skierIdEnd = numSkiers;
      } else {
        skierIdEnd = skierIdStart + numOfSkierIdsPerThread - 1;
      }

      Runnable th = new TaskForClientPart2(blockingQueue, skierIdStart, skierIdEnd, numLifts,
          START_TIME_PHASE_2,
          END_TIME_PHASE_2,
          resortID, skiDayId, NUM_POST_PHASE_2, NUM_GET_PHASE_2, phase2LatchTenPct,
          phase2LatchAll, serverAddr);
      new Thread(th).start();
    }

    phase2LatchTenPct.await();

    // Phase 3
    int numThreadForPhase3 = maxThreads / 4;
    CountDownLatch phase3LatchAll = new CountDownLatch(numThreadForPhase3);
    numOfSkierIdsPerThread = numSkiers / numThreadForPhase3;
    skierIdEnd = 0;

    for (int i = 0; i < numThreadForPhase3; i++) {
      skierIdStart = skierIdEnd + 1;
      if (i == numThreadForPhase3 - 1) {
        skierIdEnd = numSkiers;
      } else {
        skierIdEnd = skierIdStart + numOfSkierIdsPerThread - 1;
      }
      Runnable th = new TaskForClientPart2(blockingQueue, skierIdStart, skierIdEnd, numLifts,
          START_TIME_PHASE_3,
          END_TIME_PHASE_3,
          resortID, skiDayId, NUM_POST_PHASE_3, NUM_GET_PHASE_3, null,
          phase3LatchAll, serverAddr);
      new Thread(th).start();
    }

    phase1LatchAll.await();
    phase2LatchAll.await();
    phase3LatchAll.await();

    allPhaseFinished.set(true);

    long endTimeInMillSec = System.currentTimeMillis();
    int numOfSuccess = sharedRequestCountAtomic.numSuccessAtomic.get();
    int numOfFailure = sharedRequestCountAtomic.numFailureAtomic.get();
    double wallTimeInSec = ((endTimeInMillSec - startTimeInMillSec) / 1000.0) * 1.0;
    double throughPut =
        numOfSuccess / (wallTimeInSec * 1.0);
    System.out.println(String.format("Number of successful requests= %s \n"
            + "number of failed requests= %s \n"
            + "Total requests= %s \n"
            + "Total run time in seconds (wall time)= %s \n"
            + "Throughput= %s.", numOfSuccess, numOfFailure, numOfFailure + numOfSuccess,
        wallTimeInSec, throughPut));

    // Read csv files from desk and process data
    BufferedReader csvReader = new BufferedReader(
        new FileReader(String.format("%s_threads.csv", maxThreads)));
    String row = csvReader.readLine();

    long getRequestLatencySum = 0;
    long postRequestLatencySum = 0;
    long totalRequestLatencySum = 0;

    int getRequestCount = 0;
    int postRequestCount = 0;
    int totalRequestCount = 0;

    int maxGetRequestLatency = 0;
    int maxPostRequestLatency = 0;
    int maxTotalRequestLatency = 0;

    Map<Integer, Integer> getRequestLatencyCountMap = new HashMap<>();
    Map<Integer, Integer> postRequestLatencyCountMap = new HashMap<>();
    Map<Integer, Integer> totalRequestLatencyCountMap = new HashMap<>();

    while ((row = csvReader.readLine()) != null) {
      CsvRecord csvRecord = new CsvRecord(row);
      int latency = csvRecord.getLatency();
      String requestType = csvRecord.getType();
      if (requestType.equals("GET")) {
        getRequestCount += 1;
        getRequestLatencySum += latency;
        maxGetRequestLatency = Math.max(maxGetRequestLatency, latency);
        if (getRequestLatencyCountMap.containsKey(latency)) {
          getRequestLatencyCountMap.put(latency, getRequestLatencyCountMap.get(latency) + 1);
        } else {
          getRequestLatencyCountMap.put(latency, 1);
        }
      } else if (requestType.equals("POST")) {
        postRequestCount += 1;
        postRequestLatencySum += latency;
        maxPostRequestLatency = Math.max(maxPostRequestLatency, latency);
        if (postRequestLatencyCountMap.containsKey(latency)) {
          postRequestLatencyCountMap.put(latency, postRequestLatencyCountMap.get(latency) + 1);
        } else {
          postRequestLatencyCountMap.put(latency, 1);
        }
      }

      totalRequestCount += 1;
      totalRequestLatencySum += latency;
      maxTotalRequestLatency = Math.max(maxTotalRequestLatency, latency);
      if (totalRequestLatencyCountMap.containsKey(latency)) {
        totalRequestLatencyCountMap.put(latency, totalRequestLatencyCountMap.get(latency) + 1);
      } else {
        totalRequestLatencyCountMap.put(latency, 1);
      }
    }
    csvReader.close();

    // Calculate mean, median and max value of each request latencies
    Statistics statForPost = calculateStatistics(postRequestLatencySum, postRequestCount, maxPostRequestLatency, postRequestLatencyCountMap);
    Statistics statForGet = calculateStatistics(getRequestLatencySum, getRequestCount, maxGetRequestLatency, getRequestLatencyCountMap);
    Statistics statForTotal = calculateStatistics(totalRequestLatencySum, totalRequestCount, maxTotalRequestLatency, totalRequestLatencyCountMap);

    System.out.println("\n");
    System.out.println(String.format(
        "Total wall time=%s,\n"
            + "Throughput=%s,\n"
            + "Mean response time for GET=%s,\n"
            + "Median response time for GET=%s,\n"
            + "P99 response time for GET=%s,\n"
            + "Max response time for GET=%s.\n"
            + "Mean response time for POST=%s,\n"
            + "Median response time for POST=%s,\n"
            + "P99 response time for POST=%s,\n"
            + "Max response time for POST=%s.\n"
            + "Mean response time for Total=%s,\n"
            + "Median response time for Total=%s,\n"
            + "P99 response time for Total=%s,\n"
            + "Max response time for Total=%s.\n",
        wallTimeInSec, throughPut,
        statForGet.getMean(), statForGet.getMedian(), statForGet.getP99(), statForGet.getMax(),
        statForPost.getMean(), statForPost.getMedian(), statForPost.getP99(),
        statForPost.getMax(),
        statForTotal.getMean(), statForTotal.getMedian(), statForTotal.getP99(),
        statForTotal.getMax()));
  }


  private static Statistics calculateStatistics(long sum, int count, int maxLatency,
      Map<Integer, Integer> countMap) {
    Statistics statistics = new Statistics();
    statistics.setMax(maxLatency);
    statistics.setMean((double) sum / count);
    List<Integer> keyList = new ArrayList<>(countMap.keySet());
    Collections.sort(keyList);

    int medianPos = count % 2 == 1 ? count / 2 + 1 : count / 2;
    boolean medianWithAverage = count % 2 == 0;
    int p99Pos = (int) (count * 0.99);

    boolean medianAlreadySet = false;

    int numCount = 0;
    for (int i = 0; i < keyList.size(); i++) {
      numCount += countMap.get(keyList.get(i));
      if (numCount >= p99Pos) {
        statistics.setP99(keyList.get(i));
        break;
      }
      if (!medianAlreadySet && numCount >= medianPos) {
        if (!medianWithAverage) {
          statistics.setMedian(keyList.get(i));
          medianAlreadySet = true;
        } else {
          if (numCount == medianPos) {
            statistics.setMedian((double) (keyList.get(i) + keyList.get(i + 1)) / 2);
            medianAlreadySet = true;
          } else {
            statistics.setMedian(keyList.get(i));
            medianAlreadySet = true;
          }
        }
      }
    }

    return statistics;
  }

  private static Statistics calculateStatistics(List<CsvRecord> csvRecords) {
    int[] latencies = new int[csvRecords.size()];
    for (int i = 0; i < csvRecords.size(); i++) {
      latencies[i] = csvRecords.get(i).getLatency();
    }
    // Calculate mean
    double mean = Arrays.stream(latencies).average().orElse(Double.NaN);
    // Calculate median
    double median;
    Arrays.sort(latencies);
    if (latencies.length % 2 == 0) {
      median =
          ((double) latencies[latencies.length / 2] + (double) latencies[latencies.length / 2 - 1])
              / 2;
    } else {
      median = (double) latencies[latencies.length / 2];
    }

    // Get max latency
    double maxLatency = latencies[latencies.length - 1];

    // Calculate p99
    double p99Latency = latencies[(int) (latencies.length * 0.99)];

    Statistics statistics = new Statistics();
    statistics.setMean(mean);
    statistics.setMedian(median);
    statistics.setMax(maxLatency);
    statistics.setP99(p99Latency);
    return statistics;
  }
}
