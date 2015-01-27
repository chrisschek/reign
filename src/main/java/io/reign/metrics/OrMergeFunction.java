package io.reign.metrics;

import java.util.List;

/**
 *
 * @author ypai
 *
 */
public class OrMergeFunction implements MergeFunction<GaugeData<Boolean>, GaugeData<Boolean>> {

    private boolean defaultValue = false;

    /** null is interpreted as false; if false, null is ignored */
    private boolean nullEqualsFalse = true;

    public OrMergeFunction defaultValue(boolean defaultValue) {
        setDefaultValue(defaultValue);
        return this;
    }

    public OrMergeFunction nullEqualsFalse(boolean nullEqualsFalse) {
        setNullEqualsFalse(nullEqualsFalse);
        return this;
    }

    public void setDefaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isNullEqualsFalse() {
        return nullEqualsFalse;
    }

    public void setNullEqualsFalse(boolean nullEqualsFalse) {
        this.nullEqualsFalse = nullEqualsFalse;
    }

    @Override
    public GaugeData<Boolean> merge(List<GaugeData<Boolean>> dataList) {
        if (dataList == null || dataList.size() < 1) {
            return new GaugeData<Boolean>(defaultValue);
        }

        Boolean result = dataList.get(0).getValue();
        for (int i = 1; i < dataList.size(); i++) {
            Boolean value = dataList.get(i).getValue();
            if (value == null) {
                if (nullEqualsFalse) {
                    result = result || false;
                    break;
                }
            } else {
                result = result || value;
            }
        }
        return new GaugeData<Boolean>(result);
    }
}
