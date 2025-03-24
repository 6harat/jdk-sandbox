package io.pop.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final Map<String, OrderStatus> orderStatusMap = new HashMap<>();

    @Autowired
    public OrderService(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam String id, @RequestParam double amount) {
        OrderEvent event = new OrderEvent(id, OrderStatus.CREATED, amount);
        orderStatusMap.put(id, OrderStatus.CREATED);
        kafkaTemplate.send("order-topic", event);
        logger.info("order: [{}] placed successfully", id);
        return "order placed successfully";
    }

    @GetMapping("/{id}/status")
    public String getOrderStatus(@PathVariable String id) {
        OrderStatus orderStatus = orderStatusMap.getOrDefault(id, OrderStatus.NOT_FOUND);
        logger.info("order [{}] status: [{}]", id, orderStatus);
        return orderStatus.toString();
    }

    @KafkaListener(topics = "payment-response-topic", groupId = "order-group")
    public void listenPaymentResponse(OrderEvent event) {
        if (event.status() == OrderStatus.PAYMENT_SUCCESS) {
            orderStatusMap.put(event.id(), OrderStatus.PAYMENT_SUCCESS);
            kafkaTemplate.send("inventory-topic", event);
            logger.info("order: [{}] status updated to paid", event.id());
        } else {
            orderStatusMap.put(event.id(), OrderStatus.CANCELED);
            logger.info("order: [{}] canceled due to payment failure", event.id());
        }
    }

    @KafkaListener(topics = "inventory-response-topic", groupId = "order-group")
    public void listenInventoryResponse(OrderEvent event) {
        if (event.status() == OrderStatus.STOCK_RESERVED) {
            orderStatusMap.put(event.id(), OrderStatus.COMPLETED);
            logger.info("order: [{}] completed successfully!", event.id());
        } else {
            orderStatusMap.put(event.id(), OrderStatus.CANCELED);
            kafkaTemplate.send("inventory-rollback-topic", event);
            logger.info("order: [{}] canceled due to stock unavailability", event.id());
        }
    }
}
