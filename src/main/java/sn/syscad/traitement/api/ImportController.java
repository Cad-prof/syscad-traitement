package sn.syscad.traitement.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ImportController {

  private static final Set<String> ALLOWED_ROLES =
      Set.of("AGENT_CONTROLE", "AUTORITE_VALIDATION", "ADMIN");

  private final ImportService importService;

  public ImportController(ImportService importService) {
    this.importService = importService;
  }

  @PostMapping("/imports")
  public ResponseEntity<ImportCreationResponse> createImport(
      @RequestHeader(value = "X-User-Id", required = false) String userId,
      @RequestHeader(value = "X-User-Role", required = false) String role,
      @RequestPart("fichier") MultipartFile fichier,
      @RequestParam("source") String source,
      @RequestParam("perimetre") String perimetre,
      @RequestParam(value = "millesime", required = false) String millesime,
      @RequestParam(value = "crs", required = false) String crs,
      @RequestParam(value = "couche", required = false) String couche) {

    if (userId == null || userId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required.");
    }
    if (role == null || role.isBlank() || !ALLOWED_ROLES.contains(role.trim())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rôle utilisateur non autorisé.");
    }
    if (fichier == null || fichier.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier est obligatoire.");
    }

    UUID id = UUID.randomUUID();
    String format = importService.detectFormat(fichier);
    if (!importService.isArchiveSafe(fichier)) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Archive invalide : zip-slip");
    }

    return ResponseEntity.accepted()
        .location(URI.create("/api/v1/imports/" + id))
        .body(new ImportCreationResponse(id.toString(), "recu", format, source, perimetre));
  }

  public static record ImportCreationResponse(
      String id, String etat, String format, String source, String perimetre) {}
}
