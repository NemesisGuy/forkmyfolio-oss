# Contributing to ForkMyFolio

Thank you for considering contributing! This project is preparing for its
first open-source release; these guidelines will be refined as the project
matures.

## Code of conduct

Be respectful and constructive. Harassment or hostile behavior will not be
tolerated in project spaces.

## How to contribute

1. **Open an issue first** for bugs or feature proposals, using the provided
   issue templates. Large changes should be discussed before code is written.
2. **Fork / branch** — create a feature branch from the default branch.
3. **Make your change** — keep it focused; one logical change per pull
   request.
4. **Test your change:**
   - Backend: `mvn test` (unit tests) — integration tests
     (`*IntegrationTest`) require a Docker daemon (Testcontainers).
   - Admin frontend: `npm ci && npm run test:unit && npm run build`
   - Public frontend: `npm ci && npm test && npm run build`
5. **Keep code style consistent** with the surrounding code.
   - Admin frontend: `npm run lint` (ESLint) and `npm run format` (Prettier).
6. **Submit a pull request** describing what changed and why, and link the
   issue it addresses.

## Development setup

See the "Development setup" section of the [README](README.md). The fastest
way to run the whole stack locally is the example Docker Compose file:

```bash
cp .env.example .env   # then fill in real values
docker compose -f docker-compose.example.yml up -d --build
```

## Reporting security issues

Please **do not** report security vulnerabilities through public issues.
See [SECURITY.md](SECURITY.md).

## Licensing

By contributing, you agree that your contributions will be licensed under
the [MIT licence](LICENSE) of the repository. See
[ATTRIBUTION.md](ATTRIBUTION.md) for project attribution, and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for third-party component
licences.

## Areas that especially need help

- Documentation (user guide, administration guide, deployment guide).
- Test coverage across all three applications.
- Translations/localization of the public portfolio sites.
