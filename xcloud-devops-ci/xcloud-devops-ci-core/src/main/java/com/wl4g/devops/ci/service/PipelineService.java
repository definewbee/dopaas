/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
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
package com.wl4g.devops.ci.service;

import com.wl4g.components.core.bean.ci.ClusterExtension;
import com.wl4g.components.core.bean.ci.PipeStepBuilding;
import com.wl4g.components.core.bean.ci.Pipeline;
import com.wl4g.devops.page.PageModel;

import java.util.List;

/**
 * @author vjay
 * @date 2020-04-27 15:06:00
 */
public interface PipelineService {

	PageModel list(PageModel pm, String pipeName, String providerKind, String environment);

	void save(Pipeline pipeline);

	Pipeline detail(Integer id);

	void del(Integer id);

	List<Pipeline> getByClusterId(Integer clusterId);

	PipeStepBuilding getPipeStepBuilding(Integer clusterId, Integer pipeId, Integer refType);

	List<Pipeline> getForSelect(String environment);

	PageModel clusterExtensionList(PageModel pm, String clusterName);

	void saveClusterExtension(ClusterExtension clusterExtension);

}