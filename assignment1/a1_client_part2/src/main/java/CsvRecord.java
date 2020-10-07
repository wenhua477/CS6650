public class CsvRecord {
  private long startTime;
  private String type;
  private int latency;
  private int responseCode;

  public CsvRecord(String csvRecord) {
    String[] values = csvRecord.split(",");
    this.startTime = Long.valueOf(values[0]);
    this.type = values[1];
    this.latency = Integer.valueOf(values[2]);
    this.responseCode = Integer.parseInt(values[3]);
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
