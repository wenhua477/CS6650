public class SharedRequestCount {
  private int numSuccess;
  private int numFailure;

  public SharedRequestCount(int numSuccess, int numFailure) {
    this.numSuccess = numSuccess;
    this.numFailure = numFailure;
  }

  public int getNumSuccess() {
    return numSuccess;
  }

  public int getNumFailure() {
    return numFailure;
  }

  public synchronized void updateNumSuccess(int num) {
    this.numSuccess += num;
  }

  public synchronized void updateNumFailure(int num) {
    this.numFailure += num;
  }
}
