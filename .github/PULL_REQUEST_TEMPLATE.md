<!-- Thanks for contributing! Please read CONTRIBUTING.md before opening a PR. -->

## Linked issue

<!-- Every PR should close an issue, e.g. "Fixes #123". If there is no issue yet, open one first. -->

Fixes #

## What changed

<!-- A short, plain-English summary of the change and why it's the right fix. -->

## How it was tested

<!-- e.g. full Gradle test suite (command + result), manual test steps, screenshots for UI changes. -->

## Checklist

- [ ] The full test suite passes locally (`cd android && ./gradlew test`)
- [ ] New/changed `@Serializable` models decode tolerantly (optionals for fields the server might add or rename)
- [ ] No new third-party dependencies (the list in `android/app/build.gradle.kts` is locked)
- [ ] No invented API endpoints or JSON shapes (verified against upstream source or a running server)
