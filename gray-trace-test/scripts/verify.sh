#!/bin/bash
# ============================================================
# gray-trace 手动验证脚本
# 前提：gray-trace-test 服务已启动（mvn spring-boot:run）
# ============================================================

BASE_URL="http://localhost:8080"
GRAY_TAG="gray-v1"

echo ""
echo "========================================"
echo " 场景 1: Servlet 入口 - 灰度请求"
echo "========================================"
curl -s -H "x-gray-tag: $GRAY_TAG" "$BASE_URL/gray/servlet" | python3 -m json.tool

echo ""
echo "========================================"
echo " 场景 1: Servlet 入口 - 稳定请求（无Header）"
echo "========================================"
curl -s "$BASE_URL/gray/servlet" | python3 -m json.tool

echo ""
echo "========================================"
echo " 场景 2+3: @Async + ThreadPool - 灰度请求"
echo "========================================"
curl -s -H "x-gray-tag: $GRAY_TAG" "$BASE_URL/gray/async" | python3 -m json.tool
curl -s -H "x-gray-tag: $GRAY_TAG" "$BASE_URL/gray/threadpool" | python3 -m json.tool

echo ""
echo "========================================"
echo " 场景 5+6: HTTP 出口 - 灰度请求（观察 echo 中是否含 x-gray-tag）"
echo "========================================"
curl -s -H "x-gray-tag: $GRAY_TAG" "$BASE_URL/gray/http" | python3 -m json.tool

echo ""
echo "========================================"
echo " 全链路 all-in-one"
echo "========================================"
curl -s -H "x-gray-tag: $GRAY_TAG" "$BASE_URL/gray/all" | python3 -m json.tool

echo ""
echo "========================================"
echo " 并发压测（10 灰度 + 10 稳定）"
echo "========================================"
for i in $(seq 1 10); do
  curl -s -H "x-gray-tag: $GRAY_TAG" "$BASE_URL/gray/servlet" &
  curl -s "$BASE_URL/gray/servlet" &
done
wait
echo "并发请求完成，请检查日志是否存在上下文串扰"
