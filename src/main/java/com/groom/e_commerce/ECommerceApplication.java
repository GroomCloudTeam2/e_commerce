package com.groom.e_commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@ConfigurationPropertiesScan
@SpringBootApplication
public class ECommerceApplication {
	public static void main(String[] args) {
		SpringApplication.run(ECommerceApplication.class, args);
	}
}
