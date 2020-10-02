import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.util.concurrent.CountDownLatch;

public class Task implements Runnable{
  protected int skierIdStart;
  protected int skierIdEnd;
  protected int liftIdRange;
  protected int timeStart;
  protected int timeEnd;
  protected String resortId;
  protected String skiDayNumber;
  protected int numPost;
  protected int numGet;
  protected CountDownLatch ninetyPctLatch;
  protected CountDownLatch totalLatch;
  private String address;

  public Task(int skierIdStart, int skierIdEnd, int liftIdRange, int timeStart, int timeEnd,
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
    if (code == 201) {
      return true;
    }
    if (code / 100 == 4 || code / 100 == 5) {
      // TODO: log the error using log4j.
      return false;
    }

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
    if (code == 200) {
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

  public  String getRandomLiftId(int range) {
    // TODO:
    return "";
  }

  public  String getRandomSkierId(int start, int end) {
    // TODO
    return "";
  }

  public  String getRandomTime(int start, int end) {
    //TODO
    return "";
  }
}
