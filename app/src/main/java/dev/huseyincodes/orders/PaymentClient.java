package dev.huseyincodes.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** A fake payment provider: every order id ending in 3 is slow and retried, every id ending in 7 fails. */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    public void charge(long orderId) {
        if (orderId % 10 == 3) {
            log.warn("payment provider timed out for order {}, retrying (attempt 2 of 3)", orderId);
        }
        if (orderId % 10 == 7) {
            throw new IllegalStateException("payment provider returned 503 for order " + orderId);
        }
    }
}
