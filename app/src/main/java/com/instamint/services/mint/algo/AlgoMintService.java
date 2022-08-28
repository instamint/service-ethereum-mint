package com.instamint.services.mint.algo;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class AlgoMintService {
    public void service(ConsumerRecord<String,String> cr){
        System.out.println("Algo request received: " + cr.value());
    };
}
