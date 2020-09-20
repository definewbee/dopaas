# Copyright (c) 2017 ~ 2025, the original author wangl.sir individual Inc,
# All rights reserved. Contact us <Wanglsir@gmail.com, 983708408@qq.com>
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# #### Spring cloud config server configuration. ####
#
spring:
  application.name: ${projectName?uncap_first}-server
  profiles:
    include: common,support,util
    active: dev
  cloud:
    devops:
      iam: # IAM client configuration.
        cors:
          enabled: true # Default: true
          rules:
            '[/**]':
              allows-methods: [GET,HEAD,OPTIONS,POST]
              allows-headers: [X-Iam-*]
              allow-credentials: true
              #allows-origins:
                #- '*'
        xsrf:
          enabled: true
        replay:
          enabled: true
        xss:
          enabled: true # Default: true
        client: # IAM client configuration.
          filter-chain:
            /public/**: anon # Public rule release
          cipher:
            enable-data-cipher: true # Default by true
          session:
            enable-access-token-validity: true # Default by true

# ### Server configuration. ###
server:
  servlet:
    contextPath: /${r'${'}spring.application.name}
  #address: 0.0.0.0
  port: 8080
  sessionTimeout: 30
  tomcat:
    uri-encoding: UTF-8
    protocolHeader: x-forwarded-proto
    remoteIpHeader: x-forwarded-for
    basedir: /tmp/${r'${'}spring.application.name}
    access-log-enabled: false
    accesslog.directory: logs/
    backgroundProcessorDelay: 30 #seconds
    max-thread: 50 # Max worker threads(default:200)
