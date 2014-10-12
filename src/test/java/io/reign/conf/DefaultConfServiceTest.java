/*
 * Copyright 2013 Yen Pai ypai@reign.io
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.reign.conf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ypai
 * 
 */
public class DefaultConfServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConfServiceTest.class);

    private DefaultConfService confService;

    @Before
    public void setUp() throws Exception {
        confService = new DefaultConfService();
    }

    @Test
    public void testIsValidConfPath() {
        assertTrue(confService.isValidConfPath("this.properties"));
        assertTrue(confService.isValidConfPath("/conf/what/super/a.json"));
        assertTrue(confService.isValidConfPath("/conf/what/super/a.0.1.x.json"));
        assertTrue(confService.isValidConfPath("/conf/what/this.properties"));
        assertTrue(confService.isValidConfPath("/conf/what/super/this.properties"));

        assertFalse(confService.isValidConfPath("."));
        assertFalse(confService.isValidConfPath("/."));
        assertFalse(confService.isValidConfPath("a."));
        assertFalse(confService.isValidConfPath(".properties"));
        assertFalse(confService.isValidConfPath("/.properties"));
        assertFalse(confService.isValidConfPath("/conf/what/super/this.properties/"));
    }

    @Test
    public void testCastValueIfNecessary() {
        Object value;

        value = confService.castValueIfNecessary("(int)999");
        assertTrue(value.equals(999) && value instanceof Integer);

        value = confService.castValueIfNecessary("(long)9999999999");
        assertTrue(value.equals(9999999999L) && value instanceof Long);

        value = confService.castValueIfNecessary("(double)9.99");
        assertTrue(value.equals(9.99) && value instanceof Double);

        value = confService.castValueIfNecessary("(float)99.9");
        assertTrue(value.equals(99.9F) && value instanceof Float);

        value = confService.castValueIfNecessary("(short)9");
        assertTrue(value.equals((short) 9) && value instanceof Short);

        value = confService.castValueIfNecessary("(byte)1");
        assertTrue(value.equals((byte) 1) && value instanceof Byte);
    }
}
