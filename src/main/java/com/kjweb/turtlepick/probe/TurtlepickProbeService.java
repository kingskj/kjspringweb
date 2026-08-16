package com.kjweb.turtlepick.probe;

import com.kjweb.domain.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "tp.probe.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TurtlepickProbeService {

    private final MenuRepository menuRepository;

    public String recoverInventoryReservation() {
        try {
            reservePrimaryInventory();
            return "reserved";
        } catch (IllegalStateException ex) {
            return "backorder";
        }
    }

    private void reservePrimaryInventory() {
        throw new IllegalStateException("primary inventory unavailable");
    }

    public String ping() {
        return "ok";
    }

    @Transactional(readOnly = true)
    public String slowSelfTime() {
        measuredTinyChild();
        try {
            Thread.sleep(180L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("probe interrupted", ex);
        }
        return "self-time";
    }

    public int measuredTinyChild() {
        try {
            Thread.sleep(5L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("probe interrupted", ex);
        }
        return 1;
    }

    @Transactional(readOnly = true)
    public String slowPrivateHelper() {
        slowPrivateWork();
        return "private-helper";
    }

    private void slowPrivateWork() {
        try {
            Thread.sleep(180L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("probe interrupted", ex);
        }
    }

    @Transactional(readOnly = true)
    public String slowRepeatedRepository(int repeat) {
        int safeRepeat = Math.max(1, Math.min(repeat, 120));
        long total = 0L;
        for (int i = 0; i < safeRepeat; i++) {
            total += menuRepository.turtlepickProbeRangeCount(100_000L + i);
        }
        return "repeated-repository:" + total;
    }
}
