package no.fintlabs.provider.kafka.offset;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/offset")
public class OffsetController {

    private final OffsetState offsetState;

    @GetMapping
    public OffsetState getOffsetState() {
        return offsetState;
    }

}
