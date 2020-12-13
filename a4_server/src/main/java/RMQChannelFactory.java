import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class RMQChannelFactory extends BasePooledObjectFactory<Channel> {

  private Connection connection;
  private static final String QUEUE_NAME = "RabbitMQ for assignment 3";


  public RMQChannelFactory() {
    ConnectionFactory factory = new ConnectionFactory();
      factory.setUsername(System.getProperty("RMQ_USERNAME"));
      factory.setPassword(System.getProperty("RMQ_PASSWORD"));
      factory.setVirtualHost("/"); // I think this is the default "virtual host"
      factory.setHost(System.getProperty("RMQ_HOST_ADDRESS")); // For example, something like ec2-x-y-z.compute.amazonaws.com
      factory.setPort(Integer.parseInt(System.getProperty("RMQ_PORT"))); // This is normally the default port that RabbitmQ grabs
    try {
        connection = factory.newConnection();

      } catch (TimeoutException | IOException e) {
        e.printStackTrace();
      }
  }

  @Override
  public Channel create() throws Exception {
    Channel channel = this.connection.createChannel();
    channel.basicQos(1);
    channel.queueDeclare(QUEUE_NAME, true, false, false, null);

    return channel;
  }

  @Override
  public PooledObject<Channel> wrap(Channel channel) {
    return new DefaultPooledObject<Channel>(channel);
  }
}
