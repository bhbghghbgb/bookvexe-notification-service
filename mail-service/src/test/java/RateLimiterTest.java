import com.bookvexe.mailservice.config.RateLimitRule;
import com.bookvexe.mailservice.config.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RateLimiterTest {

    private final long SHORT_WINDOW = 500; // 500ms
    private final int SHORT_LIMIT = 2;     // 2 requests

    private final long LONG_WINDOW = 2000; // 2000ms (2s)
    private final int LONG_LIMIT = 5;      // 5 requests

    private RateLimitRule shortRule;
    private RateLimitRule longRule;

    @BeforeEach
    void setUp() {
        // Initialize rules before each test
        shortRule = new RateLimitRule(SHORT_WINDOW, SHORT_LIMIT);
        longRule = new RateLimitRule(LONG_WINDOW, LONG_LIMIT);
    }

    // --- RateLimitRule Tests ---

    @Test
    void testAcquireWithinLimit() {
        // Should acquire the first item
        assertTrue(shortRule.tryAcquire(), "First acquisition should succeed.");
        // Should acquire the second item
        assertTrue(shortRule.tryAcquire(), "Second acquisition should succeed.");
    }

    @Test
    void testBlockWhenLimitExceeded() {
        // Acquire 1st and 2nd
        assertTrue(shortRule.tryAcquire());
        assertTrue(shortRule.tryAcquire());

        // 3rd attempt should fail immediately
        assertFalse(shortRule.tryAcquire(), "Third acquisition should fail immediately.");
    }

    @Test
    void testSlidingWindowResetsAfterTime() throws InterruptedException {
        // Acquire 1st and 2nd (Hit limit)
        assertTrue(shortRule.tryAcquire());
        assertTrue(shortRule.tryAcquire());

        // Fail 3rd
        assertFalse(shortRule.tryAcquire());

        // Wait for the window to pass (500ms + buffer)
        Thread.sleep(SHORT_WINDOW + 50);

        // 4th attempt should succeed as the first two timestamps have slid out of the window
        assertTrue(shortRule.tryAcquire(), "Acquisition should succeed after window time passes.");

        // 5th attempt should succeed
        assertTrue(shortRule.tryAcquire(), "Acquisition should succeed after window time passes.");

        // 6th attempt should fail again
        assertFalse(shortRule.tryAcquire(), "Acquisition should fail after new items hit the limit.");
    }

    // --- RateLimiter (Multiple Rule) Tests ---

    @Test
    void testRateLimiterPassesWhenAllRulesPass() {
        // Create a RateLimiter with the two rules
        RateLimiter limiter = new RateLimiter(List.of(shortRule, longRule));

        // Attempt 1: Both rules have capacity
        assertTrue(limiter.canSend(), "Both rules should allow send.");

        // Attempt 2: Both rules still have capacity
        assertTrue(limiter.canSend(), "Both rules should allow send.");

        // Short rule is now at limit (2/2)
    }

    @Test
    void testRateLimiterFailsWhenShortRuleFails() {
        // Short rule: 2 per 500ms | Long rule: 5 per 2000ms
        RateLimiter limiter = new RateLimiter(List.of(shortRule, longRule));

        // Acquire 1st and 2nd (Both succeed)
        assertTrue(limiter.canSend());
        assertTrue(limiter.canSend());

        // 3rd attempt: Short rule is maxed out, so canSend() should return false
        assertFalse(limiter.canSend(), "canSend() should fail because the SHORT_RULE limit is hit.");

        // Crucially, the long rule's counter should NOT have incremented on failure
        // The implementation ensures that if tryAcquire() fails for any rule, 
        // the remaining rules in the list are not checked/acquired.
        // The longRule should still be at 2/5 capacity.
    }

    @Test
    void testRateLimiterFailsWhenLongRuleFails() {
        // Short rule: 2 per 500ms | Long rule: 5 per 2000ms
        RateLimiter limiter = new RateLimiter(List.of(shortRule, longRule));

        // Acquire up to the long rule limit (5 times)
        // Since the short window is 500ms, we must space these out or 
        // the short rule will block immediately. 
        // We will only acquire 2 in the short window, then wait.

        // Acquires 1, 2
        assertTrue(limiter.canSend());
        assertTrue(limiter.canSend());

        // Wait for short window to pass (ensures short rule resets)
        try {
            Thread.sleep(SHORT_WINDOW + 50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Acquires 3, 4
        assertTrue(limiter.canSend());
        assertTrue(limiter.canSend());

        // Wait again for short window to pass
        try {
            Thread.sleep(SHORT_WINDOW + 50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Acquires 5 (Long rule is now maxed out 5/5)
        assertTrue(limiter.canSend());

        // 6th attempt should fail because the LONG_RULE limit is hit.
        assertFalse(limiter.canSend(), "canSend() should fail because the LONG_RULE limit is hit.");
    }
}