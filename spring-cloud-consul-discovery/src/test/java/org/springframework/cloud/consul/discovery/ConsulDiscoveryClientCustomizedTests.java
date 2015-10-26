/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.consul.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ConsulDiscoveryClientCustomizedTests.MyTestConfig.class)
@WebIntegrationTest(value = {"spring.application.name=testConsulDiscovery2", "spring.cloud.consul.discovery.instanceId=testConsulDiscovery2Id"}, randomPort = true)
public class ConsulDiscoveryClientCustomizedTests {

	@Autowired
	private ConsulDiscoveryClient discoveryClient;

	@Test
	public void getInstancesForServiceWorks() {
		List<ServiceInstance> instances = discoveryClient.getInstances("consul");
		assertNotNull("instances was null", instances);
		assertFalse("instances was empty", instances.isEmpty());

		ServiceInstance instance = instances.get(0);
		assertNotIpAddress(instance);
	}

	private void assertNotIpAddress(ServiceInstance instance) {
		assertFalse("host is an ip address", InetAddressUtils.isIPv4Address(instance.getHost()));
	}

	@Test
	public void getLocalInstance() {
		ServiceInstance instance = discoveryClient.getLocalServiceInstance();
		assertNotNull("instance was null", instance);
		assertNotIpAddress(instance);
		assertEquals("instance id was wrong", "testConsulDiscovery2Id", instance.getServiceId());
	}

	@Configuration
	@EnableDiscoveryClient
	@EnableAutoConfiguration
	@Import({ ConsulAutoConfiguration.class, ConsulDiscoveryClientConfiguration.class })
	public static class MyTestConfig {

	}
}
