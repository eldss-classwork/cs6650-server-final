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

    public RabbitMQChannelFactory(Connection conn) {
        super();
        this.conn = conn;
    }

    @Override
    public Channel create() throws Exception {
        return conn.createChannel();
    }

    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        return new DefaultPooledObject<>(channel);
    }
}
