package paysim.output;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import paysim.base.Transaction;
import paysim.parameters.Parameters;

import java.util.ArrayList;
import java.util.Properties;

public class KafkaOutput {

    private KafkaProducer<String, String> producer;
    private String topic;
    private Properties kafkaConfig;


    public KafkaOutput(String topic){
        this.kafkaConfig = generateKafkaConfig();
        this.producer = new KafkaProducer<String, String>(kafkaConfig);
        this.topic = topic;
    }

    private Properties generateKafkaConfig(){
        Properties props = new Properties();
        props.put("bootstrap.servers", Parameters.kafkaBrokers);
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        return props;
    }

    public void sendTransactionsToKafka(ArrayList<Transaction> transactions){
        for (Transaction transaction : transactions) {
            this.producer.send(new ProducerRecord<String, String>(this.topic, transaction.getDateTime().getMonth().toString(), transaction.toString()));
        }
    }

    public void closeProducer(){
        producer.close();
    }

}
