package com.example.football.favorite;

import com.example.football.auth.User;
import com.example.football.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteTeamRepository extends JpaRepository<FavoriteTeam, UUID> {

    List<FavoriteTeam> findByUserId(UUID userId);

    Optional<FavoriteTeam> findByUserIdAndTeamId(UUID userId, UUID teamId);

    boolean existsByUserIdAndTeamId(UUID userId, UUID teamId);

    void deleteByUserIdAndTeamId(UUID userId, UUID teamId);

    List<UUID> findTeamIdsByUserId(UUID userId);
}