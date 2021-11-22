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
package com.wl4g.dopaas.umc.example.health;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.wl4g.component.common.lang.SystemUtils2;

/**
 * {@link MyMqttv5AutoConfiguration}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-11-19 v1.0.0
 * @since v1.0.0
 */
@Configuration
public class MyMqttv5AutoConfiguration {

    // [Note]: MQTT server must enabled mqtt.v5, otherwise the connect and
    // subscription will fail.
    @Bean
    public MqttClient consumerMqttv5Client() {
        try {
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);
            options.setConnectionTimeout(3);
            options.setKeepAliveInterval(3);
            options.setUserName("admin");
            options.setPassword("public".getBytes());
            MqttClient client = new MqttClient("tcp://127.0.0.1:1883", "mqtt.consumer." + SystemUtils2.GLOBAL_PROCESS_SERIAL);

            // If the connect() is not called here, the health check is down.
            client.connect(options);
            return client;
        } catch (MqttException e) {
            // When an error occurs, the service continues to running.
            e.printStackTrace();
        }
        return null;
    }

}
