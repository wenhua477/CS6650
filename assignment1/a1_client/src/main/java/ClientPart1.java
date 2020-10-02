import io.swagger.client.ApiClient;

public class ClientPart1 implements Runnable {

  public static void main(String[] args) {
    // read them from args
    int maxThreads = 256;
    int numSkiers = 50000;
    int numLifts = 40;
    int skiDayId = 1;
    String resortID = "SilverMt";
    String serverAddr = new ApiClient().getBasePath();
    //  each ski day is of length 420 minutes
    int skiDayLength = 420;

//    Thread[] threads = new Thread[100];
//    long before = System.currentTimeMillis();
//    for (int i = 0; i < 100; i++) {
//      threads[i] = new Thread(new ClientPart1());
//      threads[i].start();
//    }
//
//    for (int i = 0; i < 100; i ++) {
//      try {
//        threads[i].join();
//      } catch (InterruptedException e) {
//      }
//    }
//    long after = System.currentTimeMillis();
//    System.out.println(after - before);
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

  }
}
