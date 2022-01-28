package io.hkarling.qeurydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.persistence.EntityManager;

@SpringBootApplication
public class QeurydslApplication {

    public static void main(String[] args) {
        SpringApplication.run(QeurydslApplication.class, args);
    }

//    @Bean
//    JPAQueryFactory jpaQueryFactory(EntityManager em) {
//        return new JPAQueryFactory(em); // Bean 으로 등록하면 그냥 주입 가능하지.
//    }
}
