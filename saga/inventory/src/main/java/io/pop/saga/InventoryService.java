package io.pop.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Autowired
    public InventoryService(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "payment-response-topic", groupId = "inventory-group")
    public void processInventory(OrderEvent event) {
        if (event.status() == OrderStatus.PAYMENT_SUCCESS) {
            boolean isStockAvailable = checkStock(event.id());

            OrderStatus status = isStockAvailable ? OrderStatus.STOCK_RESERVED : OrderStatus.STOCK_FAILED;
            OrderEvent responseEvent = new OrderEvent(event.id(), status, event.amount());

            kafkaTemplate.send("inventory-response-topic", responseEvent);
            logger.info("order: [{}] inventory processed with: [{}]", event.id(), status);
        }
    }

    private boolean checkStock(String orderId) {
        return Math.random() > 0.3;
    }
}
