/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.samza.logging.log4j;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.samza.config.Config;
import org.apache.samza.config.MapConfig;
import org.apache.samza.logging.log4j.serializers.LoggingEventJsonSerde;
import org.apache.samza.logging.log4j.serializers.LoggingEventStringSerde;
import org.apache.samza.logging.log4j.serializers.LoggingEventStringSerdeFactory;
import org.junit.Test;

public class TestStreamAppender {

  static Logger log = Logger.getLogger(TestStreamAppender.class);

  @Test
  public void testDefaultSerde() {
    System.setProperty("samza.container.name", "samza-container-1");
    MockSystemProducerAppender systemProducerAppender = new MockSystemProducerAppender();
    PatternLayout layout = new PatternLayout();
    layout.setConversionPattern("%m");
    systemProducerAppender.setLayout(layout);
    systemProducerAppender.activateOptions();
    assertNotNull(systemProducerAppender.getSerde());
    assertEquals(LoggingEventJsonSerde.class, systemProducerAppender.getSerde().getClass());
  }

  @Test
  public void testNonDefaultSerde() {
    System.setProperty("samza.container.name", "samza-container-1");
    String streamName = StreamAppender.getStreamName("log4jTest", "1");
    Map<String, String> map = new HashMap<String, String>();
    map.put("job.name", "log4jTest");
    map.put("job.id", "1");
    map.put("serializers.registry.log4j-string.class", LoggingEventStringSerdeFactory.class.getCanonicalName());
    map.put("systems.mock.samza.factory", MockSystemFactory.class.getCanonicalName());
    map.put("systems.mock.streams." + streamName + ".samza.msg.serde", "log4j-string");
    MockSystemProducerAppender systemProducerAppender = new MockSystemProducerAppender(new MapConfig(map));
    PatternLayout layout = new PatternLayout();
    layout.setConversionPattern("%m");
    systemProducerAppender.setLayout(layout);
    systemProducerAppender.activateOptions();
    assertNotNull(systemProducerAppender.getSerde());
    assertEquals(LoggingEventStringSerde.class, systemProducerAppender.getSerde().getClass());
  }

  @Test
  public void testSystemProducerAppender() {
    System.setProperty("samza.container.name", "samza-container-1");

    MockSystemProducerAppender systemProducerAppender = new MockSystemProducerAppender();
    PatternLayout layout = new PatternLayout();
    layout.setConversionPattern("%m");
    systemProducerAppender.setLayout(layout);
    systemProducerAppender.activateOptions();
    log.addAppender(systemProducerAppender);
    log.info("testing");
    log.info("testing2");

    systemProducerAppender.flushSystemProducer();

    assertEquals(2, MockSystemProducer.messagesReceived.size());
    assertTrue(new String((byte[]) MockSystemProducer.messagesReceived.get(0)).contains("\"message\":\"testing\""));
    assertTrue(new String((byte[]) MockSystemProducer.messagesReceived.get(1)).contains("\"message\":\"testing2\""));
  }

  /**
   * a mock class which overrides the getConfig method in SystemProducerAppener
   * for testing purpose. Because the environment variable where the config
   * stays is difficult to test.
   */
  class MockSystemProducerAppender extends StreamAppender {
    private final Config config;

    public MockSystemProducerAppender() {
      Map<String, String> map = new HashMap<String, String>();
      map.put("job.name", "log4jTest");
      map.put("systems.mock.samza.factory", MockSystemFactory.class.getCanonicalName());
      config = new MapConfig(map);
    }

    public MockSystemProducerAppender(Config config) {
      this.config = config;
    }

    @Override
    protected Config getConfig() {
      return config;
    }
  }
}