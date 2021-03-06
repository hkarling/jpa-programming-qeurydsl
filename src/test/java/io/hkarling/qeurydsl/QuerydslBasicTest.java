package io.hkarling.qeurydsl;

import static com.querydsl.jpa.JPAExpressions.*;
import static io.hkarling.qeurydsl.entity.QMember.*;
import static io.hkarling.qeurydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.hkarling.qeurydsl.entity.Member;
import io.hkarling.qeurydsl.entity.QMember;
import io.hkarling.qeurydsl.entity.Team;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
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
        // ?????? ???????????? join ?????? ????????? ??? ???????????? QMember ??? ???????????? ????????????.
//        QMember m = new QMember("m");
//        Member findMember = queryFactory.select(m).from(m).where(m.username.eq("member1")).fetchOne();

        Member findMember = queryFactory.select(member) // static import ????????? ???
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
                member.username.eq("member1"), // AND ?????? ?????? ?????? ?????? ??????. null ????????????.
                member.age.eq(10)
            )
            .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
        // List
        List<Member> fetch = queryFactory
            .select(member)
            .fetch();

        // ??? ???, ????????? ??? ???????????? NonUniqueResultException
        Member fetchOne = queryFactory
            .selectFrom(member)
            .fetchOne();

        // ?????? ??? ??? ??????
        Member fetchFirst = queryFactory
            .selectFrom(member)
            .fetchFirst();

        // ??????????????? ??????
        QueryResults<Member> memberQueryResults = queryFactory
            .selectFrom(member)
            .fetchResults();

        // ???????????????
        long count = queryFactory
            .selectFrom(member)
            .fetchCount();
    }

    /**
     * ?????? ?????? ??????
     * 1. ?????? ?????? (desc)
     * 2. ?????? ?????? (asc)
     * ???, ????????? ????????? ???????????? ??????
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory.selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .fetch();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
            .selectFrom(member)
            .orderBy(member.username.asc())
            .offset(1)
            .limit(2)
            .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
            .selectFrom(member)
            .orderBy(member.username.asc())
            .offset(1)
            .limit(2)
            .fetchResults(); // fetchResults() ??? deprecated ??????????????? fetch()??? ????????????, count ????????? ???????????? ????????????.

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * JPQL
     * select
     *   COUNT(m), //?????????
     *   SUM(m.age), //?????? ???
     *   AVG(m.age), //?????? ??????
     *   MAX(m.age), //?????? ??????
     *   MIN(m.age) //?????? ??????
     * from Member m
     */
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
            .select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min())
            .from(member)
            .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * ?????? ????????? ??? ?????? ?????? ????????? ?????????
     */
    @Test
    public void group() {
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            //.having() ????????? ?????????.
            .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * Team A??? ????????? ?????? ??????
     */
    @Test
    public void join() {
        List<Member> result = queryFactory.selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");
    }

    @Test
    public void join2() {
        List<Member> result = queryFactory.selectFrom(member)
            .leftJoin(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");
    }

    /**
     * ?????? ??????(??????????????? ?????? ????????? ??????)
     * ????????? ????????? ??? ????????? ?????? ?????? ??????
     */
    @Test
    public void thetaJoin() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory.select(member)
            .from(member, team)
            .where(member.username.eq(team.name))
            .fetch();

        assertThat(result).extracting("username").containsExactly("teamA", "teamB");
    }

    /**
     * ???) ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     *  SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     */
    @Test
    public void joinOnFiltering() {
        List<Tuple> result = queryFactory.select(member, team)
            .from(member)
            .leftJoin(member.team, team).on(team.name.eq("teamA"))
            .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    /**
     * ??????????????? ?????? ????????? ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ?????? ??????
     */
    @Test
    public void joinOnNoRelation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory.select(member, team)
            .from(member)
            .leftJoin(team).on(member.username.eq(team.name))
            //.join(team).on(member.username.eq(team.name))
            .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory.selectFrom(member)
            .where(member.username.eq("member1"))
            .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("?????? ?????? ?????????").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory.selectFrom(member)
            .join(member.team, team).fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("?????? ?????? ?????????").isTrue();
    }

    /**
     * ????????? ?????? ?????? ??????
     */
    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
            .where(member.age.goe(
                select(memberSub.age.max())
                    .from(memberSub)))
            .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * ????????? ?????? ?????????
     */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
            .where(member.age.goe(
                select(memberSub.age.avg())
                    .from(memberSub)))
            .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
            .where(member.age.in(
//                JPAExpressions.select(memberSub.age) // static import ??????
                select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.age.gt(10))))
            .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory.select(
                member.username,
                member.age.subtract(select(memberSub.age.avg())
                    .from(memberSub)))
            .from(member)
            .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * JPA JPQL ??????????????? ??????????????? from ?????? ????????????(????????? ???)??? ???????????? ?????????.
     * ????????? Querydsl ??? ???????????? ?????????.
     * ?????????????????? ???????????? ???????????? select ?????? ??????????????? ????????????.
     * Querydsl??? ?????????????????? ???????????? ???????????? select ?????? ??????????????? ????????????.
     *
     * SQL AntiPatterns ??????
     */

    /**
     * CASE ??????
     */
    @Test
    public void basicCase() {
        List<String> result = queryFactory.select(
                member.age
                    .when(10).then("??????")
                    .when(20).then("?????????")
                    .otherwise("??????"))
            .from(member)
            .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

        List<String> result1 = queryFactory.select(
                new CaseBuilder()
                    .when(member.age.between(0, 20)).then("0~20???")
                    .when(member.age.between(21, 30)).then("21~30???")
                    .otherwise("??????"))
            .from(member)
            .fetch();

        for (String s : result1) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void caseWithOrderBy() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
            .when(member.age.between(0, 20)).then(2)
            .when(member.age.between(21, 30)).then(1)
            .otherwise(3);

        List<Tuple> result = queryFactory.select(member.username, member.age, rankPath)
            .from(member)
            .orderBy(rankPath.desc())
            .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = (Integer) tuple.get(rankPath);

            System.out.println("username = " + username + ", age = " + age + ", rank = " + rank);;
        }
    }

    /**
     * ?????? / ?????? ?????????
     */
    @Test
    public void constant() {
        List<Tuple> tu = queryFactory.select(member.username, Expressions.constant("A"))
            .from(member)
            .fetch();
        for (Tuple tuple : tu) {
            System.out.println("tuple = " + tuple);
        }

        List<String> result = queryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
