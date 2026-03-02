package com.trustai.service.impl;

import com.trustai.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class RateLimiterServiceImpl implements RateLimiterService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final StringRedisTemplate redisTemplate;

    @Override
    public void checkQuota(String modelCode, int limit, LocalDate date) {
        if (limit <= 0) return; // 0 表示不限
        String key = key(modelCode, date);
        String value = redisTemplate.opsForValue().get(key);
        long current = value == null ? 0L : Long.parseLong(value);
        if (current >= limit) {
            throw new IllegalStateException("模型" + modelCode + "今日调用已达上限" + limit);
        }
    }

    @Override
    public void increment(String modelCode, LocalDate date) {
        String key = key(modelCode, date);
        redisTemplate.opsForValue().increment(key, 1);
    }

    @Override
    public void reset(String modelCode, LocalDate date) {
        redisTemplate.delete(key(modelCode, date));
    }

    private String key(String modelCode, LocalDate date) {
        return "ai:call:" + modelCode + ":" + DATE.format(date);
    }
}
