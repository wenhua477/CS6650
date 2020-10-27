import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
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
  private static final int NUM_POST_PHASE_1 = 1000;
  private static final int NUM_GET_PHASE_2 = 5;
  private static final int NUM_POST_PHASE_2 = 1000;
  private static final int NUM_GET_PHASE_3 = 10;
  private static final int NUM_POST_PHASE_3 = 1000;

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

    CountDownLatch blockingQueueConsumerLatch = new CountDownLatch(1);
    BlockingQueue<List<String>> blockingQueue = new LinkedBlockingDeque<>();
    AtomicBoolean allPhaseFinished = new AtomicBoolean(false);
    Runnable blockingQueueConsumer = new BlockingQueueConsumer(blockingQueue, maxThreads,
        allPhaseFinished, blockingQueueConsumerLatch);
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

    // wait for all data in blockingQueue got processed
    allPhaseFinished.set(true);
    blockingQueueConsumerLatch.await();

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

    APIDataObject getSkierDayVerticalApiObj = new APIDataObject(0L, 0,
        0, new HashMap<>());

    APIDataObject getSkierResortTotalsApiObj = new APIDataObject(0L, 0,
        0, new HashMap<>());

    APIDataObject postApiObj = new APIDataObject(0L, 0,
        0, new HashMap<>());

    APIDataObject totalApiObj = new APIDataObject(0L, 0,
        0, new HashMap<>());

    while ((row = csvReader.readLine()) != null) {
      CsvRecord csvRecord = new CsvRecord(row);
      int latency = csvRecord.getLatency();
      String requestType = csvRecord.getType();
      if (requestType.equals("GetSkierDayVertical")) {
        processDataHelper(latency, getSkierDayVerticalApiObj);
      } else if (requestType.equals("GetSkierResortTotals")) {
        processDataHelper(latency, getSkierResortTotalsApiObj);
      } else if (requestType.equals("POST")) {
        processDataHelper(latency, postApiObj);
      }

      processDataHelper(latency, totalApiObj);
    }
    csvReader.close();

    // Calculate mean, median and max value of each request latencies
    Statistics statForPostApi = calculateStatistics(postApiObj);
    Statistics statForGetSkierResortTotalsApi = calculateStatistics(getSkierResortTotalsApiObj);
    Statistics statForGetSkierDayVerticalApi = calculateStatistics(getSkierDayVerticalApiObj);
    Statistics statForTotal = calculateStatistics(totalApiObj);

    System.out.println("\n");
    System.out.println(String.format(
        "Total wall time=%s,\n"
            + "Throughput=%s,\n"
            + "Mean response time for GetSkierResortTotalsApi=%s,\n"
            + "Median response time for GetSkierResortTotalsApi=%s,\n"
            + "P99 response time for GetSkierResortTotalsApi=%s,\n"
            + "Max response time for GetSkierResortTotalsApi=%s.\n"
            + "\n"

            + "Mean response time for GetSkierDayVerticalApi=%s,\n"
            + "Median response time for GetSkierDayVerticalApi=%s,\n"
            + "P99 response time for GetSkierDayVerticalApi=%s,\n"
            + "Max response time for GetSkierDayVerticalApi=%s.\n"
            + "\n"

            + "Mean response time for POST=%s,\n"
            + "Median response time for POST=%s,\n"
            + "P99 response time for POST=%s,\n"
            + "Max response time for POST=%s.\n"
            + "\n"

            + "Mean response time for Total=%s,\n"
            + "Median response time for Total=%s,\n"
            + "P99 response time for Total=%s,\n"
            + "Max response time for Total=%s.\n",
        wallTimeInSec, throughPut,
        statForGetSkierResortTotalsApi.getMean(), statForGetSkierResortTotalsApi.getMedian(),
        statForGetSkierResortTotalsApi.getP99(), statForGetSkierResortTotalsApi.getMax(),
        statForGetSkierDayVerticalApi.getMean(), statForGetSkierDayVerticalApi.getMedian(),
        statForGetSkierDayVerticalApi.getP99(), statForGetSkierDayVerticalApi.getMax(),
        statForPostApi.getMean(), statForPostApi.getMedian(), statForPostApi.getP99(),
        statForPostApi.getMax(),
        statForTotal.getMean(), statForTotal.getMedian(), statForTotal.getP99(),
        statForTotal.getMax()));
  }


  private static void processDataHelper(int latency, APIDataObject apiDataObject) {
    apiDataObject.setRequestCount(apiDataObject.getRequestCount() + 1);
    apiDataObject.setLatencySum(apiDataObject.getLatencySum() + latency);
    apiDataObject.setMaxLatency(Math.max(apiDataObject.getMaxLatency(), latency));
    if (apiDataObject.getLatencyCountmap().containsKey(latency)) {
      apiDataObject.getLatencyCountmap()
          .put(latency, apiDataObject.getLatencyCountmap().get(latency) + 1);
    } else {
      apiDataObject.getLatencyCountmap().put(latency, 1);
    }
  }


  private static Statistics calculateStatistics(APIDataObject apiDataObject) {
    long sum = apiDataObject.getLatencySum();
    int count = apiDataObject.getRequestCount();
    int maxLatency = apiDataObject.getMaxLatency();
    Map<Integer, Integer> countMap = apiDataObject.getLatencyCountmap();

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
}
