import java.util.Map;

public class APIDataObject {

  private Long LatencySum;
  private Integer requestCount;
  private Integer maxLatency;
  private Map<Integer, Integer> latencyCountmap;

  public APIDataObject(Long latencySum, Integer requestCount, Integer maxLatency,
      Map<Integer, Integer> latencyCountmap) {
    LatencySum = latencySum;
    this.requestCount = requestCount;
    this.maxLatency = maxLatency;
    this.latencyCountmap = latencyCountmap;
  }

  public Long getLatencySum() {
    return LatencySum;
  }

  public void setLatencySum(Long latencySum) {
    LatencySum = latencySum;
  }

  public Integer getRequestCount() {
    return requestCount;
  }

  public void setRequestCount(Integer requestCount) {
    this.requestCount = requestCount;
  }

  public Integer getMaxLatency() {
    return maxLatency;
  }

  public void setMaxLatency(Integer maxLatency) {
    this.maxLatency = maxLatency;
  }

  public Map<Integer, Integer> getLatencyCountmap() {
    return latencyCountmap;
  }

  public void setLatencyCountmap(Map<Integer, Integer> latencyCountmap) {
    this.latencyCountmap = latencyCountmap;
  }
}
