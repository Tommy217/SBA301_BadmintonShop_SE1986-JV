package fu.se.badmintonshop.modules.common.repository;

import fu.se.badmintonshop.modules.common.entity.SystemSetting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {}
