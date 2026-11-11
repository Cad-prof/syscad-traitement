package sn.syscad.traitement.api;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ImportDecisionController {

  private static final java.util.Set<String> ALLOWED_ROLES =
      java.util.Set.of("AUTORITE_VALIDATION", "ADMIN");

  @PostMapping("/imports/{id}/validation")
  public ResponseEntity<Map<String, Object>> valider(
      @RequestHeader(value = "X-User-Id", required = false) String userId,
      @RequestHeader(value = "X-User-Role", required = false) String role,
      @PathVariable UUID id,
      @RequestBody DecisionRequest request) {

    if (userId == null || userId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
    }
    if (role == null || role.isBlank() || !ALLOWED_ROLES.contains(role.trim())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle utilisateur non autorisé.");
    }
    if (request == null || request.decision() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La décision est obligatoire.");
    }
    if (request.decision().equalsIgnoreCase("REJETER")
        && (request.motif() == null || request.motif().isBlank() || request.motif().trim().length() < 10)) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Le motif est obligatoire.");
    }

    return ResponseEntity.ok(
        Map.of(
            "id", id.toString(),
            "decision", request.decision(),
            "motif", request.motif() == null ? "" : request.motif(),
            "auteur", userId,
            "etat", "en_attente_validation"));
  }

  public record DecisionRequest(String decision, String motif) {}
}
