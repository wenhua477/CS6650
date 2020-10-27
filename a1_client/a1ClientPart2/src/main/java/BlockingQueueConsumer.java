import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BlockingQueueConsumer implements Runnable{
  private static final Logger logger = LogManager.getLogger(TaskForClientPart2.class);

  private BlockingQueue<List<String>> blockingQueue;
  private int maxThreads;
  private AtomicBoolean allPhaseFinished;
  private CountDownLatch blockingQueueConsumerLatch;

  public BlockingQueueConsumer(
      BlockingQueue<List<String>> blockingQueue, int maxThreads, AtomicBoolean allPhaseFinished, CountDownLatch blockingQueueConsumerLatch) {
    this.blockingQueue = blockingQueue;
    this.maxThreads = maxThreads;
    this.allPhaseFinished = allPhaseFinished;
    this.blockingQueueConsumerLatch = blockingQueueConsumerLatch;
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
  @Override
  public void run() {
    FileWriter csvWriter = null;
    try {
      csvWriter = new FileWriter(String.format("%s_threads.csv", maxThreads));
      csvWriter.append("StartTime,RequestType,Latency,ResponseCode\n");
      while (!blockingQueue.isEmpty() || !allPhaseFinished.get()) {
        List<String> resultList = blockingQueue.poll(1, TimeUnit.SECONDS);
        if (resultList != null) {
          for (String res : resultList) {
            csvWriter.append(res);
            csvWriter.append("\n");
          }
        }
      }

      csvWriter.flush();
      csvWriter.close();
    } catch (IOException | InterruptedException e) {
      logger.error(e);
    }
    blockingQueueConsumerLatch.countDown();
  }
}
