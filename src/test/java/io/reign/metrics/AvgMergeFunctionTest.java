package io.reign.metrics;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * 
 * @author ypai
 *
 */
public class AvgMergeFunctionTest {

    @Test(expected = IllegalStateException.class)
    public void testNullDataList() throws Exception {
        AvgMergeFunction<Long> mf = new AvgMergeFunction<Long>();
        mf.merge(null);

    }

    @Test(expected = IllegalStateException.class)
    public void testEmptyDataList() throws Exception {
        AvgMergeFunction<Long> mf = new AvgMergeFunction<Long>();
        mf.merge(new ArrayList<GaugeData<Long>>());
    }

    @Test
    public void testByte() throws Exception {
        AvgMergeFunction<Byte> mf = new AvgMergeFunction<Byte>();

        List<GaugeData<Byte>> dataList = new ArrayList<GaugeData<Byte>>();
        dataList.add(new GaugeData<Byte>((byte) 1));
        dataList.add(new GaugeData<Byte>((byte) 2));
        dataList.add(new GaugeData<Byte>((byte) 3));

        GaugeData<Byte> result = mf.merge(dataList);
        assertTrue(result.getValue() == (short) 2);
    }

    @Test
    public void testShort() throws Exception {
        AvgMergeFunction<Short> mf = new AvgMergeFunction<Short>();

        List<GaugeData<Short>> dataList = new ArrayList<GaugeData<Short>>();
        dataList.add(new GaugeData<Short>((short) 1));
        dataList.add(new GaugeData<Short>((short) 2));
        dataList.add(new GaugeData<Short>((short) 3));

        GaugeData<Short> result = mf.merge(dataList);
        assertTrue(result.getValue() == (short) 2);
    }

    @Test
    public void testInteger() throws Exception {
        AvgMergeFunction<Integer> mf = new AvgMergeFunction<Integer>();

        List<GaugeData<Integer>> dataList = new ArrayList<GaugeData<Integer>>();
        dataList.add(new GaugeData<Integer>(1));
        dataList.add(new GaugeData<Integer>(2));
        dataList.add(new GaugeData<Integer>(3));

        GaugeData<Integer> result = mf.merge(dataList);
        assertTrue(result.getValue() == 2);
    }

    @Test
    public void testLong() throws Exception {
        AvgMergeFunction<Long> mf = new AvgMergeFunction<Long>();

        List<GaugeData<Long>> dataList = new ArrayList<GaugeData<Long>>();
        dataList.add(new GaugeData<Long>(1L));
        dataList.add(new GaugeData<Long>(2L));
        dataList.add(new GaugeData<Long>(3L));

        GaugeData<Long> result = mf.merge(dataList);
        assertTrue(result.getValue() == 2L);
    }

    @Test
    public void testDouble() throws Exception {
        AvgMergeFunction<Double> mf = new AvgMergeFunction<Double>();

        List<GaugeData<Double>> dataList = new ArrayList<GaugeData<Double>>();
        dataList.add(new GaugeData<Double>(1.0));
        dataList.add(new GaugeData<Double>(2.0));
        dataList.add(new GaugeData<Double>(3.0));

        GaugeData<Double> result = mf.merge(dataList);
        assertTrue(result.getValue() == 2.0);
    }

    @Test
    public void testFloat() throws Exception {
        AvgMergeFunction<Float> mf = new AvgMergeFunction<Float>();

        List<GaugeData<Float>> dataList = new ArrayList<GaugeData<Float>>();
        dataList.add(new GaugeData<Float>(1.0f));
        dataList.add(new GaugeData<Float>(2.0f));
        dataList.add(new GaugeData<Float>(3.0f));

        GaugeData<Float> result = mf.merge(dataList);
        assertTrue(result.getValue() == 2.0f);
    }

}
