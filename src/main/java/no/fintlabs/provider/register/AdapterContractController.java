package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import no.fintlabs.provider.security.AdapterContractContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class AdapterContractController {

    private final AdapterContractContext adapterContractContext;

    @GetMapping("/adapter")
    public ResponseEntity<Set<String>> getAdapterIds() {
        return ResponseEntity.ok(adapterContractContext.getAdapterIds());
    }

}
