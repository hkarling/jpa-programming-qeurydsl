package io.hkarling.qeurydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import io.hkarling.qeurydsl.entity.Member;
import io.hkarling.qeurydsl.entity.QMember;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static io.hkarling.qeurydsl.entity.QMember.*;

@Repository
public class MemberJpaRepository {

    private final EntityManager em; // Proxy 만 주입이 되어 Transaction 단위로 Entity Manager 를 할당해준다.
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        queryFactory = new JPAQueryFactory(em);
    }

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAllQueryDsl() {
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsernameQuerydsl(String username) {
        return queryFactory.selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }
}
