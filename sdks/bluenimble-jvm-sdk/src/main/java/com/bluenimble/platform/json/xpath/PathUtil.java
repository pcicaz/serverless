/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluenimble.platform.json.xpath;

import java.util.LinkedList;
import java.util.List;

/**
 * User: kalle stenflo
 * Date: 2/2/11
 * Time: 2:08 PM
 */
public class PathUtil {

    public static boolean isPathDefinite(String jsonPath) {
        //return !jsonPath.replaceAll("\"[^\"\\\\\\n\r]*\"", "").matches(".*(\\.\\.|\\*|\\[[\\\\/]|\\?|,|:|>|\\(|<|=|\\+).*");
        return !jsonPath.replaceAll("\"[^\"\\\\\\n\r]*\"", "").matches(".*(\\.\\.|\\*|\\[[\\\\/]|\\?|,|:\\s?\\]|\\[\\s?:|>|\\(|<|=|\\+).*");
    }

    public static List<String> splitPath(String jsonPath) {

        LinkedList<String> fragments = new LinkedList<String>();

        if (!jsonPath.startsWith("$.")) {
            jsonPath = "$." + jsonPath;
        }

        jsonPath = jsonPath.replace("$['","$.['")
                .replaceAll("\\['(\\w+)\\.(\\w+)']", "['$1~$2']")
                .replace("']['","'].['" )
                .replace("..", ".~~.")
                .replace("[", ".[")
                .replace("@.", "@")
                .replace("['", "")
                .replace("']", "");

        String[] split = jsonPath.split("\\.");

        for (int i = 0; i < split.length; i++) {
            if (split[i].trim().isEmpty()) {
                continue;
            }
            fragments.add(split[i].replace("@", "@.").replace("~", "."));
        }

        //for (String fragment : fragments) {
        //    System.out.println(fragment);
        //}

        return fragments;
    }
}
