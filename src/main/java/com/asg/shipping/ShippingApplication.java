package com.asg.shipping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ComponentScan(basePackages = {"com.asg"})
@EnableJpaRepositories(basePackages = {"com.asg.common.lib.repository", "com.asg.shipping"})
@EntityScan(basePackages = {"com.asg.common.lib.entity", "com.asg.shipping"})
@SpringBootApplication
public class ShippingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShippingApplication.class, args);
	}

}
