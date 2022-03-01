/*
 * Copyright 2017 ~ 2050 the original author or authors <Wanglsir@gmail.com, 983708408@qq.com>.
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
package com.wl4g.dopaas.ucm.config;

import static com.wl4g.dopaas.ucm.common.UCMConstants.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.wl4g.infra.common.crypto.asymmetric.RSACryptor;
import com.wl4g.infra.common.crypto.symmetric.AES128ECBPKCS5;
import com.wl4g.infra.core.web.method.PrefixHandlerMappingSupport;
import com.wl4g.infra.core.config.mapping.PrefixHandlerMapping;
import com.wl4g.infra.support.cache.jedis.JedisService;
import com.wl4g.dopaas.ucm.annotation.UcmEndpoint;
import com.wl4g.dopaas.ucm.endpoint.UcmServerEndpoint;
import com.wl4g.dopaas.ucm.handler.CentralConfigServerHandler;
import com.wl4g.dopaas.ucm.handler.CheckCentralConfigServerHandler;
import com.wl4g.dopaas.ucm.publish.ConfigSourcePublisher;
import com.wl4g.dopaas.ucm.publish.JedisConfigSourcePublisher;
//import com.wl4g.dopaas.ucm.session.ConfigServerSecurityManager;

/**
 * UCM auto configuration
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年5月27日
 * @since
 */
public class UcmAutoConfiguration extends AbstractHandlerMappingSupport {

	@Bean
	@ConfigurationProperties(prefix = "spring.cloud.devops.ucm")
	public UcmProperties ucmProperties() {
		return new UcmProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public CentralConfigServerHandler configContextHandler() {
		return new CheckCentralConfigServerHandler();
	}

	@Bean
	public ConfigSourcePublisher configSourcePublisher(JedisService jedisService) {
		return new JedisConfigSourcePublisher(ucmProperties(), jedisService);
	}

	@Bean(BEAN_MVC_EXECUTOR)
	public ThreadPoolTaskExecutor mvcTaskExecutor(UcmProperties config) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(config.getCorePoolSize());
		executor.setQueueCapacity(config.getQueueCapacity());
		executor.setMaxPoolSize(config.getMaxPoolSize());
		return executor;
	}

	@Bean
	public UcmWebMvcConfigurer ucmWebMvcConfigurer(UcmProperties config,
			@Qualifier(BEAN_MVC_EXECUTOR) ThreadPoolTaskExecutor executor) {
		return new UcmWebMvcConfigurer(config, executor);
	}

	// @Bean
	// public ConfigServerSecurityManager ucmServerConfigSecurityManager() {
	// return new ConfigServerSecurityManager(new RSACryptor(), new
	// AES128ECBPKCS5());
	// }

	//
	// --- Endpoint's. ---
	//

	@Bean
	public UcmServerEndpoint ucmServerEnndpoint() {
		return new UcmServerEndpoint();
	}

	@Bean
	public PrefixHandlerMapping ucmServerEndpointPrefixHandlerMapping() {
		return super.newPrefixHandlerMapping(URI_S_BASE, UcmEndpoint.class);
	}

	final public static String BEAN_MVC_EXECUTOR = "mvcTaskExecutor";

}