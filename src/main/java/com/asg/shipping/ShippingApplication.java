package com.asg.shipping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.asg"})
@EnableJpaRepositories(basePackages = {
		"com.asg.shipping",
		"com.asg.common.lib.repository"
})
@EntityScan(basePackages = {
		"com.asg.shipping",
		"com.asg.common.lib.entity"
})
public class ShippingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShippingApplication.class, args);
	}

}
