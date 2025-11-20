import com.bookvexe.mailservice.config.RateLimitRule;
import com.bookvexe.mailservice.config.RateLimiter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void testRateLimiterFast() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(List.of(new RateLimitRule(50, 1),    // 1 per 50 ms
            new RateLimitRule(100, 2)    // 2 per 100 ms
        ));

        // First send
        assertTrue(limiter.canSend(), "1st send should pass");

        // Immediately sending again violates 50ms rule
        assertFalse(limiter.canSend(), "2nd send too soon -> blocked");

        // Wait past 50ms window
        Thread.sleep(55);
        assertTrue(limiter.canSend(), "After 50ms -> allowed");

        // Now 2 events exist inside the last 100ms → further send must be blocked
        assertFalse(limiter.canSend(), "3rd send violates 100ms window -> blocked");

        // Wait until the 100ms window expires
        Thread.sleep(110);
        assertTrue(limiter.canSend(), "After 100ms -> allowed again");
    }

}
