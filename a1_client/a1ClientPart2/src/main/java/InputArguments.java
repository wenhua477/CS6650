public class InputArguments {

  private static final int DEFAULT_NUM_SKIERS = 50000;
  private static final int DEFAULT_NUM_LIFTS = 40;
  private static final String DEFAULT_SKI_DAY_NUMBER = "1";
  private static final String DEFAULT_RESORT_NAME = "SilverMt";

  private static final int MAX_THREADS = 512;
  private static final int MIN_NUM_LIFTS = 5;
  private static final int MAX_NUM_LIFTS = 60;


  private int maxThreads;
  private int numSkiers;
  private int numLifts;
  private String skiDayNumber;
  private String resortId;
  private String serverAddress;


  public InputArguments(String[] args) throws Exception {
    this.numSkiers = DEFAULT_NUM_SKIERS;
    this.numLifts = DEFAULT_NUM_LIFTS;
    this.skiDayNumber = DEFAULT_SKI_DAY_NUMBER;
    this.resortId = DEFAULT_RESORT_NAME;

    boolean validMaxThreads = false;
    boolean validAddress = false;
    int i = 0;

    while (i < args.length) {
      switch (args[i]) {
        case "-maxThreads":
          if (i + 1 >= args.length) {
            throw new Exception("Invalid input: missing value for maxThreads!");
          }
          int inputMaxThreads = Integer.parseInt(args[i + 1]);
          if (inputMaxThreads < 0 || inputMaxThreads > MAX_THREADS) {
            throw new Exception(
                String.format("Value of maxThreads out of range: 0 to %s.", MAX_THREADS));
          }
          this.maxThreads = inputMaxThreads;
          validMaxThreads = true;
          break;
        case "-numSkiers":
          if (i + 1 >= args.length) {
            throw new Exception("Invalid input: missing value for numSkiers!");
          }
          this.numSkiers = Integer.parseInt(args[i + 1]);
          break;
        case "-numLifts":
          if (i + 1 >= args.length) {
            throw new Exception("Invalid input: missing value for numLifts!");
          }
          int inputNumLifts = Integer.parseInt(args[i + 1]);
          if (inputNumLifts < MIN_NUM_LIFTS || inputNumLifts > MAX_NUM_LIFTS) {
            throw new Exception(String
                .format("Value of numLifts out of range: %s to %s.", MIN_NUM_LIFTS, MAX_NUM_LIFTS));
          }
          this.numLifts = inputNumLifts;
          break;
        case "-skiDay":
          if (i + 1 >= args.length) {
            throw new Exception("Invalid input: missing value for skiDay!");
          }
          if (!onlyDigits(args[i + 1])) {
            throw new Exception("Invalid input: value of skiDay should be digits only!");
          }
          this.skiDayNumber = args[i + 1];
          break;
        case "-resortID":
          if (i + 1 >= args.length) {
            throw new Exception("Invalid input: missing value for resortID!");
          }
          this.resortId = args[i + 1];
          break;
        case "-address":
          if (i + 1 >= args.length) {
            throw new Exception("Invalid input: missing value for address!");
          }
          this.serverAddress = args[i + 1];
          validAddress = true;
          break;
        default:
          break;
      }
      i += 1;
    }

    if (!validAddress || !validMaxThreads) {
      throw new Exception("Missing valid input for address or maxThreads!");
    }
  }

  // Check if a string contains only digits
  private static boolean onlyDigits(String str) {
    int len = str.length();
    // Traverse the string from start to end
    for (int i = 0; i < len; i++) {
      if (!(str.charAt(i) >= '0' && str.charAt(i) <= '9')) {
        return false;
      }
    }
    return true;
  }

  public int getMaxThreads() {
    return maxThreads;
  }

  public int getNumSkiers() {
    return numSkiers;
  }

  public int getNumLifts() {
    return numLifts;
  }

  public String getSkiDayNumber() {
    return skiDayNumber;
  }

  public String getResortId() {
    return resortId;
  }

  public String getServerAddress() {
    return serverAddress;
  }

  @Override
  public String toString() {
    return "InputArguments{" +
        "maxThreads=" + maxThreads +
        ", numSkiers=" + numSkiers +
        ", numLifts=" + numLifts +
        ", skiDayNumber='" + skiDayNumber + '\'' +
        ", resortId='" + resortId + '\'' +
        ", serverAddress='" + serverAddress + '\'' +
        '}';
  }
}
