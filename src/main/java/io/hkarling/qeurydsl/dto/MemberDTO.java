package io.hkarling.qeurydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDTO {

    private String username;
    private int age;

    @QueryProjection // Q 파일을 생성. Querydsl 에 dependency 하기 때문에 이를 걷어낼 때 문제될 수 있다.
    public MemberDTO(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
