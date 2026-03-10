# Onboarding

## Design

Our goal is to follow native guidelines as closely as it possible while keeping
the idea of our app. The design system is material 3 expressive. Some screens
of the app do not yet support certain fancy material 3 things, animations,
correct paddings and so on -- this is currently WIP.

## Architecture

Current architecture is intentionally very simple to allow extensibility in the
future. The current aim is to make architecture such that it will be easy to
build something more complex at post-MVP state, so basically everything is
kept in the `:app` module at the moment, except for `:cards`, which is a
modified library for feed cards.

For now, all files are kept in a single package (until they're moved to a
separate module), there is a
[good read](https://y9vad9.com/en/notes/package-naming-problem) about that.

## How to contribute?

You can chat with the maintainers in
[Discussions](https://github.com/friendly-social/android/discussions) and
suggest your help in any domain. First issues you can find in [backlog](https://github.com/friendly-social/android/milestone/1) milestone.

Only use issues for already planned tasks.

And then just create a PR and preferably use the
[conventional commit names](https://www.conventionalcommits.org/en/v1.0.0/).

There is a complete list of all features in [FEDs](https://github.com/friendly-social/knowledge/tree/main/fed) documentation.
