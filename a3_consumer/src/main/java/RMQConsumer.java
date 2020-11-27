import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RMQConsumer {

  public static void main(String[] argv) throws Exception {
    int num_threads = 5;
    String hostName;
    num_threads = Integer.parseInt(argv[0]);
    hostName = argv[1];
    int portNumber = Integer.parseInt(argv[2]);
    String userName = argv[3];
    String password = argv[4];

    // java -jar a3_consumer.jar 100 "hostname" 5672 wenhua 12345678
    System.out.println(String.format("NumThreads = %s", num_threads));

    final ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername(userName);
    factory.setPassword(password);
    factory.setVirtualHost("/"); // I think this is the default "virtual host"
    factory.setHost(hostName); // For example, something like ec2-x-y-z.compute.amazonaws.com
    factory.setPort(portNumber); // This is normally the default port that RabbitmQ grabs

    final Connection connection = factory.newConnection();

    // start threads and block to receive messages
    for (int i = 0; i < num_threads; i++) {
      new Thread(new DBWriter(connection)).start();
    }
  }
}
