package cs523.producer;

public class KafkaConfig {

    public static final String BOOTSTRAPSERVERS  = "127.21.0.4:9092";
    public static final String TOPIC = "tweets";
    public static final String BOOTSTRAP_SERVERS_CONFIG = "StringDeserializer";
    public static final String  VALUE_DESERIALIZER_CLASS_CONFIG = "StringDeserializer";

}
