package io.reign.metrics;

/**
 * 
 * @author ypai
 * 
 */
public class GaugeData<T> {

    private T value;

    public GaugeData() {

    }

    public GaugeData(T initialValue) {
        this.value = initialValue;

    }

    public T getValue() {
        return value;
    }

    void setValue(T value) {
        this.value = value;
    }

}
