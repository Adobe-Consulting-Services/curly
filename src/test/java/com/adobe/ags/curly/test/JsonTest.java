/*
 * Copyright 2016 Adobe Global Services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.ags.curly.test;

import com.adobe.ags.curly.xml.Action;
import com.adobe.ags.curly.xml.ResultType;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brobert
 */
public class JsonTest {
    
    public JsonTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    public static String enumTest1 = "{\n" +
"        \"name\": \"Get references for image\",\n" +
"        \"description\": \"Find all page that refer to a given image/asset in a jcr path.\",\n" +
"        \"command\": \"-X GET ${server}/bin/querybuilder.json?path=${site|/content/mySite}&1_property=fileReference&1_property.value=${image|/content/dam/myImage}&p.limit=${limit|-1}\",\n" +
"        \"resultType\":\"PLAIN\"\n" +
"    }";
    
    @Test
    public void resultTypeEnumeration() {
        Gson gson = new Gson();
        Action test1 = gson.fromJson(enumTest1, Action.class);
        assertNotNull(test1);
        assertEquals(ResultType.PLAIN, test1.getResultType());
    }
}
