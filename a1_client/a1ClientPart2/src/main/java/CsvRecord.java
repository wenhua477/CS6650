import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CsvRecord {

  private static final Logger logger = LogManager.getLogger(TaskForClientPart2.class);

  private long startTime;
  private String type;
  private int latency;
  private int responseCode;

  public CsvRecord(String csvRecord) {
    try {
      String[] values = csvRecord.split(",");
      this.startTime = Long.valueOf(values[0]);
      this.type = values[1];
      this.latency = Integer.valueOf(values[2]);
      this.responseCode = Integer.parseInt(values[3]);
    } catch (Exception e) {
      logger.error(e);
      logger.error(String.format("The erroneous csvRecord is %s.", csvRecord));
    }
  }

  public long getStartTime() {
    return startTime;
  }

  public String getType() {
    return type;
  }

  public int getLatency() {
    return latency;
  }

  public int getResponseCode() {
    return responseCode;
  }
}
