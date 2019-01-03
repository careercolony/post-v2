package com.mj.users.mongo

import com.mj.users.config.Application._

trait KafkaAccess {

  def sendPostToKafka(post: String): Unit = {
    import java.util.Properties

    import org.apache.kafka.clients.producer._

    val props = new Properties()
    props.put("bootstrap.servers", brokers)

    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)
    val record: ProducerRecord[String, String] = new ProducerRecord[String, String](topic, post)
    producer.send(record)
    producer.close()
  }

}
