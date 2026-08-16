package com.kjweb.turtlepick.probe;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/ping")
    public String ping() {
        return turtlepickProbeService.ping();
    }

    @GetMapping("/slow-self-time")
    public String slowSelfTime() {
        return turtlepickProbeService.slowSelfTime();
    }

    @GetMapping("/slow-private-helper")
    public String slowPrivateHelper() {
        return turtlepickProbeService.slowPrivateHelper();
    }

    @GetMapping("/slow-repeated-repository")
    public String slowRepeatedRepository(@RequestParam(defaultValue = "60") int repeat) {
        return turtlepickProbeService.slowRepeatedRepository(repeat);
    }

    @GetMapping("/slow-repeated-repository-boxed")
    public String slowRepeatedRepositoryBoxed(@RequestParam(defaultValue = "60") int repeat) {
        return turtlepickProbeService.slowRepeatedRepositoryBoxed(repeat);
    }
}
