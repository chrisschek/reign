package io.reign.metrics;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

/**
 * 
 * @author mxing
 *
 */
public class CounterMapMergeFunction implements MergeFunction<GaugeData<Map<String, Integer>>> {

    private int maxCounters;

    public CounterMapMergeFunction() {
        this.maxCounters = 10;
    }

    public CounterMapMergeFunction(int maxCounters) {
        this.maxCounters = maxCounters;
    }

    @Override
    public GaugeData<Map<String, Integer>> merge(List<GaugeData<Map<String, Integer>>> dataList) {
        Map<String, Integer> mergedCounters = new HashMap<String, Integer>();
        for (GaugeData<Map<String, Integer>> gaugeData : dataList) {
            for (Entry<String, Integer> entry : gaugeData.getValue().entrySet()) {
                String key = entry.getKey();
                int count = entry.getValue();
                // if already exists, add on to it
                if (mergedCounters.containsKey(key)) {
                    int newCount = mergedCounters.get(key) + count;
                    mergedCounters.put(key, newCount);
                } else {
                    mergedCounters.put(key, count);
                }
            }
        }

        // sort
        Ordering<Map.Entry<String, Integer>> counterOrdering = new Ordering<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Entry<String, Integer> left, Entry<String, Integer> right) {
                return Ints.compare(left.getValue(), right.getValue());
            }
        };

        Map<String, Integer> topCounters = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : counterOrdering.greatestOf(mergedCounters.entrySet(), maxCounters)) {
            topCounters.put(entry.getKey(), entry.getValue());
        }

        GaugeData<Map<String, Integer>> mergedData = new GaugeData<Map<String, Integer>>();
        mergedData.setValue(topCounters);
        return mergedData;
    }

}
