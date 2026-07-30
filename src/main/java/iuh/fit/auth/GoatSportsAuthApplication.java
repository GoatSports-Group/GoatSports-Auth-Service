package iuh.fit.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class GoatSportsAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoatSportsAuthApplication.class, args);
    }
}
