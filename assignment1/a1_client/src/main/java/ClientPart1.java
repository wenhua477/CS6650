import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import picocli.CommandLine;

public class ClientPart1 {

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
    CountDownLatch phase1LatchNinetyPct = new CountDownLatch(numThreadForPhase1 * 90 / 100);
    CountDownLatch phase1LatchAll = new CountDownLatch(numThreadForPhase1);
    int numOfSkierIdsPerThread = numSkiers / numThreadForPhase1;
    int skierIdStart = 0;
    int skierIdEnd = 0;

    long startTimeInMillSec = System.currentTimeMillis();

    // Phase 1
    for (int i = 0; i < numThreadForPhase1; i++) {
      skierIdStart = skierIdEnd + 1;
      if (i == numThreadForPhase1 - 1) {
        skierIdEnd = numSkiers;
      } else {
        skierIdEnd = skierIdStart + numOfSkierIdsPerThread - 1;
      }
      Runnable th = new TaskForClientPart1(skierIdStart, skierIdEnd, numLifts, START_TIME_PHASE_1,
          END_TIME_PHASE_1,
          resortID, skiDayId, NUM_POST_PHASE_1, NUM_GET_PHASE_1, phase1LatchNinetyPct,
          phase1LatchAll, serverAddr);
      new Thread(th).start();
    }

    phase1LatchNinetyPct.await();

    // Phase 2
    int numThreadForPhase2 = maxThreads;
    CountDownLatch phase2LatchNinetyPct = new CountDownLatch(numThreadForPhase2 * 90 / 100);
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

      Runnable th = new TaskForClientPart1(skierIdStart, skierIdEnd, numLifts, START_TIME_PHASE_2,
          END_TIME_PHASE_2,
          resortID, skiDayId, NUM_POST_PHASE_2, NUM_GET_PHASE_2, phase2LatchNinetyPct,
          phase2LatchAll, serverAddr);
      new Thread(th).start();
    }

    phase2LatchNinetyPct.await();

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
      Runnable th = new TaskForClientPart1(skierIdStart, skierIdEnd, numLifts, START_TIME_PHASE_3,
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
        + "Throughput= %s.", numOfSuccess, numOfFailure, numOfFailure + numOfSuccess, wallTimeInSec, throughPut));
  }
}
