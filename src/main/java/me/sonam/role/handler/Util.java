package me.sonam.role.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.reactive.function.server.ServerRequest;

public class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    public static PageRequest getPageable(ServerRequest serverRequest) {
        LOG.debug("constructing page");
        int page = 0;
        int size = 100;
        String sortby = null;

        try {
            if (serverRequest.queryParams().getFirst("page") != null && !serverRequest.queryParams().getFirst("page").isEmpty()) {
                page = Integer.parseInt(serverRequest.queryParams().getFirst("page"));
            }
            if (serverRequest.queryParams().getFirst("size") != null && !serverRequest.queryParams().getFirst("size").isEmpty()) {
                size = Integer.parseInt(serverRequest.queryParams().getFirst("size"));
            }
            if (serverRequest.queryParams().getFirst("sortBy") != null && !serverRequest.queryParams().getFirst("sortBy").isEmpty()) {
                sortby = serverRequest.queryParams().getFirst("sortBy");

            }
        }
        catch (IllegalArgumentException e) {
            LOG.warn("no page/size or variable found, use default page {}, and size: {}", page, size);
        }
        if (sortby != null) {
            return PageRequest.of(page, size, Sort.by(sortby));
        }
        else {
            return PageRequest.of(page, size);
        }
    }
}
