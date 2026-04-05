package com.example.messageinbottle;

import com.example.messageinbottle.config.QiniuProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(QiniuProperties.class)
public class MessageInBottleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageInBottleApplication.class, args);
    }
}
