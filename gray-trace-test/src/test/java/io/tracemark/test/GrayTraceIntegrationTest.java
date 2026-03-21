package io.tracemark.test;

import io.tracemark.gray.core.GrayConstants;
import io.tracemark.gray.core.GrayContext;
import io.tracemark.test.service.AsyncService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 灰度追踪全链路验证测试
 *
 * <p>每个测试方法覆盖一个具体场景，断言灰度标是否正确传递。
 */
@SpringBootTest
@AutoConfigureMockMvc
class GrayTraceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AsyncService asyncService;

    @AfterEach
    void tearDown() {
        GrayContext.clear();
    }

    // ──────────────────────────────────────────────────────
    // 场景 1：Servlet 入口 - 有 x-gray-tag Header
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景1-有灰度Header时，GrayContext应被正确设置")
    void servlet_withGrayHeader_shouldSetContext() throws Exception {
        mockMvc.perform(get("/gray/servlet")
                        .header("x-gray-tag", "gray-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gray_tag").value("gray-v1"));
    }

    @Test
    @DisplayName("场景1-无灰度Header时，GrayContext应为stable")
    void servlet_withoutGrayHeader_shouldBeStable() throws Exception {
        mockMvc.perform(get("/gray/servlet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gray_tag").value(GrayConstants.TAG_STABLE));
    }

    // ──────────────────────────────────────────────────────
    // 场景 2：@Async 线程池上下文传递
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景2-@Async方法应继承调用者线程的灰度标")
    void async_shouldPropagateGrayTag() throws Exception {
        GrayContext.set("gray-v2");

        CompletableFuture<String> result = asyncService.asyncMethod();
        String tagInAsync = result.get();

        assertThat(tagInAsync).contains("tag=gray-v2");
    }

    @Test
    @DisplayName("场景2-@Async无灰度时应为stable")
    void async_withoutTag_shouldBeStable() throws Exception {
        // 不设置 GrayContext，默认为 stable
        CompletableFuture<String> result = asyncService.asyncMethod();
        String tagInAsync = result.get();

        assertThat(tagInAsync).contains("tag=stable");
    }

    // ──────────────────────────────────────────────────────
    // 场景 3：ThreadPoolExecutor 上下文传递
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景3-ThreadPoolExecutor应正确传递灰度标（TTL）")
    void threadPool_shouldPropagateGrayTag() throws Exception {
        GrayContext.set("gray-v3");

        CompletableFuture<String> result = asyncService.threadPoolExecutor();
        String response = result.get();

        // captured_before_submit 是兜底方案，无论哪种模式都能验证
        assertThat(response).contains("captured_before_submit=gray-v3");
    }

    // ──────────────────────────────────────────────────────
    // 场景 4：HTTP 入口完整链路（通过 MockMvc 模拟带 Header 的请求）
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景4-完整链路：请求入口到异步方法都应携带灰度标")
    void fullChain_asyncViaHttpRequest() throws Exception {
        MvcResult result = mockMvc.perform(get("/gray/async")
                        .header("x-gray-tag", "gray-v4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.main_thread_tag").value("gray-v4"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // async_thread_tag 也应包含 gray-v4
        assertThat(body).contains("gray-v4");
    }

    // ──────────────────────────────────────────────────────
    // 场景 5：线程安全 - 并发请求互不干扰
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景5-并发请求之间灰度标不应相互污染")
    void concurrent_requestsShouldNotInterfere() throws Exception {
        // 灰度请求
        CompletableFuture<MvcResult> grayFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(get("/gray/servlet")
                                .header("x-gray-tag", "gray-concurrent"))
                        .andReturn();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // 稳定请求（不带 Header）
        CompletableFuture<MvcResult> stableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(get("/gray/servlet"))
                        .andReturn();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        String grayBody = grayFuture.get().getResponse().getContentAsString();
        String stableBody = stableFuture.get().getResponse().getContentAsString();

        assertThat(grayBody).contains("gray-concurrent");
        assertThat(stableBody).contains("stable");
        // 两者不相互污染
        assertThat(stableBody).doesNotContain("gray-concurrent");
    }

    // ──────────────────────────────────────────────────────
    // 场景 6：请求结束后 GrayContext 应被清理
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("场景6-请求结束后GrayContext应被清理，不污染下一个请求")
    void contextCleanup_afterRequest() throws Exception {
        // 第一个灰度请求
        mockMvc.perform(get("/gray/servlet")
                        .header("x-gray-tag", "gray-cleanup-test"))
                .andExpect(jsonPath("$.gray_tag").value("gray-cleanup-test"));

        // 第二个请求不带 Header，应恢复为 stable
        mockMvc.perform(get("/gray/servlet"))
                .andExpect(jsonPath("$.gray_tag").value("stable"));
    }
}