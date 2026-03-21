package io.tracemark.gray.autoconfigure.filter;

import io.tracemark.gray.core.GrayConstants;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Spring Boot 2.x（javax.servlet）版入口过滤器
 *
 * <p>从每个入站请求的 Header 中提取 {@code x-gray-tag}，
 * 写入 {@link io.tracemark.gray.core.GrayContext}，在请求结束时清理。
 */
public class GrayServletFilter extends AbstractGrayFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String tag = GrayConstants.TAG_STABLE;
        if (request instanceof HttpServletRequest) {
            tag = resolveTag(((HttpServletRequest) request).getHeader(GrayConstants.HEADER_GRAY_TAG));
        }
        final String finalTag = tag;
        try {
            doGrayFilter(finalTag, () -> chain.doFilter(request, response));
        } catch (IOException | ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy() {}
}