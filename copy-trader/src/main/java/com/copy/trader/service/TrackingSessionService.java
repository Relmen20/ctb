package com.copy.trader.service;

import com.copy.trader.task.SolanaTransactionTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TrackingSessionService {

    public static final String DELIMITER = ":";

    private final ConcurrentHashMap<String, SolanaTransactionTracker> followTrackerMap = new ConcurrentHashMap<>();

    public void putTracker(String key, SolanaTransactionTracker tracker) {
        followTrackerMap.put(key, tracker);
    }

    public SolanaTransactionTracker getTracker(String key) {
        return followTrackerMap.get(key);
    }

    public void removeTracker(String key) {
        followTrackerMap.remove(key);
    }

    public boolean containsTracker(String key) {
        return followTrackerMap.containsKey(key);
    }
}
