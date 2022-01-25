package io.hkarling.qeurydsl;

import static io.hkarling.qeurydsl.entity.QMember.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.hkarling.qeurydsl.entity.Member;
import io.hkarling.qeurydsl.entity.QMember;
import io.hkarling.qeurydsl.entity.Team;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL() {
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
            .setParameter("username", "member1")
            .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl() {
        // 같은 테이블을 join 해서 구분을 할 경우에만 QMember 을 선언해서 사용한다.
//        QMember m = new QMember("m");
//        Member findMember = queryFactory.select(m).from(m).where(m.username.eq("member1")).fetchOne();

        Member findMember = queryFactory.select(member) // static import 활용할 것
            .from(member)
            .where(member.username.eq("member1"))
            .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory.selectFrom(member)
            .where(member.username.eq("member1")
                .and(member.age.between(10, 30)))
            .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory.selectFrom(member)
            .where(
                member.username.eq("member1"), // AND 로만 있을 경우 이게 좋다. null 무시된다.
                member.age.eq(10)
            )
            .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
}
