package io.hkarling.qeurydsl.repository;

import io.hkarling.qeurydsl.dto.MemberSearchCondition;
import io.hkarling.qeurydsl.dto.MemberTeamDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberRepositoryCustom {

    public List<MemberTeamDTO> search(MemberSearchCondition condition);
    public Page<MemberTeamDTO> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    public Page<MemberTeamDTO> searchPageComplex(MemberSearchCondition condition, Pageable pageable);

}
