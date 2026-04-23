package no.fintlabs.provider.register;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class AdapterContractController {

    private final ContractService contractService;

    @GetMapping("/adapter")
    public ResponseEntity<Set<String>> getAdapterIds() {
        return ResponseEntity.ok(contractService.getAdapterIds());
    }

}
