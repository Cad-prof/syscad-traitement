package sn.syscad.traitement.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ImportEtat {
  RECU,
  EN_PREPARATION,
  NORMALISE,
  CONTROLE,
  EN_ATTENTE_VALIDATION,
  VALIDE,
  REJETE,
  PUBLIE,
  ARCHIVE,
  ECHEC;

  private static final Map<ImportEtat, Set<ImportEtat>> TRANSITIONS = initialiserTransitions();

  private static Map<ImportEtat, Set<ImportEtat>> initialiserTransitions() {
    Map<ImportEtat, Set<ImportEtat>> transitions = new EnumMap<>(ImportEtat.class);
    transitions.put(RECU, EnumSet.of(EN_PREPARATION, ECHEC));
    transitions.put(EN_PREPARATION, EnumSet.of(NORMALISE, ECHEC));
    transitions.put(NORMALISE, EnumSet.of(CONTROLE, ECHEC));
    transitions.put(CONTROLE, EnumSet.of(EN_ATTENTE_VALIDATION, ECHEC));
    transitions.put(EN_ATTENTE_VALIDATION, EnumSet.of(VALIDE, REJETE, ECHEC));
    transitions.put(VALIDE, EnumSet.of(PUBLIE, ARCHIVE, ECHEC));
    transitions.put(REJETE, EnumSet.of(ARCHIVE, ECHEC));
    transitions.put(PUBLIE, EnumSet.of(ARCHIVE, ECHEC));
    transitions.put(ARCHIVE, EnumSet.noneOf(ImportEtat.class));
    transitions.put(ECHEC, EnumSet.noneOf(ImportEtat.class));
    return Map.copyOf(transitions);
  }

  public boolean peutTransitionVers(ImportEtat etatCible) {
    if (etatCible == null) {
      return false;
    }
    if (this == etatCible) {
      return true;
    }
    return TRANSITIONS.getOrDefault(this, Set.of()).contains(etatCible);
  }
}
