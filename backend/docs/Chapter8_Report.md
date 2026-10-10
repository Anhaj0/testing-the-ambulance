# Chapter 8: Emergency Patient Triage & Dispatch Decision Engine Analysis

## 1. Introduction
This chapter details the complexity analysis and performance benchmarking of the Triage Decision Engine (Task 4) implemented in the Ambulance Dispatch System. The engine consists of a hybrid approach utilizing the Manchester Triage System (MTS) decision tree for categorization and a weighted scoring system coupled with a synchronized binary max-heap for priority resolution. We also compare our primary approach to a K-Nearest Neighbors (KNN) classifier algorithm to demonstrate the tradeoffs.

## 2. Time and Space Complexity Analysis

### 2.1 MTS Decision Tree
* **Time Complexity**: $O(1)$
  * The evaluation path length is bounded by a constant number of fixed physiological checks (systolic BP, oxygen saturation, temperature, etc.). It does not depend on the number of existing patients ($N$).
* **Space Complexity**: $O(1)$
  * The algorithm evaluates inline without constructing or retaining any new dynamic data structures proportional to $N$.

### 2.2 Priority Dispatch Queue (Binary Max-Heap)
* **Time Complexity**: $O(\log N)$ for Insertion and Extraction.
  * Maintaining the heap property requires bubbling up/sinking down nodes along the height of the binary tree, bounded by $\log N$.
* **Space Complexity**: $O(N)$
  * The heap stores all active (unresolved) assessments.

### 2.3 KNN Classifier (Alternative Baseline)
* **Time Complexity**: $O(N \log K)$ or $O(N \cdot D)$
  * Computing the Euclidean distance to every historical point requires iterating over all $N$ records. Sorting or maintaining a top-K heap takes additional time, rendering this approach strictly linear with respect to the dataset size $N$.
* **Space Complexity**: $O(N)$ Resident Memory Footprint
  * Requires loading the entire historical training dataset of size $N$ into memory (or querying the database repeatedly).

## 3. Empirical Benchmark Results

Benchmarks were executed using custom-built timing loops with `System.nanoTime()` in `TriageBenchmarkTest.java`, scaling from $N=100$ to $N=100,000$.

### 3.1 Execution Time & Empirical Growth Rates

| Algorithm | N Range | Empirical Time Ratio | Expected $O(1)$ | Expected $O(\log N)$ | Expected $O(N)$ |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **MTSDecisionTree** | $10,000 \to 100,000$ | 1.035x | 1.000x | - | - |
| **KNNClassifier** | $10,000 \to 100,000$ | 13.112x | - | - | 10.000x |

**Analysis**:
The empirical ratio represents the time multiplier when dataset size $N$ increases by a factor of 10. 
* **MTSDecisionTree** maintained a ratio of ~1.0x, firmly verifying its $O(1)$ constant time complexity. Execution times remained flat at roughly `0.05ms`.
* **KNNClassifier** yielded ratios consistently between 11x and 13.5x, confirming its $O(N)$ linear time complexity, eventually taking over `26 seconds` per query at $N=100,000$. 

*(Note: The empirical ratio for WeightedScore+Heap fluctuated due to JVM warm-up characteristics and microsecond-scale execution speeds, but remained exceptionally fast at ~0.07ms).*

### 3.2 Memory Footprint Validation

To accurately profile the *High memory footprint* limitation inherent to the KNN approach, we explicitly measured the resident memory required to house the pre-computed historical dataset in memory:

| N (Dataset Size) | KNN Resident Footprint |
| :--- | :--- |
| 100 | ~9.04 KB |
| 1,000 | ~113.77 KB |
| 10,000 | ~1,143.83 KB |
| 100,000 | ~11,299.44 KB (11.3 MB) |

**Analysis**:
The KNN Resident memory grew linearly with $N$ directly fulfilling the expected $O(N)$ space complexity. Loading millions of clinical records for active real-time triage scoring using KNN would be computationally and memory prohibitive without extensive batching or dedicated vector databases. 

In contrast, the `MTSDecisionTree` requires no resident training data and maintains a flat constant baseline footprint.

## 4. Conclusion
The implementation of the hybrid MTS Decision Tree + Max-Heap Dispatch Queue successfully satisfies the strict latency requirements of an Emergency Dispatch Engine. The benchmarks explicitly validate the theoretical complexity expectations, establishing the superiority of our algorithmic design over a pure distance-based classification model (KNN) in terms of both processing speed and memory utilization.
