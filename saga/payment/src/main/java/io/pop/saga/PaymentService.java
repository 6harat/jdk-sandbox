package io.pop.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Autowired
    public PaymentService(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order-topic", groupId = "payment-group")
    public void processPayment(OrderEvent event) {
        boolean isPaymentSuccessful = event.amount() < 1000;

        OrderStatus status = isPaymentSuccessful ? OrderStatus.PAYMENT_SUCCESS : OrderStatus.PAYMENT_FAILED;
        OrderEvent responseEvent = new OrderEvent(event.id(), status, event.amount());

        kafkaTemplate.send("payment-response-topic", responseEvent);
        logger.info("order: [{}] payment processed with: [{}]", event.id(), status);
    }

    @KafkaListener(topics = "inventory-rollback-topic", groupId = "payment-group")
    public void rollbackPayment(OrderEvent event) {
        logger.info("rolling back payment for order: [{}]", event.id());
    }
}
