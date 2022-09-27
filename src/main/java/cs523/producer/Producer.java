package cs523.producer;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import cs523.model.Tweet;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Producer {

	final Logger logger = LoggerFactory.getLogger(Producer.class);

	private Client client;
	private KafkaProducer<String, String> producer;
	private BlockingQueue<String> msgQueue = new LinkedBlockingQueue<>(30);
	private List<String> trackTerms = Lists.newArrayList("football", "soccer", "basketball", "handball", "volleyball");

	public static void main(String[] args) {
		BasicConfigurator.configure();
		new Producer().run();
	}

	// Twitter Client
	private Client createTwitterClient(BlockingQueue<String> msgQueue) {
		/** Setting up a connection */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hbEndpoint = new StatusesFilterEndpoint();
		// Term that I want to search on Twitter
		hbEndpoint.trackTerms(trackTerms);
		// Twitter API and tokens
		Authentication hosebirdAuth = new OAuth1("1R5FaVapm53Gnc0OgijvMmd28",
				"xlggPOkv9C61OcEW9JznROomkKPqPwZxBJAg25DsKwnUT7506y",
				"154526910-OBcHAEnnraYFLBj5YEZlw2KtoUULzROIBnkcorVG",
				"dLnUIaAE2t7jAlzTYNY3dkaSGBIMMtBi3kCbWcUNj6SEZ");

		/** Creating a client */
		ClientBuilder builder = new ClientBuilder().name("Hosebird-Client")
				.hosts(hosebirdHosts).authentication(hosebirdAuth)
				.endpoint(hbEndpoint)
				.processor(new StringDelimitedProcessor(msgQueue));

		Client hbClient = builder.build();

		return hbClient;
	}

	// Kafka Producer
	private KafkaProducer<String, String> createKafkaProducer() {
		// Create producer properties
		Properties properties = new Properties();

		properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
				"172.21.0.4:9092");

		properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class);

		properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class);
		return new KafkaProducer<String, String>(properties);
	}

	private void run() {
		logger.info("Setting up");

		// 1. Call the Twitter Client
		client = createTwitterClient(msgQueue);
		client.connect();

		// 2. Create Kafka Producer
		producer = createKafkaProducer();

		// Shutdown Hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("Application is not stopping!");
			client.stop();
			logger.info("Closing Producer");
			producer.close();
			logger.info("Finished closing");
		}));

		// 3. Send Tweets to Kafka
		while (!client.isDone()) {
			String msg = null;
			try {
				msg = msgQueue.poll(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				client.stop();
			}
			if (msg != null) {
				logger.info(msg);
				JSONObject js = new JSONObject(msg);
				logger.info(js.toString());
				Tweet t = getTweet(js);

				logger.info(t.toString());

				producer.send(new ProducerRecord<String, String>(
						KafkaConfig.TOPIC, "", new Gson().toJson(t)),
						new Callback() {
							@Override
							public void onCompletion(
									RecordMetadata recordMetadata, Exception e) {
								if (e != null) {
									logger.error(
											"Some error OR something bad happened",
											e);
								}
							}
						});
			}
		}
		logger.info("\n Application End");
	}

	public static Tweet getTweet(JSONObject o) {

		Tweet tweet = new Tweet();

		tweet.setId(o.getString("id_str"));
		System.out.println("id: " + o.getString("id_str"));
		tweet.setText(o.getString("text"));
		tweet.setRetweet(tweet.getText().startsWith("RT @"));
		tweet.setInReplyToStatusId(o.get("in_reply_to_status_id").toString());

		JSONArray hasTags = o.getJSONObject("entities")
				.getJSONArray("hashtags");

		hasTags.forEach(tag -> {
			tweet.getHashTags().add(tag.toString());
		});

		tweet.setUsername(o.getJSONObject("user").getString("screen_name"));
		tweet.setTimeStamp(o.getString("timestamp_ms"));
		tweet.setLang(o.getString("lang"));

		return tweet;
	}
}