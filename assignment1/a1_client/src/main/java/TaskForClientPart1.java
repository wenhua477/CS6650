import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.util.concurrent.CountDownLatch;

public class TaskForClientPart1 implements Runnable{
  private int skierIdStart;
  private int skierIdEnd;
  private int liftIdRange;
  private int timeStart;
  private int timeEnd;
  private String resortId;
  private String skiDayNumber;
  private int numPost;
  private int numGet;
  private CountDownLatch ninetyPctLatch;
  private CountDownLatch totalLatch;
  private String address;

  public TaskForClientPart1(int skierIdStart, int skierIdEnd, int liftIdRange, int timeStart, int timeEnd,
      String resortId, String skiDayNumber, int numPost, int numGet,
      CountDownLatch ninetyPctLatch, CountDownLatch totalLatch, String address) {
    this.skierIdStart = skierIdStart;
    this.skierIdEnd = skierIdEnd;
    this.liftIdRange = liftIdRange;
    this.timeStart = timeStart;
    this.timeEnd = timeEnd;
    this.resortId = resortId;
    this.skiDayNumber = skiDayNumber;
    this.numPost = numPost;
    this.numGet = numGet;
    this.ninetyPctLatch = ninetyPctLatch;
    this.totalLatch = totalLatch;
    this.address = address;
  }

  private void sendRequests() {
    SkiersApi skiersApi = new SkiersApi();
    skiersApi.getApiClient().setBasePath(address);
    int successCnt = 0;
    int failureCnt = 0;
    boolean isSuccessful;
    for (int i = 0; i < numPost; i++) {
      isSuccessful = sendPost(skiersApi);
      if (isSuccessful) {
        successCnt += 1;
      } else {
        failureCnt += 1;
      }
    }

    for (int i = 0; i < numGet; i++) {
      isSuccessful = sendGet(skiersApi);
      if (isSuccessful) {
        successCnt += 1;
      } else {
        failureCnt += 1;
      }
    }

    ClientPart1.sharedRequestCountAtomic.numSuccessAtomic.addAndGet(successCnt);
    ClientPart1.sharedRequestCountAtomic.numFailureAtomic.addAndGet(failureCnt);

  }

  private boolean sendPost(SkiersApi skiersApi) {
//    1. a skierID from the range of ids passed to the thread
//    2. a lift number (liftID)
//    3. a time from the range of minutes passed to each thread (start and end time -
//        same for each thread)
    String skierId = getRandomSkierId(skierIdStart, skierIdEnd);
    String liftId = getRandomLiftId(liftIdRange);
    String time = getRandomTime(timeStart, timeEnd);
    LiftRide liftRide = new LiftRide();
    liftRide.setResortID(resortId);
    liftRide.setLiftID(liftId);
    liftRide.setSkierID(skierId);
    liftRide.setTime(time);
    liftRide.setDayID(skiDayNumber);

    ApiResponse apiResponse = null;
    try {
      apiResponse = skiersApi.writeNewLiftRideWithHttpInfo(liftRide);
    } catch (ApiException e) {
      // TODO: log the error using log4j.
      e.printStackTrace();
      return false;
    }

    if (apiResponse == null) {
      return false;
    }

    int code = apiResponse.getStatusCode();
    if (code == 201 || code == 200) {
      return true;
    }
    if (code / 100 == 4 || code / 100 == 5) {
      // TODO: log the error using log4j.
      return false;
    }
    System.out.println(code);
    return false;
  }

  private boolean sendGet(SkiersApi skiersApi) {
//    Each GET randomly selects a skierID and calls /skiers/{resortID}/days/{dayID}/skiers/{skierID}
    String skierId = getRandomSkierId(skierIdStart, skierIdEnd);
    ApiResponse apiResponse = null;
    try {
      apiResponse = skiersApi.getSkierDayVerticalWithHttpInfo(resortId, skiDayNumber, skierId);
    } catch (ApiException e) {
      // TODO: log the error using log4j.
      e.printStackTrace();
      return false;
    }

    if (apiResponse == null) {
      return false;
    }

    int code = apiResponse.getStatusCode();
    if (code == 200 || code == 201) {
      return true;
    }
    if (code / 100 == 4 || code / 100 == 5) {
      // TODO: log the error using log4j.
      return false;
    }

    return false;
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

    if (ninetyPctLatch != null) {
      ninetyPctLatch.countDown();
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
