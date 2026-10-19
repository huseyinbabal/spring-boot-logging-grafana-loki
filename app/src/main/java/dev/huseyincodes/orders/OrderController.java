package dev.huseyincodes.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final Map<Long, String> cache = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong(40);
    private final PaymentClient payments;

    public OrderController(PaymentClient payments) {
        this.payments = payments;
    }

    @PostMapping
    public Map<String, Object> create(@RequestParam String item) {
        log.trace("create called with item={}", item);
        long id = ids.incrementAndGet();
        cache.put(id, item);
        log.info("order {} created for item {}", id, item);
        return Map.of("id", id, "item", item);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> get(@PathVariable long id) {
        String item = cache.get(id);
        log.debug("cache {} for order {}", item != null ? "hit" : "miss", id);
        return item != null ? ResponseEntity.ok(item) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<String> pay(@PathVariable long id) {
        try {
            payments.charge(id);
            log.info("order {} paid", id);
            return ResponseEntity.ok("paid");
        } catch (IllegalStateException e) {
            log.error("payment failed for order {}", id, e);
            return ResponseEntity.status(502).body("payment failed");
        }
    }
}
