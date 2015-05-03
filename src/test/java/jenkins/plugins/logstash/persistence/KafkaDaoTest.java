package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class KafkaDaoTest {
  KafkaDao dao;
  @Mock KafkaProducer<String, String> kafkaProducer;


  KafkaDao createDao(String host, int port, String key, String username, String password) {
    KafkaDao factory = new KafkaDao(kafkaProducer, host, port, key, username, password);

    return factory;
  }
  
  
  @Before
  public void before() throws Exception {
    int port = (int) (Math.random() * 1000);

    // Note that we can't run these tests in parallel
    dao = createDao("localhost", port, "logstash", "username", "password");

  }

  @After
  public void after() throws Exception {
    verifyNoMoreInteractions(kafkaProducer);
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

  //@Test
  public void pushFailCantConnect() throws Exception {
    // Initialize mocks
    when(kafkaProducer.send(new ProducerRecord<String, String>("logstash", ""))).thenThrow(new SocketException("Connection refused"));

    // Unit under test
    try {
      dao.push("");
    } catch (IOException e) {
      // Verify results
      verify(kafkaProducer).send(new ProducerRecord<String, String>("logstash", ""));
      //verify(mockLogger).println(Matchers.startsWith("java.net.SocketException: Connection refused"));
      assertEquals("wrong error message", "java.net.SocketException: Connection refused", ExceptionUtils.getMessage(e));
      throw e;
    }

  }

  //@Test
  public void pushFailCantWrite() throws Exception {
    // Initialize mocks
    doThrow(new SocketException("Queue length limit exceeded")).when(kafkaProducer).send(new ProducerRecord<String, String>("logstash", "{}"));

    // Unit under test
    try {
      dao.push("{}");
    } catch (IOException e) {
      // Verify results
      verify(kafkaProducer).send(new ProducerRecord<String, String>("logstash", ""));
      assertEquals("wrong error message", "java.net.SocketException: Queue length limit exceeded", ExceptionUtils.getMessage(e));
      throw e;
    }

  }

  @Test
  public void pushSuccess() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Unit under test
    dao.push(json);
    // Verify results
    verify(kafkaProducer).send(new ProducerRecord<String, String>("logstash", json));

  }

  @Test
  public void pushSuccessNoAuth() throws Exception {
    String json = "{ 'foo': 'bar' }";
    dao = createDao("localhost", 5672, "logstash", null, null);

    // Unit under test
    dao.push(json);
    // Verify results
    verify(kafkaProducer).send(new ProducerRecord<String, String>("logstash", json));

  }
}
