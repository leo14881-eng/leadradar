package com.leadradar.source;

import com.leadradar.lead.Post;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class ManualImportController {
    private final ManualImportSource source;

    public ManualImportController(ManualImportSource source) {
        this.source = source;
    }

    public record ImportRequest(String source, String url, String author, String text) {
    }

    @PostMapping
    public String submit(@RequestBody ImportRequest req) {
        var id = req.url() != null ? req.url() : UUID.randomUUID().toString();
        source.submit(new Post(req.source() == null ? "manual" : req.source(), id, req.url(), req.author(), req.text(), Instant.now()));
        return "queued";
    }
}
