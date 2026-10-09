package com.tradex.node.service;

import com.tradex.node.entity.DistributedEvent;
import com.tradex.node.repository.DistributedEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LamportClockService {

    private final AtomicLong clock = new AtomicLong(0);
    private final DistributedEventRepository eventRepository;

    @Value("${node.name:node1}")
    private String nodeName;

    public LamportClockService(DistributedEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public synchronized long getClock() {
        return clock.get();
    }

    public synchronized long getTime() {
        return clock.get();
    }

    public synchronized long tick() {
        return clock.incrementAndGet();
    }

    public synchronized long update(long receivedTimestamp) {
        long maxClock = Math.max(clock.get(), receivedTimestamp);
        long newClock = maxClock + 1;
        clock.set(newClock);
        return newClock;
    }

    public synchronized DistributedEvent recordLocalEvent(String description) {
        long current = clock.incrementAndGet();
        DistributedEvent event = new DistributedEvent(
                nodeName, "LOCAL_EVENT", current, nodeName, null, null, description
        );
        return eventRepository.save(event);
    }

    public synchronized long prepareSendEvent(String targetNode, String description) {
        long current = clock.incrementAndGet();
        DistributedEvent event = new DistributedEvent(
                nodeName, "MESSAGE_SENT", current, nodeName, targetNode, null, description
        );
        eventRepository.save(event);
        return current;
    }

    public synchronized DistributedEvent recordReceiveEvent(String sourceNode, long receivedTimestamp, String description) {
        long maxClock = Math.max(clock.get(), receivedTimestamp);
        long newClock = maxClock + 1;
        clock.set(newClock);

        DistributedEvent event = new DistributedEvent(
                nodeName, "MESSAGE_RECEIVED", newClock, sourceNode, nodeName, receivedTimestamp, description
        );
        return eventRepository.save(event);
    }

    public List<DistributedEvent> getRecentEvents() {
        return eventRepository.findTop50ByOrderByWallClockTimeDesc();
    }

    public DistributedEvent logEvent(String eventType, String sourceNode, String destNode, Long receivedTimestamp, String desc) {
        long current = clock.incrementAndGet();
        DistributedEvent event = new DistributedEvent(
                nodeName, eventType, current, sourceNode, destNode, receivedTimestamp, desc
        );
        return eventRepository.save(event);
    }
}
