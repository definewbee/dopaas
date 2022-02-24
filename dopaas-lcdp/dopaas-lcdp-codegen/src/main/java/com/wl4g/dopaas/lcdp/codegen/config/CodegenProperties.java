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
package com.wl4g.dopaas.lcdp.codegen.config;

import static com.wl4g.infra.common.lang.Assert2.hasTextOf;
import static com.wl4g.infra.common.log.SmartLoggerFactory.getLogger;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.SystemUtils.USER_HOME;
import static org.apache.commons.lang3.SystemUtils.USER_NAME;

import java.io.File;

import org.springframework.beans.factory.InitializingBean;

import com.wl4g.infra.common.bridge.RpcContextIamSecurityBridges;
import com.wl4g.infra.common.log.SmartLogger;

/**
 * {@link CodegenProperties}
 *
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2020-09-10
 * @since
 */
public class CodegenProperties implements InitializingBean {

	protected final SmartLogger log = getLogger(getClass());

	/**
	 * Global workspace directory path.
	 */
	private String workspace = USER_HOME + File.separator + ".codegen-workspace"; // By-default.

	/**
	 * Is it allowed to override the key of the data model.
	 */
	private boolean allowRenderingCustomizeModelOverride = false;

	/**
	 * Generator watermark content string.
	 */
	// private String watermarkContent = "Generated by DoPaaS for
	// Codegen, refer:
	// https://github.com/wl4g/dopaas/tree/master/dopaas-dts or
	// http://dts.devops.wl4g.com";
	private String watermarkContent = "Generated by DoPaaS for Codegen, refer: http://dts.devops.wl4g.com";

	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String workspace) {
		this.workspace = hasTextOf(workspace, "workspace");
	}

	public boolean isAllowRenderingCustomizeModelOverride() {
		return allowRenderingCustomizeModelOverride;
	}

	public void setAllowRenderingCustomizeModelOverride(boolean allowRenderingCustomizeModelOverride) {
		this.allowRenderingCustomizeModelOverride = allowRenderingCustomizeModelOverride;
	}

	public String getWatermarkContent() {
		return watermarkContent;
	}

	public void setWatermarkContent(String watermarkContent) {
		this.watermarkContent = hasTextOf(watermarkContent, "watermarkContent");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		applyDefaultProperties();
	}

	/**
	 * Apply default properties values.
	 */
	protected void applyDefaultProperties() {

	}

	//
	// Function's.
	//

	/**
	 * e.g: job.root-1000001-1599726930000
	 * 
	 * @param genProjectId
	 * @return
	 */
	public String generateJobId(String genProjectId) {
		hasTextOf(genProjectId, "genProjectId");
		String principalName = USER_NAME;
		try {
			principalName = RpcContextIamSecurityBridges.currentIamPrincipalName();
		} catch (Exception e) {
		}
		return "job.".concat(principalName).concat("-").concat(genProjectId).concat("-").concat(valueOf(currentTimeMillis()));
	}

	/**
	 * e.g. </br>
	 * ~/.codegen-workspace/jobs/job.root-1000001-1599726930000/
	 * 
	 * @param genJobId
	 * @return
	 */
	public File generateJobDir(String genJobId) {
		hasTextOf(genJobId, "genJobId");
		return new File(getWorkspace().concat("/").concat(DEFUALT_JOB_BASEDIR).concat(genJobId));
	}

	final public static String DEFUALT_JOB_BASEDIR = "jobs";

}