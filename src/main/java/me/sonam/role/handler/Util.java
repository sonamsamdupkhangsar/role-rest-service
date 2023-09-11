package me.sonam.role.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.reactive.function.server.ServerRequest;

public class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    public static PageRequest getPageable(ServerRequest serverRequest) {
        int page = 0;
        int size = 100;

        try {
            if (serverRequest.pathVariable("page") != null && !serverRequest.pathVariable("page").isEmpty()) {
                page = Integer.parseInt(serverRequest.pathVariable("page"));
            }
            if (serverRequest.pathVariable("size") != null && !serverRequest.pathVariable("size").isEmpty()) {
                size = Integer.parseInt(serverRequest.pathVariable("size"));
            }
        }
        catch (IllegalArgumentException e) {
            LOG.warn("no page/size or variable found, use default page {}, and size: {}", page, size);
        }
        return PageRequest.of(page, size);
    }
}
