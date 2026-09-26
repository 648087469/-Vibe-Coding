package com.investment.analysis.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源健康度跟踪（简易熔断器）：
 * 某数据源连续失败达到阈值后，在冷却期内直接跳过，避免每次请求都等待超时。
 */
@Component
public class ProviderHealthTracker {

    private static final Logger log = LoggerFactory.getLogger(ProviderHealthTracker.class);

    private final Map<String, State> states = new ConcurrentHashMap<String, State>();

    private static class State {
        private int consecutiveFailures;
        private long blockedUntilMillis;
    }

    /**
     * @return true 表示该数据源当前处于熔断冷却中，应跳过
     */
    public boolean isBlocked(String source) {
        State state = states.get(source);
        if (state == null) {
            return false;
        }
        synchronized (state) {
            return state.blockedUntilMillis > System.currentTimeMillis();
        }
    }

    public void recordSuccess(String source) {
        State state = states.computeIfAbsent(source, key -> new State());
        synchronized (state) {
            state.consecutiveFailures = 0;
            state.blockedUntilMillis = 0L;
        }
    }

    public void recordFailure(String source, int failureThreshold, int openMinutes) {
        State state = states.computeIfAbsent(source, key -> new State());
        synchronized (state) {
            state.consecutiveFailures++;
            if (state.consecutiveFailures >= Math.max(1, failureThreshold)) {
                state.blockedUntilMillis = System.currentTimeMillis() + Math.max(1, openMinutes) * 60_000L;
                log.warn("数据源 {} 连续失败 {} 次，熔断 {} 分钟", source, state.consecutiveFailures, openMinutes);
                state.consecutiveFailures = 0;
            }
        }
    }

    /**
     * 手动重置全部数据源状态（强制刷新时调用）。
     */
    public void resetAll() {
        states.clear();
    }
}
