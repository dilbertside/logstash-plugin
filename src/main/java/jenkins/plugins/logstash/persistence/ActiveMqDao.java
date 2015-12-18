/*
 * The MIT License
 *
 * Copyright 2014 Rusty Gerard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.logstash.persistence;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.StringUtils;

/**
 * ActiveMq Data Access Object.
 *
 * @author dbs
 * @since 1.0.4
 */
public class ActiveMqDao extends AbstractLogstashIndexerDao {

  private static final Logger logger = Logger.getLogger( ActiveMqDao.class.getName() );
  
  ActiveMQConnectionFactory connectionFactory = null;

  //primary constructor used by indexer factory
  public ActiveMqDao(String host, int port, String key, String username, String password) {
    this(null, host, port, key, username, password);
  }

  // Constructor for unit testing
  ActiveMqDao(ActiveMQConnectionFactory mqConnectionFactory, String host, int port, String key, String username, String password) {
    super(host, port, key, username, password);

    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("JMS queue name is required");
    }

    // The ConnectionFactory must be a singleton
    // We assume this is used as a singleton as well
    // Calling this method means the configuration has changed and the pool must be re-initialized
    this.connectionFactory = mqConnectionFactory == null ? new ActiveMQConnectionFactory(String.format("tcp://%s:%d", host, port)): mqConnectionFactory;
    
    if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
      connectionFactory.setUserName(username);
      connectionFactory.setPassword(password);
    }

  }

  @Override
  public void push(String data) throws IOException {
    Connection connection = null;
    Session session = null;
    try {
      // Create a Connection
      connection = connectionFactory.createQueueConnection();
      connection.start();
      // Create a Session
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      
      // Create the destination Queue
      Destination destination = session.createQueue(key);
      
      // Create the MessageProducer from the Session to the Queue
      MessageProducer producer = session.createProducer(destination);
      producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      
      // Create the message
      TextMessage message = session.createTextMessage(data);
      String txId = UUID.randomUUID().toString();
      message.setStringProperty("txId", txId);
      message.setJMSType("application/json");
      // Tell the producer to send the message
      producer.send(message);
      
      logger.log( Level.FINER, String.format("JMS message sent with txId [%s]", txId));

    } catch (JMSException e) {
      logger.log( Level.SEVERE, null != e.getMessage() ? e.getMessage() : e.getClass().getName());
      throw new IOException(e);
    }finally {
      // Clean up
      try {
        if(null != session)
          session.close();
      } catch (JMSException e) {
        logger.log( Level.WARNING, null != e.getMessage() ? e.getMessage() : e.getClass().getName());
      }
      try {
        if(null != connection)
          connection.close();
      } catch (JMSException e) {
        logger.log( Level.WARNING, null != e.getMessage() ? e.getMessage() : e.getClass().getName());
      }
    }
  }

  @Override
  public IndexerType getIndexerType() {
    return IndexerType.ACTIVE_MQ;
  }

}
