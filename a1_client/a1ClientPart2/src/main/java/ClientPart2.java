import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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

    List<String> resultList = Collections.synchronizedList(new ArrayList<String>());

    long startTimeInMillSec = System.currentTimeMillis();

    // Phase 1
    for (int i = 0; i < numThreadForPhase1; i++) {
      skierIdStart = skierIdEnd + 1;
      if (i == numThreadForPhase1 - 1) {
        skierIdEnd = numSkiers;
      } else {
        skierIdEnd = skierIdStart + numOfSkierIdsPerThread - 1;
      }
      Runnable th = new TaskForClientPart2(resultList, skierIdStart, skierIdEnd, numLifts,
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

      Runnable th = new TaskForClientPart2(resultList, skierIdStart, skierIdEnd, numLifts,
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
      Runnable th = new TaskForClientPart2(resultList, skierIdStart, skierIdEnd, numLifts,
          START_TIME_PHASE_3,
          END_TIME_PHASE_3,
          resortID, skiDayId, NUM_POST_PHASE_3, NUM_GET_PHASE_3, null,
          phase3LatchAll, serverAddr);
      new Thread(th).start();
    }

    phase1LatchAll.await();
    phase2LatchAll.await();
    phase3LatchAll.await();

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

    FileWriter csvWriter = new FileWriter(String.format("%s_threads.csv", maxThreads));
    csvWriter.append("StartTime,RequestType,Latency,ResponseCode\n");

    List<CsvRecord> csvRecordsForGet = new LinkedList<CsvRecord>();
    List<CsvRecord> csvRecordsForPost = new LinkedList<CsvRecord>();
    List<CsvRecord> csvRecordsTotal = new LinkedList<>();
    for (String res : resultList) {
      csvWriter.append(res);
      csvWriter.append("\n");
      if (res.contains("GET")) {
        csvRecordsForGet.add(new CsvRecord(res));
      } else {
        csvRecordsForPost.add(new CsvRecord(res));
      }
      csvRecordsTotal.add(new CsvRecord(res));
    }
    csvWriter.flush();
    csvWriter.close();

    // Calculate mean, median and max value of each request latencies
    Statistics statForPost = calculateStatistics(csvRecordsForPost);
    Statistics statForGet = calculateStatistics(csvRecordsForGet);
    Statistics statForTotal = calculateStatistics(csvRecordsTotal);

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
