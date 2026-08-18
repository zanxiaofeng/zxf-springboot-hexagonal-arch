# REQ-{id}: {Module Name}

## Status
- [ ] Requirement Analysis
- [ ] Technical Design
- [ ] Implementation
- [ ] Testing
- [ ] Completed

## Background
{Brief business background}

## Functional Requirements

### UC-{id}: {Use Case Name}
**Given** {precondition}  
**When** {action}  
**Then** {expected result}

#### Acceptance Criteria
1. {criteria 1}
2. {criteria 2}

## Non-Functional Requirements
- Response time < 200ms (P95)

## Database Changes
- New table `{table_name}` (see `db/migration/V{version}__{description}.sql`)

## Downstream Integration
- {Describe if this feature needs to call external services}
- {Define the client interface and expected behavior}

## Related Docs
- API Spec: `docs/design/api-spec-v1.md#{anchor}`
- Domain Model: `docs/design/domain-model.md#{anchor}`
