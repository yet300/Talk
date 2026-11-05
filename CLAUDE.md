@AGENTS.md

## Claude Code

- Project skills: `.claude/skills` is a symlink to `.agents/skills`, so the same skills serve every agent.
- Long Gradle builds (first build, iOS link, full test runs) go in the background.
- After changing npm dependencies of a Kotlin/JS module, run `./gradlew kotlinUpgradeYarnLock`
  and commit `kotlin-js-store/yarn.lock`.
- Web preview: the `web` configuration in `.claude/launch.json` runs the webpack dev server on port 8080.
