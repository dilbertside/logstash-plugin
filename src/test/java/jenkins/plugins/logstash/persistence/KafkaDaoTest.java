package jenkins.plugins.logstash.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.internals.FutureRecordMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class KafkaDaoTest {
  KafkaDao dao;
  @Mock KafkaProducer<String, String> mockKafkaProducer;
  FutureRecordMetadata future;


  KafkaDao createDao(String host, int port, String key, String username, String password) {
    KafkaDao factory = new KafkaDao(mockKafkaProducer, host, port, key, username, password);

    return factory;
  }
  
  
  @Before
  public void before() throws Exception {
    int port = (int) (Math.random() * 1000);

    dao = createDao("localhost", port, "logstash", "username", "password");
    
    when(mockKafkaProducer.send(new ProducerRecord<String, String>("logstash", "{ 'foo': 'bar' }"))).thenReturn(future);

  }

  @After
  public void after() throws Exception {
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
      assertEquals("Wrong error message was thrown", "Kafka topic name is required", e.getMessage());
      throw e;
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailEmptyKey() throws Exception {
    try {
      createDao("localhost", 5672, " ", "username", "password");
    } catch (IllegalArgumentException e) {
      assertEquals("Wrong error message was thrown", "Kafka topic name is required", e.getMessage());
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

  @Test
  public void pushSuccess() throws Exception {
    String json = "{ 'foo': 'bar' }";

    // Unit under test
    dao.push(json);
    // Verify results
    //commented because verify use equals() and ProducerRecord does not include a equals() method 
    //verify(mockKafkaProducer).send(new ProducerRecord<String, String>("logstash", json));
  }

  private boolean isError(Future<?> future) {
    try {
        future.get();
        return false;
    } catch (Exception e) {
        return true;
    }
  }
}
