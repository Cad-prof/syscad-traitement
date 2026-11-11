package sn.syscad.traitement.api;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import sn.syscad.traitement.domain.DecisionValidation;
import sn.syscad.traitement.domain.ImportDonnees;
import sn.syscad.traitement.domain.ImportEtat;

@Service
public class PublicationService {

  public ImportDonnees validerImport(ImportDonnees importDonnees, String auteur, String motif) {
    importDonnees.transitionTo(ImportEtat.EN_PREPARATION);
    importDonnees.transitionTo(ImportEtat.NORMALISE);
    importDonnees.transitionTo(ImportEtat.CONTROLE);
    importDonnees.transitionTo(ImportEtat.EN_ATTENTE_VALIDATION);
    importDonnees.appliquerDecision(DecisionValidation.VALIDER, motif == null ? "Données conformes" : motif);
    return importDonnees;
  }

  public ImportDonnees rejeterImport(ImportDonnees importDonnees, String auteur, String motif) {
    if (motif == null || motif.isBlank() || motif.trim().length() < 10) {
      throw new IllegalArgumentException("Le motif du rejet est obligatoire.");
    }
    importDonnees.transitionTo(ImportEtat.EN_PREPARATION);
    importDonnees.transitionTo(ImportEtat.NORMALISE);
    importDonnees.transitionTo(ImportEtat.CONTROLE);
    importDonnees.transitionTo(ImportEtat.EN_ATTENTE_VALIDATION);
    importDonnees.appliquerDecision(DecisionValidation.REJETER, motif);
    return importDonnees;
  }

  public List<UUID> publierLot(ImportDonnees importDonnees) {
    if (importDonnees.getEtat() != ImportEtat.VALIDE) {
      throw new IllegalStateException("Le lot doit être validé avant publication.");
    }
    importDonnees.transitionTo(ImportEtat.PUBLIE);
    importDonnees.transitionTo(ImportEtat.ARCHIVE);
    return List.of(importDonnees.getId());
  }
}
