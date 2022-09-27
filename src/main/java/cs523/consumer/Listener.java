package cs523.consumer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cs523.model.Tweet;
import cs523.model.TweetHbaseTable;
import kafka.serializer.StringDecoder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.spark.streaming.kafka.KafkaUtils;

public class Listener {

	public static void main(String[] args) throws Exception {
		SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("Listener");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		JavaStreamingContext ssc = new JavaStreamingContext(jsc,
				Durations.seconds(5));
		// JavaReceiverInputDStream<SparkFlumeEvent> stream = FlumeUtils
		// .createStream(jsc, "127.0.0.1", 9999);

		Set<String> topics = new HashSet<>(Arrays.asList("tweets".split(",")));
		Map<String, String> kafkaParams = new HashMap<>();
		kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
				"localhost:9092");
		kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				"StringDeserializer");
		kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				"StringDeserializer");
		kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");

		JavaPairInputDStream<String, String> stream = KafkaUtils
				.createDirectStream(ssc, String.class, String.class,
						StringDecoder.class, StringDecoder.class, kafkaParams,
						topics);


		stream.foreachRDD(rdd -> {

			JavaRDD<Tweet> jrdd = rdd.map(f -> new Gson().fromJson(f._2,
					Tweet.class));

			jrdd.foreach(t -> {
				System.out.println("listener tweet" + t);
				//tweets.putTweet(t);
				TweetHbaseTable.populateData(t);
			});
		});

		ssc.start();
		ssc.awaitTermination();
	}

	public static Tweet getTweet(JsonObject o) {
		Tweet tweet = new Tweet();

		tweet.setId(o.get("id").getAsString());

		tweet.setText(o.get("text").getAsString());
		tweet.setRetweet(tweet.getText().startsWith("RT @"));
		tweet.setInReplyToStatusId(o.get("in_reply_to_status_id").toString());

		JsonArray hasTags = o.get("entities").getAsJsonObject().get("hashtags")
				.getAsJsonArray();
		hasTags.forEach(tag -> {
			tweet.getHashTags().add(
					hasTags.getAsJsonObject().get("text").getAsString());
		});

//		tweet.getHashTags().add(o.get("entities").getAsJsonObject().get("hashtags")
//				.getAsJsonObject().get("text").getAsString());

		tweet.setUsername(o.getAsJsonObject("user").get("screen_name")
				.getAsString());
		tweet.setTimeStamp(o.get("timestamp_ms").getAsString());
		tweet.setLang(o.get("lang").getAsString());

		return tweet;
	}
}
