package com.saywell.backend.repository;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.saywell.backend.entity.User;

@Mapper
public interface UserRepository {

    /**
     * メールアドレスでユーザー検索 (パスワードハッシュを含むEntityを返す)
     */
    Optional<User> findByEmail(@Param("email") String email);

}
