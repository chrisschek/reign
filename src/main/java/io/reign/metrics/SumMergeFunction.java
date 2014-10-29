package io.reign.metrics;

import java.util.List;

/**
 * 
 * @author ypai
 *
 */
public class SumMergeFunction<T extends Number> implements MergeFunction<GaugeData<T>> {

    @Override
    public GaugeData<T> merge(List<GaugeData<T>> dataList) {
        if (dataList == null || dataList.size() < 1) {
            throw new IllegalStateException("dataList is null or empty.");
        }

        Number value = null;
        int i = 0;
        for (i = 0; i < dataList.size(); i++) {
            value = dataList.get(i).getValue();
            if (value != null) {
                break;
            }
        }

        if (value instanceof Long) {
            long result = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    result += value.longValue();
                }
            }
            return (GaugeData<T>) new GaugeData<Long>(result);

        } else if (value instanceof Double) {
            double result = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    result += value.doubleValue();
                }
            }
            return (GaugeData<T>) new GaugeData<Double>(result);

        } else if (value instanceof Float) {
            float result = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    result += value.floatValue();
                }
            }
            return (GaugeData<T>) new GaugeData<Float>(result);

        } else if (value instanceof Integer) {
            int result = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    result += value.intValue();
                }
            }
            return (GaugeData<T>) new GaugeData<Integer>(result);

        } else if (value instanceof Short) {
            short result = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    result += value.shortValue();
                }
            }
            return (GaugeData<T>) new GaugeData<Short>(result);

        } else if (value instanceof Byte) {
            byte result = 0;
            for (GaugeData<? extends Number> item : dataList) {
                value = item.getValue();
                if (value != null) {
                    result += value.byteValue();
                }
            }
            return (GaugeData<T>) new GaugeData<Byte>(result);

        } else {
            throw new IllegalArgumentException("Unsupported type:  " + value.getClass());
        }

    }
}
