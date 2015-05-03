package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ActiveMqDaoTest {
  ActiveMqDao dao;
  @Mock ActiveMQConnectionFactory mockConnectionFactory;
  @Mock Connection mockConnection;
  @Mock Session mockSession;
  @Mock Queue mockDestination;
  @Mock MessageProducer mockProducer;
  @Mock TextMessage mockMessage;

  ActiveMqDao createDao(String host, int port, String key, String username, String password) {
    ActiveMqDao factory = new ActiveMqDao(mockConnectionFactory, host, port, key, username, password);

    if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
      verify(mockConnectionFactory, atLeastOnce()).setUserName(username);
      verify(mockConnectionFactory, atLeastOnce()).setPassword(password);
    }

    return factory;
  }
  
  
  @Before
  public void before() throws Exception {
    int port = (int) (Math.random() * 1000);

    // Note that we can't run these tests in parallel
    dao = createDao("localhost", port, "logstash", "username", "password");

    when(mockConnectionFactory.createConnection()).thenReturn(mockConnection);

    when(mockConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockSession);
    
    when(mockSession.createQueue("logstash")).thenReturn(mockDestination);
    
    when(mockSession.createProducer(mockDestination)).thenReturn(mockProducer);
    
    when(mockSession.createTextMessage("{ 'foo': 'bar' }")).thenReturn(mockMessage);
    
  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(mockConnectionFactory);
    verifyNoMoreInteractions(mockConnection);
    verifyNoMoreInteractions(mockSession);
    verifyNoMoreInteractions(mockDestination);
    verifyNoMoreInteractions(mockProducer);
    verifyNoMoreInteractions(mockMessage);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullHost() throws Exception {
    try {
      createDao(null, 5672, "logstash", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyHost() throws Exception {
    try {
      createDao(" ", 5672, "logstash", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "host name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailNullKey() throws Exception {
    try {
      createDao("localhost", 5672, null, "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "JMS queue name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyKey() throws Exception {
    try {
      createDao("localhost", 5672, " ", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "JMS queue name is required", e.getMessage());
      throw e;
    }
  }

  @Test
  public void constructorSuccess() throws Exception {
    // Unit under test
    dao = createDao("localhost", 5672, "logstash", "username", "password");

    // Verify results
    assertEquals("Wrong host name", "localhost", dao.host);
    assertEquals("Wrong port", 5672, dao.port);
    assertEquals("Wrong key", "logstash", dao.key);
    assertEquals("Wrong password", "password", dao.password);
  }

  @Test(expected = IOException.class)
  public void pushFailUnauthorized() throws Exception {
    // Initialize mocks
    when(mockConnectionFactory.createConnection()).thenThrow(new JMSException("Not authorized"));

    // Unit under test
    try {
      dao.push("{}");
    } catch (IOException e) {
      // Verify results
      verify(mockConnectionFactory).createConnection();
      assertEquals("wrong error message", "IOException: javax.jms.JMSException: Not authorized", ExceptionUtils.getMessage(e));
      throw e;
    }

  }

  @Test(expected = IOException.class)
  public void pushFailCantConnect() throws Exception {
    // Initialize mocks
    when(mockConnectionFactory.createConnection()).thenThrow(new JMSException("Connection refused"));

    // Unit under test
    try {
      dao.push("");
    } catch (IOException e) {
      // Verify results
      verify(mockConnectionFactory).createConnection();
      assertEquals("wrong error message", "IOException: javax.jms.JMSException: Connection refused", ExceptionUtils.getMessage(e));
      throw e;
    }

  }

  @Test(expected = IOException.class)
  public void pushFailCantWrite() throws Exception {
    // Initialize mocks
    doThrow(new JMSException("Queue length limit exceeded")).when(mockSession).createTextMessage("{}");

    // Unit under test
    try {
      dao.push("{}");
    } catch (IOException e) {
      // Verify results
      verify(mockConnectionFactory).createConnection();
      verify(mockConnection).start();
      verify(mockConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
      verify(mockSession).createQueue("logstash");
      verify(mockSession).createProducer(mockDestination);
      verify(mockProducer).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      verify(mockSession).createTextMessage("{}");
      assertEquals("wrong error message", "IOException: javax.jms.JMSException: Queue length limit exceeded", ExceptionUtils.getMessage(e));
      throw e;
    }

  }

  @Test
  public void pushSuccess() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Unit under test
    dao.push(json);
    // Verify results
    verify(mockConnectionFactory).createConnection();
    verify(mockConnection).start();
    verify(mockConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
    verify(mockSession).createQueue("logstash");
    verify(mockSession).createProducer(mockDestination);
    verify(mockProducer).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    verify(mockSession).createTextMessage(json);
    verify(mockProducer).send(mockMessage);
    verify(mockSession).close();
    verify(mockConnection).close();

  }

  @Test
  public void pushSuccessNoAuth() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("localhost", 5672, "logstash", null, null);

    // Unit under test
    dao.push(json);
    // Verify results
    verify(mockConnectionFactory).createConnection();
    verify(mockConnection).start();
    verify(mockConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
    verify(mockSession).createQueue("logstash");
    verify(mockSession).createProducer(mockDestination);
    verify(mockProducer).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    verify(mockSession).createTextMessage(json);
    verify(mockProducer).send(mockMessage);
    verify(mockSession).close();
    verify(mockConnection).close();

  }
}
