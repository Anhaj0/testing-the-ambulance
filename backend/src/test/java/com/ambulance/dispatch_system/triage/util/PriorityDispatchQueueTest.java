package com.ambulance.dispatch_system.triage.util;

import com.ambulance.dispatch_system.common.entity.TriageAssessment;
import com.ambulance.dispatch_system.common.entity.enums.TriageCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PriorityDispatchQueueTest {

    private PriorityDispatchQueue queue;

    @BeforeEach
    void setUp() {
        queue = new PriorityDispatchQueue();
    }

    private TriageAssessment createAssessment(TriageCategory category, double score, LocalDateTime time) {
        TriageAssessment ta = new TriageAssessment();
        ta.setId(UUID.randomUUID());
        ta.setAssignedCategory(category);
        ta.setTieBreakerScore(score);
        ta.setCreatedAt(time);
        return ta;
    }

    @Test
    void testInsertOutOfOrderAndExtractMaxOrder() {
        LocalDateTime now = LocalDateTime.now();
        
        TriageAssessment a1 = createAssessment(TriageCategory.GREEN, 5.0, now.minusMinutes(10));
        TriageAssessment a2 = createAssessment(TriageCategory.RED, 15.0, now.minusMinutes(5));
        TriageAssessment a3 = createAssessment(TriageCategory.YELLOW, 10.0, now.minusMinutes(2));
        TriageAssessment a4 = createAssessment(TriageCategory.RED, 20.0, now.minusMinutes(1));
        TriageAssessment a5 = createAssessment(TriageCategory.RED, 20.0, now.minusMinutes(15)); // Older tiebreaker
        
        queue.insert(a1);
        queue.insert(a2);
        queue.insert(a3);
        queue.insert(a4);
        queue.insert(a5);
        
        assertEquals(5, queue.size());
        
        // a5 should be first because RED, Score=20, and older time
        assertSame(a5, queue.extractMax());
        
        // a4 should be second because RED, Score=20, newer time
        assertSame(a4, queue.extractMax());
        
        // a2 should be third because RED, Score=15
        assertSame(a2, queue.extractMax());
        
        // a3 should be fourth because YELLOW
        assertSame(a3, queue.extractMax());
        
        // a1 should be fifth because GREEN
        assertSame(a1, queue.extractMax());
        
        assertTrue(queue.isEmpty());
    }

    @Test
    void testPeekDoesNotMutate() {
        TriageAssessment a1 = createAssessment(TriageCategory.RED, 10.0, LocalDateTime.now());
        queue.insert(a1);
        
        assertEquals(1, queue.size());
        assertSame(a1, queue.peek());
        assertEquals(1, queue.size()); // Size should remain the same
        assertSame(a1, queue.peek()); // Subsequent peek should return the same
    }

    @Test
    void testBehaviorOnEmptyQueue() {
        assertTrue(queue.isEmpty());
        
        assertThrows(NoSuchElementException.class, () -> queue.peek());
        assertThrows(NoSuchElementException.class, () -> queue.extractMax());
    }

    @Test
    void testGetRank() {
        TriageAssessment a1 = createAssessment(TriageCategory.GREEN, 5.0, LocalDateTime.now());
        TriageAssessment a2 = createAssessment(TriageCategory.RED, 15.0, LocalDateTime.now());
        TriageAssessment a3 = createAssessment(TriageCategory.YELLOW, 10.0, LocalDateTime.now());
        
        queue.insert(a1);
        queue.insert(a2);
        queue.insert(a3);
        
        // RED (a2) is rank 1, YELLOW (a3) is rank 2, GREEN (a1) is rank 3
        assertEquals(1, queue.getRank(a2));
        assertEquals(2, queue.getRank(a3));
        assertEquals(3, queue.getRank(a1));
    }
    
    @Test
    void testGetRankOrderingCorrectness() {
        // GREEN patient with high tiebreaker inserted first
        TriageAssessment green = createAssessment(TriageCategory.GREEN, 99.0, LocalDateTime.now().minusMinutes(10));
        green.setSeverityRank(TriageCategory.GREEN.getSeverity());
        
        // RED patient with low tiebreaker inserted second
        TriageAssessment red = createAssessment(TriageCategory.RED, 1.0, LocalDateTime.now());
        red.setSeverityRank(TriageCategory.RED.getSeverity());
        
        queue.insert(green);
        queue.insert(red);
        
        // RED patient should get rank 1 (better) despite lower score and later arrival, because severityRank dominates
        assertEquals(1, queue.getRank(red));
        assertEquals(2, queue.getRank(green));
    }
    
    @Test
    void testRemove() {
        TriageAssessment a1 = createAssessment(TriageCategory.RED, 15.0, LocalDateTime.now());
        TriageAssessment a2 = createAssessment(TriageCategory.YELLOW, 10.0, LocalDateTime.now());
        TriageAssessment a3 = createAssessment(TriageCategory.GREEN, 5.0, LocalDateTime.now());
        
        queue.insert(a1);
        queue.insert(a2);
        queue.insert(a3);
        
        assertEquals(3, queue.size());
        
        assertTrue(queue.remove(a2.getId()));
        assertEquals(2, queue.size());
        
        // Ensure YELLOW is gone
        assertSame(a1, queue.extractMax());
        assertSame(a3, queue.extractMax());
        assertTrue(queue.isEmpty());
    }
    
    @Test
    void testConcurrency() throws InterruptedException {
        int threads = 10;
        int insertsPerThread = 100;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threads);
        
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < insertsPerThread; j++) {
                    queue.insert(createAssessment(TriageCategory.RED, j, LocalDateTime.now()));
                }
                latch.countDown();
            });
        }
        
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(threads * insertsPerThread, queue.size());
        
        // Ensure invariants hold (heap property)
        TriageAssessment prev = queue.extractMax();
        while (!queue.isEmpty()) {
            TriageAssessment current = queue.extractMax();
            // Since we extracted the max, previous should be >= current
            assertTrue(prev.getTieBreakerScore() >= current.getTieBreakerScore() || prev.getCreatedAt().isBefore(current.getCreatedAt()));
            prev = current;
        }
        executor.shutdown();
    }
}
