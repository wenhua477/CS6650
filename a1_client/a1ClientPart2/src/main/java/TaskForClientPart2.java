import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskForClientPart2 implements Runnable {

  private static final Logger logger = LogManager.getLogger(TaskForClientPart2.class);

  private BlockingQueue<List<String>> blockingQueue;
  private int skierIdStart;
  private int skierIdEnd;
  private int liftIdRange;
  private int timeStart;
  private int timeEnd;
  private String resortId;
  private String skiDayNumber;
  private int numPost;
  private int numGet;
  private CountDownLatch tenPctLatch;
  private CountDownLatch totalLatch;
  private String address;

  public TaskForClientPart2(BlockingQueue<List<String>> blockingQueue, int skierIdStart,
      int skierIdEnd,
      int liftIdRange, int timeStart, int timeEnd,
      String resortId, String skiDayNumber, int numPost, int numGet,
      CountDownLatch tenPctLatch, CountDownLatch totalLatch, String address) {
    this.blockingQueue = blockingQueue;
    this.skierIdStart = skierIdStart;
    this.skierIdEnd = skierIdEnd;
    this.liftIdRange = liftIdRange;
    this.timeStart = timeStart;
    this.timeEnd = timeEnd;
    this.resortId = resortId;
    this.skiDayNumber = skiDayNumber;
    this.numPost = numPost;
    this.numGet = numGet;
    this.tenPctLatch = tenPctLatch;
    this.totalLatch = totalLatch;
    this.address = address;
  }

  private void sendRequests() {
    List<String> localRecord = new ArrayList<>();
    SkiersApi skiersApi = new SkiersApi();
    skiersApi.getApiClient().setBasePath(address);
    int successCnt = 0;
    int failureCnt = 0;
    boolean isSuccessful;
    for (int i = 0; i < numPost; i++) {
      isSuccessful = sendPost(skiersApi, localRecord);
      if (isSuccessful) {
        successCnt += 1;
      } else {
        failureCnt += 1;
      }
    }

    for (int i = 0; i < numGet; i++) {
      isSuccessful = sendGetSkierDayVertical(skiersApi, localRecord);
      if (isSuccessful) {
        successCnt += 1;
      } else {
        failureCnt += 1;
      }
    }

    // If numGet == 10, then it is phase3, we call both GET api 10 times
    if (numGet == 10) {
      for (int i = 0; i < numGet; i++) {
        isSuccessful = sendGetSkierResortTotals(skiersApi, localRecord);
        if (isSuccessful) {
          successCnt += 1;
        } else {
          failureCnt += 1;
        }
      }
    }

    try {
      blockingQueue.put(localRecord);
    } catch (InterruptedException e) {
      logger.error(e);
    }

    ClientPart2.sharedRequestCountAtomic.numSuccessAtomic.addAndGet(successCnt);
    ClientPart2.sharedRequestCountAtomic.numFailureAtomic.addAndGet(failureCnt);
  }

  private boolean sendPost(SkiersApi skiersApi, List<String> localRecord) {
    String skierId = getRandomSkierId(skierIdStart, skierIdEnd);
    String liftId = getRandomLiftId(liftIdRange);
    String time = getRandomTime(timeStart, timeEnd);
    LiftRide liftRide = new LiftRide();
    liftRide.setResortID(resortId);
    liftRide.setLiftID(liftId);
    liftRide.setSkierID(skierId);
    liftRide.setTime(time);
    liftRide.setDayID(skiDayNumber);

    ApiResponse<Void> apiResponse;
    long startTime = System.currentTimeMillis();
    try {
      apiResponse = skiersApi.writeNewLiftRideWithHttpInfo(liftRide);
    } catch (ApiException e) {
      int errorCode = e.getCode();
      logger.error(e);
      if (errorCode / 100 == 4 || errorCode / 100 == 5) {
        logger.info("Error code: %s,\n, responseBody=%s.", errorCode, e.getResponseBody());
      }
      long endTime = System.currentTimeMillis();
      long latency = endTime - startTime;
      localRecord.add(startTime + "," + "POST" + "," + latency + "," + e.getCode());
      return false;
    }

    if (apiResponse == null) {
      return false;
    }

    long endTime = System.currentTimeMillis();
    long latency = endTime - startTime;

    int code = apiResponse.getStatusCode();
    localRecord.add(startTime + "," + "POST" + "," + latency + "," + code);

    return code == 201 || code == 200;
  }

  private boolean sendGetSkierDayVertical(SkiersApi skiersApi, List<String> localRecord) {
    // Each GET randomly selects a skierID and calls /skiers/{resortID}/days/{dayID}/skiers/{skierID}
    String skierId = getRandomSkierId(skierIdStart, skierIdEnd);
    ApiResponse<io.swagger.client.model.SkierVertical> apiResponse = null;
    long startTime = System.currentTimeMillis();
    try {
      apiResponse = skiersApi.getSkierDayVerticalWithHttpInfo(resortId, skiDayNumber, skierId);
    } catch (ApiException e) {
      int errorCode = e.getCode();
      logger.error(e);
      if (errorCode / 100 == 4 || errorCode / 100 == 5) {
        logger.info("Error code: %s,\n, responseBody=%s.", errorCode, e.getResponseBody());
      }
      long endTime = System.currentTimeMillis();
      long latency = endTime - startTime;
      localRecord.add(startTime + "," + "GetSkierDayVertical" + "," + latency + "," + e.getCode());
      return false;
    }

    if (apiResponse == null) {
      return false;
    }
    long endTime = System.currentTimeMillis();
    long latency = endTime - startTime;

    int code = apiResponse.getStatusCode();

    localRecord.add(startTime + "," + "GetSkierDayVertical" + "," + latency + "," + code);

    return code == 200 || code == 201;
  }

  private boolean sendGetSkierResortTotals(SkiersApi skiersApi, List<String> localRecord) {
    String skierId = getRandomSkierId(skierIdStart, skierIdEnd);
    ApiResponse<io.swagger.client.model.SkierVertical> apiResponse = null;
    long startTime = System.currentTimeMillis();
    try {
      apiResponse = skiersApi.getSkierResortTotalsWithHttpInfo(skierId,
          Collections.singletonList(resortId));
    } catch (ApiException e) {
      int errorCode = e.getCode();
      logger.error(e);
      if (errorCode / 100 == 4 || errorCode / 100 == 5) {
        logger.info("Error code: %s,\n, responseBody=%s.", errorCode, e.getResponseBody());
      }
      long endTime = System.currentTimeMillis();
      long latency = endTime - startTime;
      localRecord.add(startTime + "," + "GetSkierResortTotals" + "," + latency + "," + e.getCode());
      return false;
    }

    if (apiResponse == null) {
      return false;
    }
    long endTime = System.currentTimeMillis();
    long latency = endTime - startTime;

    int code = apiResponse.getStatusCode();

    localRecord.add(startTime + "," + "GetSkierResortTotals" + "," + latency + "," + code);

    return code == 200 || code == 201;
  }

  /**
   * When an object implementing interface <code>Runnable</code> is used to create a thread,
   * starting the thread causes the object's
   * <code>run</code> method to be called in that separately executing
   * thread.
   * <p>
   * The general contract of the method <code>run</code> is that it may take any action whatsoever.
   *
   * @see Thread#run()
   */
  public void run() {
    sendRequests();

    if (tenPctLatch != null) {
      tenPctLatch.countDown();
    }
    totalLatch.countDown();
  }

  private String getRandomLiftId(int range) {
    return String.valueOf(getRandomNumber(0, range));
  }

  private String getRandomSkierId(int start, int end) {
    return String.valueOf(getRandomNumber(start, end));
  }

  private String getRandomTime(int start, int end) {
    return String.valueOf(getRandomNumber(start, end));
  }

  private int getRandomNumber(int min, int max) {
    return (int) ((Math.random() * (max - min)) + min);
  }
}
