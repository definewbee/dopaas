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
package com.wl4g.dopaas.umc.watch.config;

import static com.dangdang.ddframe.job.config.JobCoreConfiguration.*;
import static com.dangdang.ddframe.job.lite.config.LiteJobConfiguration.*;

import javax.sql.DataSource;

import org.apache.shardingsphere.elasticjob.dataflow.job.DataflowJob;
import org.apache.shardingsphere.elasticjob.lite.internal.schedule.JobScheduler;
import org.apache.shardingsphere.elasticjob.reg.zookeeper.ZookeeperConfiguration;
import org.apache.shardingsphere.elasticjob.reg.zookeeper.ZookeeperRegistryCenter;
import org.apache.shardingsphere.elasticjob.simple.job.SimpleJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.wl4g.component.common.annotation.Reserved;
import com.wl4g.dopaas.umc.watch.ServiceIndicatorsStateWatcher;
import com.wl4g.dopaas.umc.watch.WatchJobListener;
import com.wl4g.dopaas.umc.watch.WatchScheduler;
import com.wl4g.dopaas.umc.watch.fetch.IndicatorsMetaFetcher;
import com.wl4g.dopaas.umc.watch.fetch.IndicatorsMetaInfo;
import com.wl4g.dopaas.umc.watch.fetch.ServiceIndicatorsMetaFetcher;

/**
 * UMC watching auto configuration.
 * 
 * @author wangl.sir
 * @version v1.0 2019年7月4日
 * @since
 */
public class UmcWatchAutoConfiguration {
	public static final String KEY_WATCH_PREFIX = "spring.cloud.devops.umc.watch";

	@Bean
	@ConfigurationProperties(prefix = KEY_WATCH_PREFIX)
	public WatchProperties watchProperties() {
		return new WatchProperties();
	}

	//
	// Elastic job
	//

	@Bean(initMethod = "init")
	public ZookeeperRegistryCenter regCenter(WatchProperties config) {
		return new ZookeeperRegistryCenter(new ZookeeperConfiguration(config.getZkServers(), config.getNamespace()));
	}

	@Bean
	public ServiceIndicatorsStateWatcher applicationIndicatorsStateWatcher() {
		return new ServiceIndicatorsStateWatcher();
	}

	@Bean
	public WatchJobListener watchJobListener() {
		return new WatchJobListener();
	}

	@Bean
	public JobEventConfiguration jobEventConfiguration(DataSource dataSource) {
		return new JobEventRdbConfiguration(dataSource);
	}

	@Bean(initMethod = "init")
	public JobScheduler watchScheduler(WatchProperties config, JobEventConfiguration eventConfig,
			ServiceIndicatorsStateWatcher job, ZookeeperRegistryCenter regCenter) {
		LiteJobConfiguration jobConfig = getDataflowLiteJobConfiguration(job.getClass(), config.getCron(), config.getTotalCount(),
				config.getItemParams());
		return new WatchScheduler(job, regCenter, jobConfig, eventConfig, watchJobListener());
	}

	@Reserved
	private LiteJobConfiguration getLiteJobConfiguration(final Class<? extends SimpleJob> jobClass, final String cron,
			final int shardingTotalCount, final String shardingItemParameters) {
		return LiteJobConfiguration
				.newBuilder(new SimpleJobConfiguration(newBuilder(jobClass.getName(), cron, shardingTotalCount)
						.shardingItemParameters(shardingItemParameters).build(), jobClass.getCanonicalName()))
				.overwrite(true).build();
	}

	private LiteJobConfiguration getDataflowLiteJobConfiguration(Class<? extends DataflowJob<IndicatorsMetaInfo>> jobClass,
			String cron, int shardingTotalCount, String shardingItemParameters) {
		return newBuilder(new DataflowJobConfiguration(
				newBuilder(jobClass.getName(), cron, shardingTotalCount).shardingItemParameters(shardingItemParameters).build(),
				jobClass.getCanonicalName(), true)).overwrite(true).build();
	}

	//
	// Fetcher
	//

	@Bean
	@ConditionalOnMissingBean(IndicatorsMetaFetcher.class)
	public IndicatorsMetaFetcher serviceIndicatorsMetaFetcher() {
		return new ServiceIndicatorsMetaFetcher();
	}

}