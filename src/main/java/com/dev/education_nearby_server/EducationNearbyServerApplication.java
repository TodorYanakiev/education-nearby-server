package com.dev.education_nearby_server;

import com.dev.education_nearby_server.config.S3Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(S3Properties.class)
public class EducationNearbyServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EducationNearbyServerApplication.class, args);
	}

}
