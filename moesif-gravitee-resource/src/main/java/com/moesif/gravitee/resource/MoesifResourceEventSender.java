/*
 * Copyright © 2024 Moesif (https://moesif.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.moesif.gravitee.resource;

import com.moesif.api.MoesifAPIClient;
import com.moesif.api.models.EventModel;
import com.moesif.gravitee.resource.api.MoesifResource;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoesifResourceEventSender extends MoesifResource<MoesifResourceConfiguration> {

    private MoesifAPIClient client;
    private Timer timer;
    private BlockingQueue<EventModel> eventQueue;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        this.client = new MoesifAPIClient(configuration().getApiToken(), configuration().getBaseUrl());
        this.eventQueue = new LinkedBlockingQueue<>(configuration().getQueueSize());

        log.info("Moesif Resource started: api token: {}", configuration().getApiToken());

        // Start a timer task for processing and sending events
        timer = new Timer("MoesifResourceEventSenderTimer");
        timer.schedule(new BatchProcessor(), 0, configuration().getBatchWaitTime());
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (timer != null) {
            timer.cancel();
        }
        log.info("Moesif Resource stopped");
    }

    public void sendEvent(EventModel event) {
        try {
            eventQueue.put(event);
            log.info("Event queued for sending to Moesif: {}", event);
        } catch (InterruptedException e) {
            log.error("Failed to queue event", e);
        }
    }

    @Override
    public String name() {
        return "moesif-resource";
    }

    class BatchProcessor extends TimerTask {

        @Override
        public void run() {
            while (!eventQueue.isEmpty()) {
                List<EventModel> events = new ArrayList<>();
                eventQueue.drainTo(events, configuration().getBatchSize());

                if (!events.isEmpty()) {
                    try {
                        client.getAPI().createEventsBatch(events);
                        log.info("Sent {} events to Moesif", events.size());
                    } catch (Throwable e) {
                        log.error("Failed to send events to Moesif", e);
                    }
                } else {
                    break; // No more full batches available, wait for next timer tick
                }
            }
        }
    }
}
