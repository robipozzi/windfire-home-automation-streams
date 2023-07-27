package com.rpozzi.windfire.kafkastreams.service;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TemperaturesStreamsService {
	private static final Logger logger = LoggerFactory.getLogger(TemperaturesStreamsService.class);
	private static final String THREAD_NAME = "home-automation-streams-shutdown-hook";
	@Value(value = "${spring.kafka.bootstrap-servers}")
	private String kafkaBootstrapServers;
	@Value(value = "${kafkastreams.application.id}")
	private String kafkaStreamsAppId;
	@Value(value = "${kafka.topic.temperatures}")
	private String temperaturesKafkaTopic;
	@Value(value = "${kafka.topic.pipeoutput}")
	private String pipeOutputKafkaTopic;
	
	public void process() {
		logger.info("====> running process() method ...");
		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, kafkaStreamsAppId);
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		
		// Initialize StreamsBuilder
		final StreamsBuilder builder = new StreamsBuilder();
		
		// Read Stream from input Kafka Topic ${kafka.topic.temperatures} (see applicatiion.properties for mapping)
		logger.info("Streaming from '" + temperaturesKafkaTopic + "' Kafka topic ...");
		KStream<String, String> source = builder.stream(temperaturesKafkaTopic);
		
		// =============== PIPE - START ===============
		// Pipe source stream's record as-is to output Kafka Topic ${kafka.topic.pipeoutput} (see applicatiion.properties for mapping)
		logger.info("Pipe to '" + pipeOutputKafkaTopic + "' Kafka topic ...");
		source.to(pipeOutputKafkaTopic);
		// =============== PIPE - END ===============
		
		final Topology topology = builder.build();
		logger.debug("Printing Topology ...");
		logger.debug(topology.describe().toString());
		final KafkaStreams streams = new KafkaStreams(topology, props);
		final CountDownLatch latch = new CountDownLatch(1);

		// attach shutdown handler to catch control-c
		Runtime.getRuntime().addShutdownHook(new Thread(TemperaturesStreamsService.THREAD_NAME) {
			@Override
			public void run() {
				streams.close();
				latch.countDown();
			}
		});

		try {
			streams.start();
			latch.await();
		} catch (Throwable e) {
			System.exit(1);
		}
		System.exit(0);
	}
}