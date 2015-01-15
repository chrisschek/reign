package io.reign.metrics;

import java.util.HashMap;
import java.util.Map;

import com.clearspring.analytics.stream.ConcurrentStreamSummary;
import com.clearspring.analytics.stream.ScoredItem;

/**
 * 
 * @author mxing
 *
 */
public class TopKGauge implements MergeableGauge<Map<String, Integer>> {

    private final static int streamSummaryCapacity = 1000;

    private int k;
    private ConcurrentStreamSummary<String> streamSummary;

    public TopKGauge() {
        this.k = 10;
        streamSummary = new ConcurrentStreamSummary<String>(streamSummaryCapacity);
    }

    public TopKGauge(int k) {
        this.k = k;
        streamSummary = new ConcurrentStreamSummary<String>(streamSummaryCapacity);
    }

    public void offer(String element) {
        streamSummary.offer(element);
    }

    @Override
    public Map<String, Integer> getValue() {
        Map<String, Integer> topK = new HashMap<String, Integer>();
        for (ScoredItem<String> score : streamSummary.peekWithScores(k)) {
            topK.put(score.getItem(), (int) score.getCount());
        }
        return topK;
    }

    @Override
    public MergeFunction<GaugeData<Map<String, Integer>>> getMergeFunction() {
        return new CounterMapMergeFunction(30);
    }
}
