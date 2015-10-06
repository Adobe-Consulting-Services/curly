/* 
 * Copyright 2015 Adobe.
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
package com.adobe.ags.curly.controller;

import com.adobe.ags.curly.CurlyApp;
import com.adobe.ags.curly.model.Action;
import com.adobe.ags.curly.model.BatchRunnerResult;
import com.adobe.ags.curly.model.RunnerResult;
import com.adobe.ags.curly.model.TaskRunner;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import org.apache.http.impl.client.CloseableHttpClient;

public class BatchRunner implements TaskRunner {
    BatchRunnerResult result;
    BlockingQueue<Runnable> tasks;
    ThreadPoolExecutor executor;
    int concurrency;
    ThreadLocal<CloseableHttpClient> clientThread;

    public BatchRunner(AuthHandler auth, int concurrency, List<Action> actions, List<Map<String, String>> batchData, Map<String, StringProperty> defaultValues, Set<String> displayColumns) {
        clientThread = ThreadLocal.withInitial(auth::getAuthenticatedClient);
        result = new BatchRunnerResult();
        tasks = new ArrayBlockingQueue<>(batchData.size());
        this.concurrency = concurrency;
        defaultValues.put("server", new ReadOnlyStringWrapper(auth.getUrlBase()));
        buildTasks(actions, batchData, defaultValues, displayColumns);
    }
    
    @Override
    public RunnerResult getResult() {
        return result;
    }

    @Override
    public void run() {
        try {
            CurlyApp.getInstance().runningProperty().set(true);
            executor = new ThreadPoolExecutor(concurrency, concurrency, 1, TimeUnit.DAYS, tasks);
            result.start();
            executor.execute(()->{
                result.stop();
            });
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.getLogger(BatchRunner.class.getName()).log(Level.SEVERE, null, ex);
            if (!executor.isShutdown()) {
                executor.getQueue().clear();
            }
        }
        result.stop();
    }

    private void buildTasks(List<Action> actions, List<Map<String, String>> batchData, Map<String, StringProperty> defaultValues, Set<String> displayColumns) {
        int row = 0;
        for (Map<String,String> data : batchData) {
            row++;
            try {
                Map<String,String> values = new HashMap<>(data);
                defaultValues.forEach((key,value)-> {
                    if (values.get(key) == null || values.get(key).isEmpty()) {
                        values.put(key,value.get());
                    }
                });
                ActionGroupRunner runner = new ActionGroupRunner("Row "+row,clientThread::get, actions, values, displayColumns);
                result.addDetail(runner.results);
                tasks.add(runner);
            } catch (ParseException ex) {
                Logger.getLogger(BatchRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
