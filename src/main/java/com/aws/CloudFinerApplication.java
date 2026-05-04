package com.aws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication

@EntityScan(basePackages = "com.aws.entity")

@EnableScheduling
public class CloudFinerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudFinerApplication.class, args);
		System.err.println("application started");
	}

}
