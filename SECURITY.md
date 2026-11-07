# Security Policy

## Supported versions

The project has not yet published a stable release. Security fixes will
apply to the latest default branch.

## Reporting a vulnerability

**Please do not report security vulnerabilities through public GitHub
issues, discussions, or pull requests.**

Instead, report them privately via GitHub's
[private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability)
feature on this repository (enabled once the repository is public), or
contact the maintainers directly if you have a private channel.

Include as much of the following as you can:

- The affected component (backend API, admin frontend, public frontend,
  proxy, deployment configuration).
- Step-by-step reproduction instructions or a proof of concept.
- The potential impact.
- Any relevant configuration (with **secrets redacted**).

## What to expect

- Acknowledgement of your report.
- An assessment and, for accepted issues, a fix timeline.
- Credit for the report (if desired) once a fix is released.

## Security-relevant design notes

- All secrets are supplied via environment variables; the backend fails
  fast on startup if required secrets are absent (no insecure defaults).
- Analytics provider credentials are encrypted at rest (AES-256-GCM).
- The example Docker Compose stack is for evaluation/self-hosting; put
  TLS termination in front of it for internet-facing deployments.
- Never commit `.env` files or real credentials. `.env.example` contains
  placeholders only.
