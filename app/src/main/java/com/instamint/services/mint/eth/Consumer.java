package com.instamint.services.mint.eth;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Consumer implements Runnable {

    EthMintService ems = new EthMintService();
    @Override
    public void run() {
        Duration timeout = Duration.ofMillis(100);
        Properties props = new Properties();
        props.put("bootstrap.servers","kb.instamint.com:9092");
        props.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
        props.put("group.id","instamint.eth.mint.service.group");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("instamint.service.eth.mint"));
        System.out.println("Instamint Ethereum Minting Service started");
        while (true) {
            ConsumerRecords<String,String> records = consumer.poll(timeout);
            for (ConsumerRecord<String,String> r : records) {
                try {
                    ems.service(r);
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
