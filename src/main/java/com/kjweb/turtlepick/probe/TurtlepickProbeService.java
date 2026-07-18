package com.kjweb.turtlepick.probe;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "tp.probe.enabled", havingValue = "true")
public class TurtlepickProbeService {

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
}
