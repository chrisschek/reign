package io.reign.metrics;

import java.util.List;

public class CounterMergeFunction implements MergeFunction<CounterData> {

    @Override
    public CounterData merge(List<CounterData> dataList) {
        long sum = 0;
        for (CounterData data : dataList) {
            sum += data.getCount();
        }
        CounterData counterData = new CounterData();
        counterData.setCount(sum);
        return counterData;
    }

}
