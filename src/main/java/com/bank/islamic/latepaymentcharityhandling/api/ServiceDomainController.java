package com.bank.islamic.latepaymentcharityhandling.api;

import com.bank.islamic.latepaymentcharityhandling.model.ControlRecord;
import com.bank.islamic.latepaymentcharityhandling.service.ControlRecordStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * BIAN semantic API for the "Late Payment Charity Handling" service domain.
 *
 * Endpoints follow the BIAN action-term style:
 *   GET  /v1/service-domain                          → who am I (SD metadata)
 *   POST /v1/late-payment-charity-entry-procedure/initiate                    → Initiate a control record
 *   GET  /v1/late-payment-charity-entry-procedure                             → Retrieve (list)
 *   GET  /v1/late-payment-charity-entry-procedure/{crId}/retrieve             → Retrieve (single)
 *   PUT  /v1/late-payment-charity-entry-procedure/{crId}/update               → Update
 *   PUT  /v1/late-payment-charity-entry-procedure/{crId}/control              → Control (suspend|resume|terminate)
 */
@RestController
@RequestMapping("/v1")
public class ServiceDomainController {

    private final ControlRecordStore store;

    public ServiceDomainController(ControlRecordStore store) {
        this.store = store;
    }

    @GetMapping("/service-domain")
    public Map<String, String> serviceDomain() {
        return Map.of(
                "serviceDomain", "Late Payment Charity Handling",
                "businessArea", "Islamic Financing",
                "businessDomain", "Lease And Partnership Financing",
                "functionalPattern", "Process",
                "assetType", "Late Payment Charity Entry",
                "controlRecord", "Late Payment Charity Entry Procedure",
                "version", "0.1.0",
                "phase", "1-shallow"
        );
    }

    @PostMapping("/late-payment-charity-entry-procedure/initiate")
    @CircuitBreaker(name = "serviceDomain")
    public ResponseEntity<ControlRecord> initiate(@RequestBody(required = false) Map<String, Object> properties) {
        return ResponseEntity.status(HttpStatus.CREATED).body(store.initiate(properties));
    }

    @GetMapping("/late-payment-charity-entry-procedure")
    public Collection<ControlRecord> list() {
        return store.list();
    }

    @GetMapping("/late-payment-charity-entry-procedure/{crId}/retrieve")
    public ResponseEntity<ControlRecord> retrieve(@PathVariable String crId) {
        return store.retrieve(crId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/late-payment-charity-entry-procedure/{crId}/update")
    public ResponseEntity<ControlRecord> update(@PathVariable String crId,
                                                @RequestBody Map<String, Object> properties) {
        return store.update(crId, properties)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/late-payment-charity-entry-procedure/{crId}/control")
    public ResponseEntity<?> control(@PathVariable String crId,
                                     @RequestBody Map<String, String> body) {
        try {
            return store.control(crId, body.get("action"))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
