package com.leadradar.lead;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {
    private final LeadRepository repo;

    public LeadController(LeadRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<StoredLead> list(@RequestParam(required = false) String merchantId,
                                 @RequestParam(required = false) LeadStatus status,
                                 @RequestParam(defaultValue = "50") int limit) {
        return repo.find(merchantId, status, Math.min(Math.max(limit, 1), 500));
    }

    public record StatusUpdate(LeadStatus status, String note) {
    }

    /** 记录跟进结果，例如 {"status":"WON","note":"4人火锅，消费120万"}。 */
    @PatchMapping("/{id}")
    public StoredLead update(@PathVariable long id, @RequestBody StatusUpdate body) {
        if (body == null || body.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 必填");
        }
        if (!repo.updateStatus(id, body.status(), body.note())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "线索不存在: " + id);
        }
        return repo.findById(id).orElseThrow();
    }
}
