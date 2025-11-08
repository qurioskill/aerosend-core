package io.aerosend.core.api;

import io.aerosend.core.deliverability.DeliverabilityQueries;
import io.aerosend.core.domain.Domain;
import io.aerosend.core.domain.DomainRepository;
import io.aerosend.core.provisioning.DomainProvisioningService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/domains")
public class DomainController {

    public record ProvisionRequest(@NotNull Long customerId, @NotNull Long serverBatchId, @NotBlank String name) {}

    private final DomainRepository domains;
    private final DomainProvisioningService provisioning;
    private final DeliverabilityQueries deliverability;

    public DomainController(DomainRepository domains, DomainProvisioningService provisioning,
                            DeliverabilityQueries deliverability) {
        this.domains = domains;
        this.provisioning = provisioning;
        this.deliverability = deliverability;
    }

    @GetMapping
    public List<Domain> list(@RequestParam Long customerId) {
        return domains.findByCustomerId(customerId);
    }

    @PostMapping
    public Domain provision(@RequestBody ProvisionRequest req) {
        return provisioning.provisionDomain(req.customerId(), req.serverBatchId(), req.name());
    }

    @GetMapping("/{id}/stats")
    public List<DeliverabilityQueries.DailyStats> stats(@PathVariable long id,
                                                        @RequestParam(defaultValue = "14") int days) {
        return deliverability.dailyStats(id, days);
    }
}
