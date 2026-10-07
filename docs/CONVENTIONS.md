# Conventions de nommage

Pour le code, la base et les URL, je recommande **tout en anglais**, avec un glossaire fixe pour les termes métier. Le français reste réservé à l'interface (textes, libellés) via les fichiers de messages. Cela évite les mélanges (`membre_id` à côté de `church_id`) qui deviennent pénibles avec le temps.

| Français | Anglais (code et base) |
|---|---|
| Église | `church` |
| Membre | `member` |
| Enveloppe (numéro) | `envelope_number` |
| Dîme | `tithe` |
| Séance de saisie du sabbat | `collection_session` |
| Transaction de dîme | `tithe_transaction` |
| Opérateur | `operator` (c'est un `app_user` avec un rôle) |
| Reçu | `receipt` |
| Annulée / Remplacée | `CANCELLED` / `REPLACED` |

## 1. PostgreSQL

### Règles générales

| Élément | Règle | Exemple |
|---|---|---|
| Tables | `snake_case`, **pluriel** | `members`, `tithe_transactions` |
| Colonnes | `snake_case`, singulier | `envelope_number`, `first_name` |
| Clé primaire | `id` (`bigint generated always as identity`) | `members.id` |
| Clé étrangère | `<table_singulier>_id` | `church_id`, `member_id` |
| Booléens | préfixe `is_` ou `has_` | `is_active` |
| Dates/heures | suffixe `_at` (`timestamptz`) ou `_date` (`date`) | `created_at`, `session_date` |
| Montants | suffixe explicite et entier | `amount_minor` (`bigint`) |
| Statuts | `varchar` + contrainte `CHECK`, valeurs `UPPER_SNAKE` | `status = 'ACTIVE'` |
| Table de liaison | `<a>_<b>` au pluriel | `user_roles` |

Notes :
- N'utilisez **pas** les `ENUM` natifs de Postgres : ils sont pénibles à modifier en migration. Un `varchar` avec `CHECK` se modifie facilement.
- Pas de guillemets, pas de majuscules, pas d'accents dans les identifiants.
- Pas de mot réservé (`user`, `order`, `session`) : d'où `app_users` et `collection_sessions`.
- Colonnes communes à toutes les tables métier : `id`, `church_id`, `created_at`, `updated_at`, `created_by`, `updated_by`.
- Si votre devise n'a pas de sous-unité utilisée, `amount_minor` en `bigint` reste valable (c'est alors simplement l'unité entière). Mettez `currency_code` sur `churches`.

### Contraintes et index

Nommez-les explicitement, car les messages d'erreur deviennent lisibles et vous pouvez mapper la violation `uq_...` vers votre `DuplicateEnvelopeEntryException`.

| Type | Préfixe | Exemple |
|---|---|---|
| Clé primaire | `pk_` | `pk_members` |
| Clé étrangère | `fk_<table>_<colonne>` | `fk_members_church_id` |
| Unicité | `uq_<table>_<colonnes>` | `uq_members_church_envelope` |
| Check | `ck_<table>_<règle>` | `ck_members_status` |
| Index | `idx_<table>_<colonnes>` | `idx_tithe_transactions_member_id` |

### Exemple

```sql
CREATE TABLE members (
    id               bigint GENERATED ALWAYS AS IDENTITY,
    church_id        bigint       NOT NULL,
    envelope_number  integer      NOT NULL,
    last_name        varchar(100) NOT NULL,
    first_name       varchar(100) NOT NULL,
    status           varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT fk_members_church_id FOREIGN KEY (church_id) REFERENCES churches (id),
    CONSTRAINT uq_members_church_envelope UNIQUE (church_id, envelope_number),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

-- Anti-doublon : une seule transaction ACTIVE par enveloppe et par séance
CREATE UNIQUE INDEX uq_tithe_transactions_session_envelope_active
    ON tithe_transactions (collection_session_id, envelope_number)
    WHERE status = 'ACTIVE';
```

### Migrations Flyway

- Dossier : `src/main/resources/db/migration`
- Format : `V<n>__<description_en_snake_case>.sql`, par exemple `V1__create_churches.sql`, `V2__create_members.sql`, `V7__add_status_to_members.sql`.
- Une migration = un changement logique.
- **Ne modifiez jamais une migration déjà appliquée** : créez-en une nouvelle.
- Données de référence éventuelles : `R__` (repeatable), à utiliser avec parcimonie.

## 2. Spring Boot / Java

### Packages : par fonctionnalité, pas par couche

```
org.votreorg.dime
├── church/
├── member/
│   ├── Member.java                  (entité)
│   ├── MemberStatus.java            (enum)
│   ├── MemberRepository.java
│   ├── MemberService.java
│   ├── MemberController.java        (pages Thymeleaf)
│   ├── MemberApiController.java     (JSON, si besoin)
│   └── dto/
│       ├── CreateMemberRequest.java
│       └── MemberResponse.java
├── collection/                      (séances et transactions)
├── receipt/
├── report/
├── audit/
├── security/
├── importer/                        (import CSV/Excel)
└── common/                          (exceptions, utilitaires, config)
```

Package de base en minuscules, au pluriel seulement quand c'est naturel (`dto`, `config`).

### Classes

| Rôle | Convention | Exemple |
|---|---|---|
| Entité JPA | nom singulier, sans suffixe | `Member`, `TitheTransaction` |
| Repository | `<Entité>Repository` | `MemberRepository` |
| Service | `<Domaine>Service` | `TitheEntryService` |
| Controller (pages) | `<Domaine>Controller` | `MemberController` |
| Controller (JSON) | `<Domaine>ApiController` | `TitheEntryApiController` |
| DTO entrée | `<Action><Entité>Request` | `CreateMemberRequest` |
| DTO sortie | `<Entité>Response` | `MemberResponse` |
| Enum | nom singulier, valeurs `UPPER_SNAKE` | `TransactionStatus.CANCELLED` |
| Exception | suffixe `Exception`, nom métier | `DuplicateEnvelopeEntryException` |
| Gestion d'erreurs | `GlobalExceptionHandler` (`@RestControllerAdvice`) | |
| Configuration | `<Sujet>Config` | `SecurityConfig` |
| Mapper | `<Entité>Mapper` | `MemberMapper` |

Pour les DTO, utilisez des **`record`** Java. Pas d'entités exposées directement dans les contrôleurs.

### Méthodes et variables

- `camelCase`, verbes pour les méthodes : `registerTithe()`, `cancelTransaction()`, `findActiveByChurchId()`.
- Constantes : `UPPER_SNAKE_CASE`.
- Booléens : `isActive`, `hasDuplicate`.
- Repository, requêtes dérivées : `findByChurchIdAndEnvelopeNumber`, `existsBy...`. Au-delà de deux conditions, passez en `@Query`.
- Évitez les abréviations (`tx`, `mbr`) sauf usage très courant.

### Entités JPA

```java
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "church_id", nullable = false)
    private Long churchId;

    @Column(name = "envelope_number", nullable = false)
    private Integer envelopeNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;
}
```

- Mettez toujours `@Table(name = "...")` et `@Column(name = "...")` explicites, même si Spring convertit `envelopeNumber` en `envelope_number` automatiquement.
- Toujours `@Enumerated(EnumType.STRING)`, jamais `ORDINAL`.
- Évitez `@Data` de Lombok sur les entités (problèmes de `equals`/`hashCode`). Préférez `@Getter` / `@Setter`.
- `ddl-auto: validate` en prod et en dev : **Flyway seul** modifie le schéma.

### REST et URLs

| Élément | Règle | Exemple |
|---|---|---|
| Ressources | **pluriel**, `kebab-case`, noms | `/api/v1/collection-sessions` |
| Sous-ressources | imbriquées sur un niveau | `/api/v1/collection-sessions/{id}/transactions` |
| Actions non-CRUD | verbe en sous-chemin `POST` | `/api/v1/transactions/{id}/cancel` |
| JSON | `camelCase` | `envelopeNumber`, `amountMinor` |
| Pages Thymeleaf | `/members`, `/members/{id}`, `/reports/session/{id}` | |

Pour les erreurs, renvoyez un JSON stable avec un **code métier** en `UPPER_SNAKE` : `{"code": "ENVELOPE_ALREADY_ENTERED", "message": "...", "details": {...}}`. Le JS de saisie s'appuie sur ce code, pas sur le texte.

### Ressources et configuration

| Élément | Convention | Exemple |
|---|---|---|
| Templates | `kebab-case`, un dossier par domaine | `templates/members/list.html`, `templates/members/detail.html` |
| Fragments | `templates/fragments/` | `fragments/layout.html` |
| Reçu | | `templates/receipts/receipt.html` |
| Statiques | `kebab-case` | `static/js/entry-screen.js`, `static/css/receipt.css` |
| Messages i18n | `messages_fr.properties`, clés `domaine.element` | `member.form.lastName=Nom` |
| Propriétés | `kebab-case` | `server.port`, `spring.datasource.url` |
| Propriétés custom | préfixe `dime.` | `dime.backup.directory` |
| Profils | `application-dev.yml`, `application-prod.yml` | |

Regroupez vos propriétés custom dans une classe `@ConfigurationProperties(prefix = "dime")` plutôt que des `@Value` éparpillés.

### Tests

- Classe : `<ClasseTestée>Test` (unitaire), `<ClasseTestée>IT` (intégration, avec Testcontainers PostgreSQL).
- Méthode : `should<Résultat>When<Condition>`, par exemple `shouldRejectEntryWhenEnvelopeAlreadyEntered`.
- Testez la contrainte anti-doublon avec deux insertions concurrentes dès le début : c'est votre règle la plus critique.

## 3. Git

- Branches : `feature/entry-screen`, `fix/duplicate-alert`.
- Commits : style *Conventional Commits* (`feat: add envelope duplicate check`, `fix: ...`, `refactor: ...`).
- Un commit par changement logique, avec la migration SQL dans le même commit que le code qui l'utilise.