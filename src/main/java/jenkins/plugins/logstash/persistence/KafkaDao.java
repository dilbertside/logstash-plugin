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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;

/**
 * Kafka Data Access Object.
 *
 * @author dbs
 * @since 1.0.5
 */
public class KafkaDao extends AbstractLogstashIndexerDao {

  Producer<String, String> kafkaProducer = null;
  String topic;

  //primary constructor used by indexer factory
  public KafkaDao(String host, int port, String key, String username, String password) {
    this(null, host, port, key, username, password);
  }

  // Constructor for unit testing
  KafkaDao(Producer<String, String> _kafkaProducer, String host, int port, String key, String username, String password) {
    super(host, port, key, username, password);

    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("Kafka topic name is required");
    }
    
    // The KafkaProducer is a singleton as it can shared be shared among all threads for best performance
    if(null == _kafkaProducer){
      Map<String, Object> props = new HashMap<String, Object>();
      props.put("bootstrap.servers", String.format("%s:%s", host, port));
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      props.put("acks", "0");
      props.put("client.id", "jenkins-logstash");
      this.topic = key;
      this.kafkaProducer = new KafkaProducer<String, String>(props);
    } else {
      this.topic = key;
      this.kafkaProducer = _kafkaProducer;
    }
  }

  @Override
  public void push(String data) throws IOException {
    try {
      
      kafkaProducer.send(new ProducerRecord<String, String>(topic, data));
      
    } catch (KafkaException e) {
      throw new IOException(e);
    }
  }

  @Override
  public IndexerType getIndexerType() {
    return IndexerType.KAFKA;
  }

}
