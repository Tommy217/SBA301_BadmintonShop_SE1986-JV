package fu.se.badmintonshop.modules.common.service;

import fu.se.badmintonshop.modules.common.entity.SystemSetting;
import fu.se.badmintonshop.modules.common.repository.SystemSettingRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {
    private final SystemSettingRepository repository;

    @Transactional(readOnly = true)
    public String get(String key) {
        return repository.findById(key).map(SystemSetting::getSettingValue).orElse(null);
    }

    @Transactional
    public void set(String key, String value, String description, Long actor) {
        SystemSetting setting = repository.findById(key).orElseGet(SystemSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription(description);
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy(actor);
        repository.save(setting);
    }
}
