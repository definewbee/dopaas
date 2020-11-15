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
package com.wl4g.devops.erm.service.impl;

import com.github.pagehelper.PageHelper;
import com.wl4g.components.core.bean.BaseBean;
import com.wl4g.components.data.page.PageModel;
import com.wl4g.components.support.cli.DestroableProcessManager;
import com.wl4g.components.support.cli.command.RemoteDestroableCommand;
import com.wl4g.devops.common.bean.erm.Host;
import com.wl4g.devops.common.bean.erm.SshBean;
import com.wl4g.devops.erm.dao.HostDao;
import com.wl4g.devops.erm.dao.SshDao;
import com.wl4g.devops.erm.service.SshService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.wl4g.devops.erm.util.SshkeyUtils.decryptSshkeyFromHex;
import static com.wl4g.devops.erm.util.SshkeyUtils.encryptSshkeyToHex;
import static com.wl4g.iam.core.utils.IamOrganizationHolder.getRequestOrganizationCode;
import static com.wl4g.iam.core.utils.IamOrganizationHolder.getRequestOrganizationCodes;
import static java.util.Objects.isNull;

/**
 * @author vjay
 * @date 2019-11-14 14:10:00
 */
@Service
public class SshServiceImpl implements SshService {

	@Autowired
	private SshDao sshDao;

	@Value("${cipher-key}")
	protected String cipherKey;

	@Autowired
	private HostDao appHostDao;

	@Autowired
	private DestroableProcessManager pm;

	@Override
	public PageModel<SshBean> page(PageModel<SshBean> pm, String name) {
		pm.page(PageHelper.startPage(pm.getPageNum(), pm.getPageSize(), true));
		pm.setRecords(sshDao.list(getRequestOrganizationCodes(), name));
		return pm;
	}

	@Override
	public List<SshBean> getForSelect() {
		return sshDao.list(getRequestOrganizationCodes(), null);
	}

	public void save(SshBean ssh) {
		if (isNull(ssh.getId())) {
			ssh.preInsert(getRequestOrganizationCode());
			insert(ssh);
		} else {
			ssh.preUpdate();
			update(ssh);
		}
	}

	private void insert(SshBean ssh) {
		if (StringUtils.isNotBlank(ssh.getSshKey())) {
			ssh.setSshKey(encryptSshkeyToHex(cipherKey, ssh.getSshKey()));
		}
		sshDao.insertSelective(ssh);
	}

	private void update(SshBean ssh) {
		if (StringUtils.isNotBlank(ssh.getSshKey())) {
			ssh.setSshKey(encryptSshkeyToHex(cipherKey, ssh.getSshKey()));
		}
		sshDao.updateByPrimaryKeySelective(ssh);
	}

	public SshBean detail(Long id) {
		Assert.notNull(id, "id is null");
		SshBean ssh = sshDao.selectByPrimaryKey(id);
		ssh.setSshKey(decryptSshkeyFromHex(cipherKey, ssh.getSshKey()));
		return ssh;
	}

	public void del(Long id) {
		Assert.notNull(id, "id is null");
		SshBean ssh = new SshBean();
		ssh.setId(id);
		ssh.setDelFlag(BaseBean.DEL_FLAG_DELETE);
		sshDao.updateByPrimaryKeySelective(ssh);
	}

	@Override
	public void testSSHConnect(Long hostId, String sshUser, String sshKey, Long sshId) throws Exception {
		Host appHost = appHostDao.selectByPrimaryKey(hostId);
		if (Objects.nonNull(sshId)) {
			SshBean ssh = sshDao.selectByPrimaryKey(sshId);
			sshUser = ssh.getUsername();
			sshKey = ssh.getSshKey();
		}
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		String command = "echo " + uuid;
		String echoStr;
		try {
			echoStr = pm.execWaitForComplete(new RemoteDestroableCommand(command, 10000, sshUser, appHost.getHostname(),
					decryptSshkeyFromHex(cipherKey, sshKey).toCharArray()));
		} catch (UnknownHostException e) {
			throw new UnknownHostException(appHost.getHostname() + ": Name or service not known");
		}
		if (Objects.isNull(echoStr) || !uuid.equals(echoStr.replaceAll("\n", ""))) {
			throw new IOException("Test Connect Fail");
		}
	}

}