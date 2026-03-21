package io.tracemark.test.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 下游回显接口 - 把收到的请求头原样返回
 *
 * <p>用于验证上游服务调用时是否正确透传了 x-gray-tag Header。
 * HttpClientService 调用本接口，观察响应中是否含有 x-gray-tag。
 */
@RestController
@RequestMapping("/gray")
public class EchoController {

    @GetMapping("/echo")
    public Map<String, Object> echo(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 收集所有请求头
        Map<String, String> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames())
                .forEach(name -> headers.put(name, request.getHeader(name)));

        result.put("received_headers", headers);
        result.put("x_gray_tag", request.getHeader("x-gray-tag"));
        result.put("description", "x-gray-tag present = " + (request.getHeader("x-gray-tag") != null));
        return result;
    }
}