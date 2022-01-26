package io.hkarling.qeurydsl;

import static io.hkarling.qeurydsl.entity.QMember.member;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.hkarling.qeurydsl.dto.MemberDTO;
import io.hkarling.qeurydsl.dto.UserDTO;
import io.hkarling.qeurydsl.entity.Member;
import io.hkarling.qeurydsl.entity.QMember;
import io.hkarling.qeurydsl.entity.Team;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class QuerydslIntermediateTest {

    @PersistenceContext
    private EntityManager em;

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
    public void simpleProjection() {
        List<String> result = queryFactory.select(member.username)
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory.select(member.username, member.age)
            .from(member)
            .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username + ", age = " + age);
        }
    }

    /**
     * tuple 은 querydsl 패키지에 존재. 따라서 이를 dao 나 repository 까지만 사용하고 service 와 controller 단까지 올리지 마라 하부 기술을 교체해도 문제가 없도록!
     */

    @Test
    public void findDtoByJPQL() {
        List<MemberDTO> resultList = em.createQuery("select new io.hkarling.qeurydsl.dto.MemberDTO(m.username, m.age) from Member m", MemberDTO.class)
            .getResultList(); // 생성자 주입만 가능하다...

        for (MemberDTO memberDTO : resultList) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDtoBySetter() {
        List<MemberDTO> result = queryFactory
            .select(Projections.bean(MemberDTO.class, member.username, member.age))
            .from(member)
            .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDtoByField() {
        List<MemberDTO> result = queryFactory
            .select(Projections.fields(MemberDTO.class,
                member.username,
                member.age))
            .from(member)
            .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDTO> result = queryFactory
            .select(Projections.constructor(MemberDTO.class,
                member.username,
                member.age))
            .from(member)
            .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");
        List<UserDTO> result = queryFactory
            .select(Projections.fields(UserDTO.class,
//                member.username, // null 로 들어간다.
                member.username.as("name"), // alias 로 맞춰준다. ExpressionUtils.as(member.username, "name") 와 같음.
//                member.age
                ExpressionUtils.as(
                    JPAExpressions.select(memberSub.age.max())
                        .from(memberSub), "age")))
            .from(member)
            .fetch();

        for (UserDTO userDTO : result) {
            System.out.println("userDTO = " + userDTO);
        }
    }

    @Test
    public void findUserDtoByConstructor() {
        List<UserDTO> result = queryFactory.select(Projections.constructor(UserDTO.class,
                member.username, // 생성자를 사용하면 property 명을 안 맞춰줘도 된다.
                member.age))
            .from(member)
            .fetch();

        for (UserDTO userDTO : result) {
            System.out.println("userDTO = " + userDTO);
        }

    }
}
