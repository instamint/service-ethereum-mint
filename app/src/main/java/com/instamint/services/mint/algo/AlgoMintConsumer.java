package com.instamint.services.mint.algo;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class AlgoMintConsumer implements Runnable {

    AlgoMintService ams = new AlgoMintService();
    @Override
    public void run() {
        Duration timeout = Duration.ofMillis(100);
        Properties props = new Properties();
        props.put("bootstrap.servers","instamint.network:9092");
        props.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
        props.put("group.id","instamint.eth.algo.service.group");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("instamint.service.algo.mint"));
        System.out.println("Instamint Algo Minting Service started");
        while (true) {
            ConsumerRecords<String,String> records = consumer.poll(timeout);
            for (ConsumerRecord<String,String> r : records) {
                    ams.service(r);
            }
        }
    }
}
