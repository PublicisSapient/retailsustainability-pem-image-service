package com.publicis.sapient.p2p;

import com.publicis.sapient.p2p.utils.EncryptionUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ImageServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImageServiceApplication.class, args);
	}

	@Bean
	public BCryptPasswordEncoder getBCryptPasswordEncoder(){
		return new BCryptPasswordEncoder();
	}

	@Bean
	public EncryptionUtil getEncryptionUtil(){
		return new EncryptionUtil();
	}
}
