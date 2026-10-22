package com.leadradar.source;

import com.leadradar.lead.Post;

import java.time.Instant;
import java.util.List;

/**
 * 数据源。约定：只读、低频、模拟正常浏览节奏；优先公开站点和官方接口。
 */
public interface PostSource {
    String name();

    /** 拉取 since 之后的新帖。 */
    List<Post> fetchSince(Instant since);
}
