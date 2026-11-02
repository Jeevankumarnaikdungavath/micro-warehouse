# Git workflow & suggested 6-member split

Rule: **never commit directly to `main`**. One feature branch per module, small commits, PR + at least one teammate review before merge.

| Member | Branch | Owns |
|---|---|---|
| 1 – Lead / Config | `feature/project-setup` | repo skeleton, `pom.xml`, `application.properties`, `DatabaseConfig`, README |
| 2 – Database | `feature/db-schema` | `schema.sql`, `data.sql` (constraints, realistic seed data) |
| 3 – DAO | `feature/dao-layer` | `ProductDao`, `OrderDao`, `OrderItemDao`, `ReportDao`, RowMappers |
| 4 – Services (inventory/orders) | `feature/service-rules` | `InventoryService`, `OrderService` (batch + atomic deduction), `WarehouseException` |
| 5 – Reports & tests | `feature/reports-and-tests` | `ReportService`, `OrderServiceTest`, extra tests |
| 6 – Console UI | `feature/console-ui` | `ConsoleApp`, input validation, demo script |

## Order of merging
`project-setup` → `db-schema` → `dao-layer` → `service-rules` + `reports-and-tests` → `console-ui`

## Commands
```bash
git clone <repo-url> && cd micro-warehouse
git checkout -b feature/dao-layer
# ... work ...
git add src/main/java/com/quickcart/warehouse/dao
git commit -m "feat(dao): add ProductDao with row-locking stock query"
git push -u origin feature/dao-layer
# open a Pull Request on GitHub, request a review, merge after approval
git checkout main && git pull
```
Keep your branch current to avoid conflicts: `git fetch origin && git rebase origin/main` (or merge `origin/main`).

## Commit message style
`feat(scope): …`, `fix(scope): …`, `test(scope): …`, `docs: …`, `refactor(scope): …` – one logical change per commit, every member commits.

## PR checklist
- [ ] Builds (`mvn clean package`) and tests pass
- [ ] No string-concatenated user input in SQL (parameterized only)
- [ ] DAO code has no business rules, services have no SQL
- [ ] At least one reviewer approved
