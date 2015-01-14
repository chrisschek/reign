package io.reign.metrics;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.codahale.metrics.Counter;

/**
 * 
 * @author ypai
 *
 */
public class ListCountersMergeFunction implements MergeFunction<GaugeData<Map<String, Integer>>> {

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
        GaugeData<Map<String, Integer>> mergedData = new GaugeData<Map<String,Integer>>();
        mergedData.setValue(mergedCounters);
        return mergedData;
    }
    
}
