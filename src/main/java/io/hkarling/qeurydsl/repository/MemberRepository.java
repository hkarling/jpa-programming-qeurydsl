package io.hkarling.qeurydsl.repository;

import io.hkarling.qeurydsl.entity.Member;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {

    // select m from member m where m.username = ?;
    List<Member> findByUsername(String username);

}
