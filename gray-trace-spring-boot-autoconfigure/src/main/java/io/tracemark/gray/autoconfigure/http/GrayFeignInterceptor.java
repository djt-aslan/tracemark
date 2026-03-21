package io.tracemark.gray.autoconfigure.http;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * OpenFeign 出口灰度头传递拦截器
 */
public class GrayFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String tag = GrayContext.get();
        if (tag != null && !tag.isEmpty()) {
            template.header(GrayConstants.HEADER_GRAY_TAG, tag);
        }
    }
}