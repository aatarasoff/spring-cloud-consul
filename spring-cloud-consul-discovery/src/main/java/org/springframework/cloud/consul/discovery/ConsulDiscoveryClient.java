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

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.agent.model.Self;
import com.ecwid.consul.v1.agent.model.Service;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.cloud.consul.discovery.IpAddressUtils.getCatalogServiceHost;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class ConsulDiscoveryClient implements DiscoveryClient {

	private ConsulLifecycle lifecycle;

	private ConsulClient client;

	private ConsulDiscoveryProperties properties;
	private ServerProperties serverProperties;

	public ConsulDiscoveryClient(ConsulClient client, ConsulLifecycle lifecycle,
								 ConsulDiscoveryProperties properties,
								 ServerProperties serverProperties) {
		this.client = client;
		this.lifecycle = lifecycle;
		this.properties = properties;
		this.serverProperties = serverProperties;
	}

	@Override
	public String description() {
		return "Spring Cloud Consul Discovery Client";
	}

	@Override
	public ServiceInstance getLocalServiceInstance() {
		Response<Map<String, Service>> agentServices = client.getAgentServices();
		Service service = agentServices.getValue().get(lifecycle.getServiceId());
		String serviceId;
		Integer port;
		if (service == null) {
			//possibly called before registration
			log.warn("Unable to locate service in consul agent: "
					+ lifecycle.getServiceId());

			serviceId = lifecycle.getServiceId();
			port = lifecycle.getConfiguredPort();
			if (port == 0 && serverProperties.getPort() != null) {
				port = serverProperties.getPort();
			}
		} else {
			serviceId = service.getId();
			port = service.getPort();
		}
		String host = "localhost";
		Response<Self> agentSelf = client.getAgentSelf();
		Member member = agentSelf.getValue().getMember();
		if (member != null) {
			if (properties.isPreferIpAddress()) {
				host = member.getAddress();
			} else if (StringUtils.hasText(member.getName())) {
				host = member.getName();
			}
		}
		return new DefaultServiceInstance(serviceId, host, port, false);
	}

	@Override
	public List<ServiceInstance> getInstances(final String serviceId) {
		List<ServiceInstance> instances = new ArrayList<>();

		addInstancesToList(instances, serviceId);

		return instances;
	}

	private void addInstancesToList(List<ServiceInstance> instances, String serviceId) {
		List<CatalogService> services = client.getCatalogService(serviceId,
				QueryParams.DEFAULT).getValue();

		final Set<String> filteredTags = properties.getFilter().getTags();

		if (!filteredTags.isEmpty()) {
			services = FluentIterable.from(services).filter(new Predicate<CatalogService>() {
				@Override
				public boolean apply(CatalogService catalogService) {
					return FluentIterable.from(catalogService.getServiceTags()).anyMatch(new Predicate<String>() {
						@Override
						public boolean apply(String tag) {
							return filteredTags.contains(tag);
						}
					});
				}
			}).toList();
		}

		for (CatalogService service : services) {
			String host = getCatalogServiceHost(service);
			instances.add(new DefaultServiceInstance(serviceId, host,
					service.getServicePort(), false
			));
		}
	}

	@Override
	public List<String> getServices() {
		return new ArrayList<>(client.getCatalogServices(QueryParams.DEFAULT).getValue()
				.keySet());
	}
}
