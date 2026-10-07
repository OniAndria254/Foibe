# Spécification Fonctionnelle - Système de Gestion des Dîmes

## 1. Contexte du Projet
Actuellement, la gestion de la dîme au sein de l'église est entièrement manuelle. 
Lors du sabbat, les membres déposent leur dîme dans une enveloppe portant leur nom, leur numéro d'enveloppe et leur église. Le soir du sabbat, les responsables ouvrent chaque enveloppe, enregistrent au stylo le numéro et le montant sur un reçu papier, puis réinsèrent le reçu dans l'enveloppe pour qu'elle soit récupérée par le membre le sabbat suivant.

**Objectif du projet :** Digitaliser progressivement ce workflow pour accélérer la saisie du sabbat soir, fiabiliser la tenue du registre des membres et conserver un historique financier exploitable.

---

## 2. Principes Métier & Règles de Gestion

### A. Membres et Enveloppes
1. **Unicité :** Une enveloppe est **strictement individuelle** (chaque membre d'une même famille possède sa propre enveloppe).
2. **Numérotation définitive :** Le numéro d'enveloppe est attribué de manière permanente à un membre.
3. **Pérennité de l'historique :** En cas de départ d'un membre de l'église, son numéro d'enveloppe **ne doit jamais être réattribué**. Le membre passe au statut `Inactif` ou `Archivé` afin de préserver l'historique comptable exact.
4. **Multi-églises (Extensibilité) :** La version initiale cible une seule église principale (~400 membres), mais la structure de données doit être prête pour gérer plusieurs assemblées locales (`church_id`).

### B. Phase 1 : Initialisation & Nettoyage du Registre
1. **Recensement :** Collecte des fiches des membres actifs.
2. **Attribution initiale :** 
   * Import en masse de la liste nettoyée (ex. via fichier Excel/CSV).
   * Tri automatique par ordre alphabétique (Nom, Prénom).
   * Génération automatique des numéros d'enveloppe de `1` à `N`.
3. **Nouveaux membres ultérieurs :** Tout nouveau membre inscrit ultérieurement recevra le numéro suivant disponible ($N+1$).

---

## 3. Workflow de Saisie du Sabbat Soir

Environ 400 enveloppes sont traitées chaque samedi soir par **2 personnes opérant en parallèle**. L'interface de saisie doit être traitée comme une caisse enregistreuse (priorité à la vitesse et à l'absence de friction).

### A. Saisie Ultra-Rapide (Keyboard-First)
* L'interface de saisie doit pouvoir être manipulée à **100% au clavier** sans toucher la souris.
* **Séquence type de saisie :**
  1. L'opérateur tape le `[Numéro d'enveloppe]` puis appuie sur `[Entrée]`.
  2. Le système affiche immédiatement en gros le nom et prénom du membre pour confirmation visuelle.
  3. L'opérateur saisit le `[Montant]` puis appuie sur `[Entrée]`.
  4. Le système enregistre la transaction et déclenche l'impression automatique du reçu thermique.
  5. Le champ de saisie se réinitialise pour l'enveloppe suivante.

### B. Contrôles & Sécurité de Saisie
* **Prévention des doublons en temps réel :** Si une enveloppe a déjà été saisie le soir même (par l'un ou l'autre des 2 opérateurs), le système doit bloquer l'action et afficher une alerte claire (*Ex: "Enveloppe #123 déjà saisie à 18:30 par l'opérateur B - Montant : X"*).
* **Audit & Modifiabilité :** Un responsable habilité peut modifier ou annuler une erreur de saisie faite lors de la session courante. La transaction d'origine est conservée en historique d'audit.

---

## 4. Système d'Impression des Reçus

* **Format cible :** Reçu thermique au format ticket de caisse (largeur 58mm ou 80mm).
* **Déclenchement :** Impression instantanée à la validation de la saisie.
* **Contenu du ticket :**
  * Nom / En-tête de l'église
  * Date et heure de saisie
  * Numéro d'enveloppe
  * Nom et prénom du membre
  * Montant de la dîme
  * Identifiant / Nom de l'opérateur de saisie

---

## 5. Exploitation des Données & Rapports

### A. Rapport de Séance (Fin de Sabbat)
À la fin de la saisie du samedi soir, le système génère un bilan comprenant :
* Total global de la collecte du jour.
* Nombre d'enveloppes traitées.
* Total collecté ventilé par opérateur (pour le contrôle de caisse).

### B. Historique & Suivi des Membres
* **Fiche membre :** Consultation du cumul des versements (mensuel, trimestriel, annuel) et historique détaillé de toutes les transactions.
* **Alertes de présence/assiduité :** Capacité d'identifier les membres n'ayant effectué aucun versement depuis une période paramétrable (ex: 3 mois ou 6 mois) afin de repérer les départs non signalés.
