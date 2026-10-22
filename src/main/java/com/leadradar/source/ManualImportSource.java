package com.leadradar.source;

import com.leadradar.lead.Post;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 验证期数据源：人工（或 Claude 浏览后）把帖子通过 HTTP 接口塞进来。
 * 跑通整条链路后，再加 Facebook 群组 / Chotot 等自动源。
 */
@Component
public class ManualImportSource implements PostSource {
    private final ConcurrentLinkedQueue<Post> inbox = new ConcurrentLinkedQueue<>();

    public void submit(Post post) {
        inbox.add(post);
    }

    @Override
    public String name() {
        return "manual";
    }

    @Override
    public List<Post> fetchSince(Instant since) {
        var out = new java.util.ArrayList<Post>();
        Post p;
        while ((p = inbox.poll()) != null) {
            out.add(p);
        }
        return out;
    }
}
