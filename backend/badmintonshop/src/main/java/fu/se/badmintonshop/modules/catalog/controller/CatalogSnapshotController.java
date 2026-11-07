package fu.se.badmintonshop.modules.catalog.controller;

import fu.se.badmintonshop.modules.catalog.service.CatalogSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogSnapshotController {
    private final CatalogSnapshotService catalog;

    @GetMapping
    public ResponseEntity<Map<String, Object>> snapshot(
            @RequestParam(defaultValue = "auto") String view, Authentication authentication) {
        if (!Set.of("auto", "shop", "management").contains(view))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chế độ tải danh mục không hợp lệ.");
        boolean staff = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF").contains(a.getAuthority()));
        if ("management".equals(view) && !staff)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền tải dữ liệu quản lý.");
        // Responses may contain management data and live stock; never cache across users.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(catalog.snapshot(staff && !"shop".equals(view)));
    }
}
