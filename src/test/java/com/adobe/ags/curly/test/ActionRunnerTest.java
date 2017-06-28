/*
 * Copyright 2017 Adobe Global Services.
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

import com.adobe.ags.curly.controller.ActionRunner;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brobert
 */
public class ActionRunnerTest {
    @Test
    public void parseTest() {
        String testStr = "-Fjcr:primaryType=sling:OrderedFolder -Fjcr:title=\"${1} ${0}\" ${2}/content/dam/test/${1}";
        List<String> result = ActionRunner.splitByUnquotedSpaces(testStr);
        String[] resultArray = result.toArray(new String[0]);
        System.out.println("Parse Result --> ");
        result.forEach(p->System.out.println(">>"+p));
        assertArrayEquals(new String[]{
            "-Fjcr:primaryType=sling:OrderedFolder",
            "-Fjcr:title=${1} ${0}",
            "${2}/content/dam/test/${1}"
        },resultArray);
    }
}
