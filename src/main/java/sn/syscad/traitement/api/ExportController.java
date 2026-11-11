package sn.syscad.traitement.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ExportController {

  private static final Set<String> ALLOWED_ROLES =
      Set.of("UTILISATEUR_AUTORISE", "AGENT_CONTROLE", "AUTORITE_VALIDATION", "ADMIN");

  @GetMapping("/exports/parcelles")
  public ResponseEntity<Map<String, Object>> exportParcelles(
      @RequestHeader(value = "X-User-Id", required = false) String userId,
      @RequestHeader(value = "X-User-Role", required = false) String role,
      @RequestParam String zone,
      @RequestParam String format) {

    if (userId == null || userId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
    }
    if (role == null || role.isBlank() || !ALLOWED_ROLES.contains(role.trim())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle utilisateur non autorisé.");
    }
    if (zone == null || zone.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le paramètre zone est obligatoire.");
    }
    if (format == null || !Set.of("geojson", "gpkg", "shp").contains(format.toLowerCase())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format inconnu.");
    }

    return ResponseEntity.ok(
        Map.of(
            "zone", zone,
            "format", format.toLowerCase(),
            "type", "export",
            "fichiers", List.of("manifest.json"),
            "id", UUID.randomUUID().toString()));
  }
}
