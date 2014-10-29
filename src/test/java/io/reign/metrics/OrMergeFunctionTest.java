package io.reign.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * 
 * @author ypai
 *
 */
public class OrMergeFunctionTest {
    @Test
    public void testNullDataListDefaultFalse() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();
        GaugeData<Boolean> gd = mf.merge(null);
        assertFalse(gd.getValue());
    }

    @Test
    public void testNullDataListDefaultFalseIgnoreNullValues() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();
        mf.nullEqualsFalse(false);
        GaugeData<Boolean> gd = mf.merge(null);
        assertFalse(gd.getValue());
    }

    @Test
    public void testNullDataListDefaultTrue() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();
        mf.defaultValue(true);
        GaugeData<Boolean> gd = mf.merge(null);
        assertTrue(gd.getValue());
    }

    @Test
    public void testNullDataListDefaultTrueIgnoreNullValues() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();
        mf.defaultValue(true).nullEqualsFalse(false);
        GaugeData<Boolean> gd = mf.merge(null);
        assertTrue(gd.getValue());
    }

    @Test
    public void testEmptyDataListDefaultFalse() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();
        GaugeData<Boolean> gd = mf.merge(new ArrayList<GaugeData<Boolean>>());
        assertFalse(gd.getValue());
    }

    @Test
    public void testEmptyDataListDefaultFalseIgnoreNullValues() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();
        mf.nullEqualsFalse(false);
        GaugeData<Boolean> gd = mf.merge(new ArrayList<GaugeData<Boolean>>());
        assertFalse(gd.getValue());
    }

    @Test
    public void testEmptyDataListDefaultTrue() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();
        mf.defaultValue(true);
        GaugeData<Boolean> gd = mf.merge(new ArrayList<GaugeData<Boolean>>());
        assertTrue(gd.getValue());
    }

    @Test
    public void testEmptyDataListDefaultTrueIgnoreNullValues() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();
        mf.defaultValue(true).nullEqualsFalse(false);
        GaugeData<Boolean> gd = mf.merge(new ArrayList<GaugeData<Boolean>>());
        assertTrue(gd.getValue());
    }

    @Test
    public void testTrueTrue() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();

        List<GaugeData<Boolean>> dataList = new ArrayList<GaugeData<Boolean>>();
        dataList.add(new GaugeData<Boolean>(true));
        dataList.add(new GaugeData<Boolean>(true));

        GaugeData<Boolean> gd = mf.merge(dataList);
        assertTrue(gd.getValue());
    }

    @Test
    public void testTrueFalse() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();

        List<GaugeData<Boolean>> dataList = new ArrayList<GaugeData<Boolean>>();
        dataList.add(new GaugeData<Boolean>(true));
        dataList.add(new GaugeData<Boolean>(false));

        GaugeData<Boolean> gd = mf.merge(dataList);
        assertTrue(gd.getValue());
    }

    @Test
    public void testFalseTrue() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();

        List<GaugeData<Boolean>> dataList = new ArrayList<GaugeData<Boolean>>();
        dataList.add(new GaugeData<Boolean>(false));
        dataList.add(new GaugeData<Boolean>(true));

        GaugeData<Boolean> gd = mf.merge(dataList);
        assertTrue(gd.getValue());
    }

    @Test
    public void testFalseFalse() throws Exception {
        OrMergeFunction mf = new OrMergeFunction();

        List<GaugeData<Boolean>> dataList = new ArrayList<GaugeData<Boolean>>();
        dataList.add(new GaugeData<Boolean>(false));
        dataList.add(new GaugeData<Boolean>(false));

        GaugeData<Boolean> gd = mf.merge(dataList);
        assertFalse(gd.getValue());
    }

}
