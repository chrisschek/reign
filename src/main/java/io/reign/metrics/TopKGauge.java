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
public class TopKGauge implements MergeableGauge<Map<String, Integer>, Map<String, Integer>> {

    private int k;
    private ConcurrentStreamSummary<String> streamSummary;

    public TopKGauge() {
        this.k = 10;
        streamSummary = new ConcurrentStreamSummary<String>(1000);
    }

    public TopKGauge(int k, int capacity) {
        this.k = k;
        streamSummary = new ConcurrentStreamSummary<String>(capacity);
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
    public MergeFunction<GaugeData<Map<String, Integer>>, GaugeData<Map<String, Integer>>> getMergeFunction() {
        return new CounterMapMergeFunction(30);
    }
}
