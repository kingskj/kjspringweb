package com.kjweb.turtlepick.probe;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tp-scenario")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tp.probe.enabled", havingValue = "true")
public class TurtlepickProbeController {

    private final TurtlepickProbeService turtlepickProbeService;

    @GetMapping("/recover")
    public String recover() {
        return turtlepickProbeService.recoverInventoryReservation();
    }
}
