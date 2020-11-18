package utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Provides a connection pool for RabbitMQ channel objects.
 */
public class RabbitMQChannelFactory
    extends BasePooledObjectFactory<Channel> {

    private final Connection conn;
    private final String queueName;

    public RabbitMQChannelFactory(Connection conn, String queueName) {
        super();
        this.conn = conn;
        this.queueName = queueName;
    }

    @Override
    public Channel create() throws Exception {
        Channel chan =  conn.createChannel();
        chan.queueDeclare(queueName, false, false, false, null);
        return chan;
    }

    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        return new DefaultPooledObject<>(channel);
    }
}
