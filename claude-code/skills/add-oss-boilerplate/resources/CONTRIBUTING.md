# Contributing to <Project Name>

Thank you for your interest in contributing! We welcome contributions from everyone.

## Getting Started

1. **Fork the repository** and clone it locally
2. **Install dependencies**: `pnpm install`
3. **Create a branch**: `git checkout -b your-username/feature-name`

## Development Workflow

### Available Scripts

| Script               | Description               |
| -------------------- | ------------------------- |
| `pnpm dev`           | Start development server  |
| `pnpm build`         | Build for production      |
| `pnpm lint`          | Run ESLint                |
| `pnpm lint:fix`      | Fix ESLint issues         |
| `pnpm format`        | Check Prettier formatting |
| `pnpm format:fix`    | Fix formatting issues     |
| `pnpm test`          | Run tests                 |
| `pnpm test:watch`    | Run tests in watch mode   |
| `pnpm test:coverage` | Run tests with coverage   |

### Before Submitting

1. Run `pnpm lint && pnpm format && pnpm build && pnpm test`
2. Ensure all checks pass
3. Write clear commit messages using conventional commits (`feat:`, `fix:`, `docs:`, etc.)

## Pull Request Process

1. Create a draft PR early to get feedback
2. Link any related issues in the PR description
3. Request review when ready
4. Address feedback and iterate

## Code of Conduct

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing.

## License

By contributing, you agree that your contributions will be licensed under the Apache 2.0 License.
