/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.net;

import org.apache.cassandra.testing.TestingClient;
import org.apache.cassandra.utils.FBUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDeliveryTask implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(MessageDeliveryTask.class);

    private final MessageIn message;
    private final long constructionTime;
    private final int id;

    public MessageDeliveryTask(MessageIn message, int id, long timestamp)
    {
        assert message != null;
        this.message = message;
        this.id = id;
        constructionTime = timestamp;
    }

    public void run()
    {
        MessagingService.Verb verb = message.verb;
        if (MessagingService.DROPPABLE_VERBS.contains(verb)
            && System.currentTimeMillis() > constructionTime + message.getTimeout())
        {
            MessagingService.instance().incrementDroppedMessages(verb);
            return;
        }

        IVerbHandler verbHandler = MessagingService.instance().getVerbHandler(verb);
        if (verbHandler == null)
        {
            logger.debug("Unknown verb {}", verb);
            return;
        }

        verbHandler.doVerb(message, id);
        if (message.verb == MessagingService.Verb.PAXOS_PREPARE
            || message.verb == MessagingService.Verb.PAXOS_PROPOSE
            || message.verb == MessagingService.Verb.PAXOS_COMMIT
            || message.verb == MessagingService.Verb.PAXOS_PREPARE_RESPONSE
            || message.verb == MessagingService.Verb.PAXOS_PROPOSE_RESPONSE
            || message.verb == MessagingService.Verb.PAXOS_COMMIT_RESPONSE) {
            // BRC - Send ACK to server that the message is processed
            String destAddr = FBUtilities.getBroadcastAddress().getHostAddress();
            int recv = Integer.parseInt(destAddr.substring(destAddr.length() - 1)) - 1;
            TestingClient.getInstance().writeToSocket(message, recv, FBUtilities.getBroadcastAddress());
        }
    }
}
