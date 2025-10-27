package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    @Query(value = """
      select t from Token t inner join User u\s
      on t.user.id = u.id\s
      where u.id = :id and (t.expired = false and t.revoked = false)\s
      """)
    List<Token> findAllValidTokenByUser(Long id);

    @Query("select t from Token t where t.tokenValue = :token")
    Optional<Token> findByToken(@Param("token") String token);
}
