package com.trustai.service;

import java.time.LocalDate;

public interface RateLimiterService {
    void checkQuota(String modelCode, int limit, LocalDate date);
    void increment(String modelCode, LocalDate date);
    void reset(String modelCode, LocalDate date);
}
