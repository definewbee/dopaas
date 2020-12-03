// Generated by XCloud DevOps for Codegen, refer: http://dts.devops.wl4g.com

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

package com.wl4g.devops.doc.service;

import com.wl4g.components.core.bean.model.PageModel;
import com.wl4g.devops.common.bean.doc.EnterpriseTeam;

/**
 *  service of {@link EnterpriseTeam}
 *
 * @author root
 * @version master
 * @Date 
 * @since v1.0
 */
public interface EnterpriseTeamService {

    /**
     *  page query.
     *
     * @param pm
     * @param enterpriseTeam
     * @return 
     */
    PageModel<EnterpriseTeam> page(PageModel<EnterpriseTeam> pm, EnterpriseTeam enterpriseTeam);

    /**
     *  save.
     *
     * @param enterpriseTeam
     * @return 
     */
    int save(EnterpriseTeam enterpriseTeam);

    /**
     *  detail query.
     *
     * @param id
     * @return 
     */
    EnterpriseTeam detail(Long id);

    /**
     *  delete.
     *
     * @param id
     * @return 
     */
    int del(Long id);

}

