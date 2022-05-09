/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.flow;

import io.gravitee.definition.model.flow.Flow;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BestMatchFlowSelector {

    private static final String PATH_PARAM_PREFIX = ":";
    private static final Pattern SEPARATOR_SPLITTER = Pattern.compile("/");

    /**
     * Filters the flows to get the one best matching the request.
     * <br/>
     * <strong>
     *     We assume the {@code List<Flow>} parameter is already filtered by the a previous resolver to be sure the Flows' path match the request according to Flows' operator.
     * </strong>
     * <br/>
     * For each part of the flow path (split by {@link #SEPARATOR_SPLITTER}), a score is attributed:
     * - 1 if the string strictly equals the same part of the request
     * - 0.5 if the part is a path parameter (starting with {@link #PATH_PARAM_PREFIX})
     * - else 0
     * <br/>
     * Then, for each flow, we compare the scores arrays to the current selected best matching flow, reading from left to right.
     * As soon as a score is greater, then the flow becomes the best match.
     * <br/>
     *
     * Here is an example with those flows configured:
     *     <ul>
     *         <li>/myPath/staticId</li>
     *         <li>/:id/staticId</li>
     *         <li>/myPath/:id</li>
     *     </ul>
     * <blockquote>
     *<table border="1">
     *     <col width="25%"/>
     *     <col width="50%"/>
     *     <col width="25%"/>
     *     <thead>
     *         <tr>
     *             <th scope="col">Request path</th>
     *             <th scope="col">Flow path <> score</th>
     *             <th scope="col">Selected path</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>/myPath/staticId</td>
     *             <td>
     *                 <p>- /myPath/staticId  <>  [1, 1]</p>
     *                 <p>- /:id/staticId  <>  [0.5, 1]</p>
     *                 <p>- /myPath/:id  <>  [1, 0.5]</p>
     *             </td>
     *             <td>/myPath/staticId</td>
     *         </tr>
     *         <tr>
     *             <td>/myPath/553</td>
     *             <td>
     *                 <p>- /myPath/staticId  <>  [1, 0]</p>
     *                 <p>- /:id/staticId  <>  [0.5, 0]</p>
     *                 <p>- /myPath/:id  <>  [1, 0.5]</p>
     *             </td>
     *             <td>/myPath/:id</td>
     *         </tr>
     *         <tr>
     *             <td>/random/staticId</td>
     *             <td>
     *                 <p>- /myPath/staticId  <>  [0, 1]</p>
     *                 <p>- /:id/staticId  <>  [0.5, 1]</p>
     *                 <p>- /myPath/:id  <>  [0, 0.5]</p>
     *             </td>
     *             <td>/:id/staticId</td>
     *         </tr>
     *     </tbody>
     * </table>
     * </blockquote>
     * @param flows the flows already filtered (by a previous resolver).
     * @param path the current execution request.
     *
     * @return a list containing the best matching flow.
     */
    public static Flow forPath(List<Flow> flows, String path) {
        // Do not process empty flows
        if (flows == null || flows.isEmpty()) {
            return null;
        }

        Flow selectedFlow = null;
        Float[] selectedFlowScore = null;

        for (Flow flow : flows) {
            String[] splits = splitPath(flow.getPath());
            final String[] pathSplits = splitPath(path);
            final Float[] scores = new Float[splits.length];

            for (int i = 0; i < splits.length; i++) {
                // First, compute a score foreach split
                if (i >= pathSplits.length || splits[i].equals(pathSplits[i])) {
                    scores[i] = 1.0f;
                } else if (splits[i].startsWith(PATH_PARAM_PREFIX)) {
                    scores[i] = 0.5f;
                } else {
                    scores[i] = 0f;
                }
                if (selectedFlow == null) {
                    selectedFlow = flow;
                    selectedFlowScore = scores;
                }

                // Then, if current splits array is longer than selected flow one, the current flow is selected as best
                if (i == selectedFlowScore.length) {
                    selectedFlow = flow;
                    selectedFlowScore = scores;
                }
                // Finally, if split score is fewer than selected, no need to continue, else we have a better matching, so we can select the flow
                if (scores[i] < selectedFlowScore[i]) {
                    break;
                } else if (scores[i] > selectedFlowScore[i]) {
                    selectedFlow = flow;
                    selectedFlowScore = scores;
                    break;
                }
            }
        }

        return selectedFlow;
    }

    /**
     * Split string with "/" character. We use an already compiled Pattern to avoid using {@link String#split(String)} which is compiling a new one for each call.
     * <br/>
     * Also, we choose a negative limit to split the string, to avoid discarding trailing empty string.
     * <br/>
     * For more information, you can see {@link Pattern#split(CharSequence, int)}.
     * <br/>
     * <quote>
     *     If the limit is negative then the pattern will be applied as many times as possible and the array can have any length.
     * </quote>
     * @param path to split
     * @return The array of strings computed by splitting the input path
     */
    private static String[] splitPath(String path) {
        return SEPARATOR_SPLITTER.split(path, -1);
    }
}
