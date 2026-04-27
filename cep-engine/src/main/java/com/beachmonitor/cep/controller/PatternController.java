package com.beachmonitor.cep.controller;

import com.beachmonitor.cep.config.EsperConfig;
import com.beachmonitor.cep.persistence.entity.EsperPatternEntity;
import com.beachmonitor.cep.persistence.repository.EsperPatternRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/patterns")
@CrossOrigin(origins = "*")
public class PatternController {

    private final EsperPatternRepository patternRepository;
    private final EsperConfig esperConfig;

    public PatternController(EsperPatternRepository patternRepository, EsperConfig esperConfig) {
        this.patternRepository = patternRepository;
        this.esperConfig = esperConfig;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPatterns() {
        List<EsperPatternEntity> patterns = patternRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (EsperPatternEntity p : patterns) {
            result.add(toMap(p));
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{patternId}/toggle")
    public ResponseEntity<Map<String, Object>> togglePattern(@PathVariable String patternId) {
        Optional<EsperPatternEntity> opt = patternRepository.findAll().stream()
                .filter(p -> p.getPatternId().equals(patternId))
                .findFirst();

        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EsperPatternEntity pattern = opt.get();
        boolean newEnabled = !Boolean.TRUE.equals(pattern.getEnabled());
        pattern.setEnabled(newEnabled);
        pattern.setUpdatedAt(LocalDateTime.now());
        patternRepository.save(pattern);

        if (newEnabled) {
            esperConfig.enablePattern(pattern);
        } else {
            esperConfig.disablePattern(patternId);
        }

        return ResponseEntity.ok(toMap(pattern));
    }

    private Map<String, Object> toMap(EsperPatternEntity p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("patternId", p.getPatternId());
        map.put("name", p.getName());
        map.put("description", p.getDescription());
        map.put("alertLevel", p.getAlertLevel());
        map.put("enabled", p.getEnabled());
        map.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        return map;
    }
}
