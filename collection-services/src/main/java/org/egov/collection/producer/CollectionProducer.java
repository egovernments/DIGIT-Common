package org.egov.collection.producer;


import lombok.extern.slf4j.Slf4j;
import org.egov.collection.config.CollectionServiceConstants;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tracer.kafka.CustomKafkaTemplate;
import org.egov.tracer.model.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CollectionProducer {

	public static final Logger logger = LoggerFactory.getLogger(CollectionProducer.class);

	@Autowired
    private CustomKafkaTemplate<String, Object> kafkaTemplate;
	
    @Autowired
    private MultiStateInstanceUtil centralInstanceUtil;

/*
    public void producer(String topicName, String key, Object value) {
        try {
            kafkaTemplate.send(topicName, key, value);
        } catch (Exception e) {
            logger.error("Pushing to Queue FAILED! ", e.getMessage());
            throw new CustomException("COLLECTIONS_KAFKA_PUSH_FAILED", CollectionServiceConstants
                    .KAFKA_PUSH_EXCEPTION_DESC);
        }
    	
    }
 */
    public void producer(String topicName, Object value) {
        try {
            kafkaTemplate.send(topicName, value);
        } catch (Exception e) {
            logger.error("Pushing to Queue FAILED! ", e.getMessage());
            throw new CustomException("COLLECTIONS_KAFKA_PUSH_FAILED", CollectionServiceConstants
                    .KAFKA_PUSH_EXCEPTION_DESC);
        }

    }
	
	
    public void push(String tenantId, String topic, Object value) {

        String updatedTopic = centralInstanceUtil.getStateSpecificTopicName(tenantId, topic);
        log.info("The Kafka topic for the tenantId : " + tenantId + " is : " + updatedTopic);
        kafkaTemplate.send(updatedTopic, value);
    }
	
}
