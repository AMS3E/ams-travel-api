package ams.asmtravel.api.services.admin.repository;

import ams.asmtravel.api.services.admin.domain.SeedRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeedRunRepository extends JpaRepository<SeedRun, String> {
}
