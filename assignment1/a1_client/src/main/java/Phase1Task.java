import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

public class Phase1Task implements Runnable {
  private int skierIdStart;
  private int skierIdEnd;
  private int liftIdRange;
  private int timeStart;
  private int timeEnd;
  private String resortId;
  private String skiDayNumber;

  public Phase1Task(int skierIdStart, int skierIdEnd, int liftIdRange, int timeStart, int timeEnd,
      String resortId, String skiDayNumber) {
    this.skierIdStart = skierIdStart;
    this.skierIdEnd = skierIdEnd;
    this.liftIdRange = liftIdRange;
    this.timeStart = timeStart;
    this.timeEnd = timeEnd;
    this.resortId = resortId;
    this.skiDayNumber = skiDayNumber;
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
    SkiersApi skiersApi = new SkiersApi();
    int successCnt = 0;
    int failureCnt = 0;
    boolean isSuccessful = false;
    for (int i = 0; i < 100; i++) {
      isSuccessful = sendPost(skiersApi);
      if (isSuccessful) {
        successCnt += 1;
      } else {
        failureCnt += 1;
      }
    }

    for (int i = 0; i < 5; i++) {
      isSuccessful = sendGet(skiersApi);
      if (isSuccessful) {
        successCnt += 1;
      } else {
        failureCnt += 1;
      }
    }

  }

  private boolean sendPost(SkiersApi skiersApi){
//    1. a skierID from the range of ids passed to the thread
//    2. a lift number (liftID)
//    3. a time from the range of minutes passed to each thread (start and end time -
//        same for each thread)
    String skierId = Utils.getRandomSkierId(skierIdStart, skierIdEnd);
    String liftId = Utils.getRandomLiftId(liftIdRange);
    String time = Utils.getRandomTime(timeStart, timeEnd);
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
      e.printStackTrace();
    }

    if (apiResponse == null) {
      return false;
    }
    int code = apiResponse.getStatusCode();

    if (code == 201) {
      return true;
    }
    if (code / 100 == 4 || code/100 == 5) {
      // TODO: log the error using log4j.
      return false;
    }

    return false;
  }

  private boolean sendGet(SkiersApi skiersApi){
//    Each GET randomly selects a skierID and calls /skiers/{resortID}/days/{dayID}/skiers/{skierID}
    return false;
  }
}
