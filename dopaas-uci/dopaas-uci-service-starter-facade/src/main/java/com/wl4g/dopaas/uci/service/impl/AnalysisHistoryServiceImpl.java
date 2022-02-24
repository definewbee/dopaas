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
package com.wl4g.dopaas.uci.service.impl;

import com.wl4g.infra.core.page.PageHolder;
import com.wl4g.dopaas.uci.data.AnalysisHistoryDao;
import com.wl4g.dopaas.uci.service.AnalysisHistoryService;
import com.wl4g.dopaas.common.bean.uci.AnalysisHistory;

import static com.wl4g.iam.common.utils.IamOrganizationUtils.getRequestOrganizationCodes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author vjay
 * @date 2019-12-16 16:13:00
 */
@Service
public class AnalysisHistoryServiceImpl implements AnalysisHistoryService {

	private @Autowired AnalysisHistoryDao analysisHistoryDao;

	@Override
	public PageHolder<AnalysisHistory> list(PageHolder<AnalysisHistory> pm) {
		pm.useCount().bind();
		pm.setRecords(analysisHistoryDao.list(getRequestOrganizationCodes()));
		return pm;
	}

}