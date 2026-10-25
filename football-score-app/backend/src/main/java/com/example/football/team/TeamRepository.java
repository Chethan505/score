package com.example.football.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByProviderTeamId(String providerTeamId);

    List<Team> findByCompetitionId(UUID competitionId);

    boolean existsByProviderTeamId(String providerTeamId);
}