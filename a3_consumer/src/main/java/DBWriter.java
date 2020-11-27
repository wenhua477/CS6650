import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import io.swagger.client.model.LiftRide;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBWriter implements Runnable {

  private final static String QUEUE_NAME = "RabbitMQ for assignment 3";
  private Connection connection;
  private LiftRideDao liftRideDao;

  public DBWriter(Connection connection) {
    this.connection = connection;
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
    this.liftRideDao = new LiftRideDao();

    try {
      final Channel channel = connection.createChannel();
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);
      // max one message per receiver
      channel.basicQos(1);
//      System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

        LiftRide liftRide = new Gson().fromJson(message, LiftRide.class);
        boolean successfulWrite = false;
        while (!successfulWrite) {
          try {
            liftRideDao.createLiftRide(liftRide);
            successfulWrite = true;
          } catch (Exception se) {
            se.printStackTrace();
          }
        }
      };
      // process messages
      channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
      });
    } catch (IOException ex) {
      Logger.getLogger(RMQConsumer.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
