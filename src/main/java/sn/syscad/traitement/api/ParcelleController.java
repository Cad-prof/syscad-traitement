package sn.syscad.traitement.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sn.syscad.traitement.domain.Parcelle;

@RestController
@RequestMapping("/api/v1")
public class ParcelleController {

  private static final Set<String> ALLOWED_ROLES =
      Set.of("UTILISATEUR_AUTORISE", "AGENT_CONTROLE", "AUTORITE_VALIDATION", "ADMIN");

  private final ParcelleService parcelleService;

  public ParcelleController(ParcelleService parcelleService) {
    this.parcelleService = parcelleService;
  }

  @GetMapping("/parcelles")
  public PageResponse<ParcelleResumeResponse> rechercher(
      @RequestHeader(value = "X-User-Id", required = false) String userId,
      @RequestHeader(value = "X-User-Role", required = false) String role,
      @RequestParam(required = false) String commune,
      @RequestParam(required = false) String statut,
      @RequestParam(required = false) String reference,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int taille) {
    requireAuthenticated(userId, role);

    int resolvedPage = Math.max(page, 0);
    int resolvedTaille = Math.max(1, Math.min(taille, 100));

    List<Parcelle> result =
        parcelleService.rechercher(commune, statut, reference, resolvedPage, resolvedTaille);
    List<ParcelleResumeResponse> contenu =
        result.stream().map(ParcelleResumeResponse::from).toList();

    return new PageResponse<>(
        resolvedPage, resolvedTaille, parcelleService.total(commune, statut, reference), contenu);
  }

  @GetMapping("/parcelles/{id}")
  public ResponseEntity<ParcelleDetailResponse> details(
      @RequestHeader(value = "X-User-Id", required = false) String userId,
      @RequestHeader(value = "X-User-Role", required = false) String role,
      @PathVariable UUID id) {
    requireAuthenticated(userId, role);

    return parcelleService
        .trouverParId(id)
        .map(parcelle -> ResponseEntity.ok(ParcelleDetailResponse.from(parcelle)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private void requireAuthenticated(String userId, String role) {
    if (userId == null || userId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
    }
    if (role == null || role.isBlank() || !ALLOWED_ROLES.contains(role.trim())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle utilisateur non autorisé.");
    }
  }

  public record ParcelleResumeResponse(
      UUID id,
      String referenceCadastrale,
      String commune,
      String statut,
      BigDecimal superficieCalculee,
      String etatPublication) {
    static ParcelleResumeResponse from(Parcelle parcelle) {
      return new ParcelleResumeResponse(
          parcelle.id(),
          parcelle.referenceCadastrale(),
          parcelle.commune(),
          parcelle.statut(),
          parcelle.superficieCalculee(),
          "PUBLIE");
    }
  }

  public record ParcelleDetailResponse(
      UUID id,
      String referenceCadastrale,
      String section,
      String numero,
      String commune,
      String statut,
      BigDecimal superficieDeclaree,
      BigDecimal superficieCalculee,
      String etatPublication,
      String source,
      Instant dateImport,
      Map<String, Object> geometrie) {
    static ParcelleDetailResponse from(Parcelle parcelle) {
      return new ParcelleDetailResponse(
          parcelle.id(),
          parcelle.referenceCadastrale(),
          parcelle.section(),
          parcelle.numero(),
          parcelle.commune(),
          parcelle.statut(),
          parcelle.superficieDeclaree(),
          parcelle.superficieCalculee(),
          "PUBLIE",
          parcelle.source(),
          parcelle.dateImport(),
          Map.of(
              "type", "MultiPolygon",
              "coordinates",
                  List.of(
                      List.of(
                          List.of(
                              List.of(-17.0, 14.0),
                              List.of(-17.0, 15.0),
                              List.of(-16.0, 15.0),
                              List.of(-16.0, 14.0),
                              List.of(-17.0, 14.0))))));
    }
  }
}
