package io.hkarling.qeurydsl;

import static io.hkarling.qeurydsl.entity.QMember.member;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.hkarling.qeurydsl.dto.MemberDTO;
import io.hkarling.qeurydsl.dto.QMemberDTO;
import io.hkarling.qeurydsl.dto.UserDTO;
import io.hkarling.qeurydsl.entity.Member;
import io.hkarling.qeurydsl.entity.QMember;
import io.hkarling.qeurydsl.entity.Team;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
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
            .select(Projections.bean(MemberDTO.class,
                member.username,
                member.age))
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

    @Test
    public void findDtoByQueryProjection() {
        List<MemberDTO> result = queryFactory.select(new QMemberDTO(member.username, member.age))
            .from(member)
            .fetch();

        for (MemberDTO memberDTO : result) {
            System.out.println("memberDTO = " + memberDTO);
        }
    }

    /**
     * 동적 쿼리 - BooleanBuilder 사용
     */
    @Test
    public void dynamicQueryBooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameParam != null) {
            builder.and(member.username.eq(usernameParam));
        }
        if (ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }
        return queryFactory.selectFrom(member)
            .where(builder)
            .fetch();
    }

    /**
     * 동적 쿼리 - Where 다중 파라미터 사용 : 실무에서 활용하기 좋음.
     *  - where 절 내의 null 값은 무시가 된다
     *  - 메서드를 다른 쿼리에서 재사용할 수 있다
     *  - 쿼리 자체의 가독성이 높아진다
     */
    @Test
    public void dynamicQueryWhereParam() {
        String usernameParam = "member1";
//        Integer ageParam = 10;
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory.selectFrom(member)
//            .where(usernameEq(usernameCond), ageEq(ageCond))
            .where(allEq(usernameCond, ageCond))
            .fetch();
    }

//    private Predicate usernameEq(String usernameCond) {
//        return usernameCond == null ? null : member.username.eq(usernameCond);
//    }
//
//    private Predicate ageEq(Integer ageCond) {
//        return ageCond == null ? null : member.age.eq(ageCond);
//    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond == null ? null : member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond == null ? null : member.age.eq(ageCond);
    }

    private BooleanExpression allEq(String usernameCmd, Integer ageCond) {
        return usernameEq(usernameCmd).and(ageEq(ageCond));
    }

    /**
     * 수정 삭제 배치 쿼리 : bulk 연산
     *  - 쿼리 한방에 대량 데이터 수정
     */
    @Test
    public void bulkUpdate() {

        // member1 = 10 -> DB member1
        // member2 = 20 -> DB member2
        // member3 = 30 -> DB member3
        // member4 = 40 -> DB member4

        long count = queryFactory.update(member)
            .set(member.username, "비회원")
            .where(member.age.lt(28))
            .execute();

        System.out.println("count = " + count);

        // 1 member1 = 10 -> 1 DB 비회원
        // 2 member2 = 20 -> 2 DB 비회원
        // 3 member3 = 30 -> 3 DB member3
        // 4 member4 = 40 -> 4 DB member4

        // 벌크연산 시 DB에 바로 쿼리를 날리지만 영속성 컨텍스트의 내용은 변하지 않는다. (이미 영속성 컨텍스트에 존재하기 때문에 새로 가져오지 않음)
        // Repeatable read.

        List<Member> result1 = queryFactory.selectFrom(member).fetch();
        for (Member member1 : result1) {
            System.out.println("member1 = " + member1);
        }

        em.flush();
        em.clear(); // 초기화 하면 제대로 나온다. 따라서 벌크 연산 이후에는 em 을 초기화 한다.

        List<Member> result2 = queryFactory.selectFrom(member).fetch();
        for (Member member1 : result2) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void bulkAdd() {
        long count = queryFactory.update(member)
            .set(member.age, member.age.add(1))
            .set(member.age, member.age.multiply(1))
            .execute();

        System.out.println("count = " + count);
    }

    @Test
    public void bulkDelete() {
        long count = queryFactory.delete(member)
            .where(member.age.gt(30))
            .execute();
        System.out.println("count = " + count);
    }

    @Test
    public void sqlFunction() {
        List<String> result = queryFactory
            .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
            .from(member)
            .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        /**
         * db function 을 추가하려면 Dialect 를 상속받아 해당 function 을 추가한 후 application.yaml 에서 dialect 를 변경한다.
         */
    }

    @Test
    public void sqlFunction2() {
        List<String> result = queryFactory.select(member.username)
            .from(member)
            //.where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
            .where(member.username.eq(member.username.lower())) // 대부분의 디비에서 공통적으로 들어가는 함수들은 이미 jpa 에서 제공하고 있다.
            .fetch();
    }
}
