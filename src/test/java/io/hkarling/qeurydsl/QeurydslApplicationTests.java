package io.hkarling.qeurydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.hkarling.qeurydsl.entity.Hello;
import io.hkarling.qeurydsl.entity.QHello;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Commit
class QeurydslApplicationTests {

    @PersistenceContext
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QHello qHello = new QHello("H");

        Hello result = query
            .selectFrom(qHello)
            .fetchOne();

        Assertions.assertThat(result.getId()).isEqualTo(hello.getId());
    }

}
