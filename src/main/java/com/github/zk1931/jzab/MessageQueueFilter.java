/**
 * Licensed to the zk1931 under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.  You may obtain a copy of the
 * License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.zk1931.jzab;

import com.github.zk1931.jzab.Participant.LeftCluster;
import com.github.zk1931.jzab.proto.ZabMessage.Message.MessageType;
import com.google.protobuf.TextFormat;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The abstract MessageQueueFilter class. Used to preprocess certain messages
 * in queue.
 */
class MessageQueueFilter {

  private final BlockingQueue<MessageTuple> messageQueue;

  private static final Logger LOG =
    LoggerFactory.getLogger(MessageQueueFilter.class);

  MessageQueueFilter(BlockingQueue<MessageTuple> messageQueue) {
    this.messageQueue = messageQueue;
  }

  protected MessageTuple getMessage(int timeoutMs)
      throws InterruptedException, TimeoutException {
    MessageTuple tuple = messageQueue.poll(timeoutMs,
                                           TimeUnit.MILLISECONDS);
    if (tuple == null) {
      throw new TimeoutException("Timeout while waiting for the message.");
    }
    if (tuple.getMessage().getType() == MessageType.SHUT_DOWN) {
      // If it's SHUT_DOWN message.
      throw new LeftCluster("Left cluster");
    }
    return tuple;
  }

  protected MessageTuple getExpectedMessage(MessageType type,
                                            String source,
                                            int timeoutMs)
      throws TimeoutException, InterruptedException {
    int startTime = (int) (System.nanoTime() / 1000000);
    // Waits until the expected message is received.
    while (true) {
      MessageTuple tuple = getMessage(timeoutMs);
      String from = tuple.getServerId();
      if (tuple.getMessage().getType() == type &&
          (source == null || source.equals(from))) {
        // Return message only if it's expected type and expected source.
        return tuple;
      } else {
        int curTime = (int) (System.nanoTime() / 1000000);
        if (curTime - startTime >= timeoutMs) {
          throw new TimeoutException("Timeout in getExpectedMessage.");
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Got an unexpected message from {}: {}",
                    tuple.getServerId(),
                    TextFormat.shortDebugString(tuple.getMessage()));
        }
      }
    }
  }
}
