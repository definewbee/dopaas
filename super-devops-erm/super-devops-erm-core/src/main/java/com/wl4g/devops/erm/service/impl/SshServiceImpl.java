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
import com.wl4g.devops.common.bean.BaseBean;
import com.wl4g.devops.common.bean.erm.Ssh;
import com.wl4g.devops.dao.erm.SshDao;
import com.wl4g.devops.erm.service.SshService;
import com.wl4g.devops.page.PageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import static java.util.Objects.isNull;

/**
 * @author vjay
 * @date 2019-11-14 14:10:00
 */
@Service
public class SshServiceImpl implements SshService {

    @Autowired
    private SshDao sshDao;

    @Override
    public PageModel page(PageModel pm,String name) {
        pm.page(PageHelper.startPage(pm.getPageNum(), pm.getPageSize(), true));
        pm.setRecords(null);
        return pm;
    }

    public void save(Ssh ssh){
        if(isNull(ssh.getId())){
            ssh.preInsert();
            insert(ssh);
        }else{
            ssh.preUpdate();
            update(ssh);
        }
    }

    private void insert(Ssh ssh){
        sshDao.insertSelective(ssh);
    }

    private void update(Ssh ssh){
        sshDao.updateByPrimaryKeySelective(ssh);
    }


    public Ssh detail(Integer id){
        Assert.notNull(id,"id is null");
        return sshDao.selectByPrimaryKey(id);
    }

    public void del(Integer id){
        Assert.notNull(id,"id is null");
        Ssh ssh = new Ssh();
        ssh.setId(id);
        ssh.setDelFlag(BaseBean.DEL_FLAG_DELETE);
        sshDao.updateByPrimaryKeySelective(ssh);
    }



}