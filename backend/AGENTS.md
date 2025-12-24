# Notes for Coding Agent

## Your behavior
- Follow the pattern can be inferred from existing resources first. You MUST propose before modifing anything when you find something to improve.
- Write source code as professional, which means your code must be easy to understand and maintain, and follow coding practices such as SOLID.
- Code something you have instructed. Don't create anything which is not instructed to do so.

## Dev Tips
- All classes have suffix to show its type. They must be like SampleController, SampleService, and so on.
- Write doc or comment in Japanese.
- Propose a user to add library before adding it by yourself. Adding a library without proper permission is violation of development guideline.

## Testing Tips
- Unit test must be desined merely to test login in this codebase. Integration level testing is out of scope.
- All tests must be repeatable, which means you must not use something that fails test in the future such as current time stamp.
- Test coverage target is 80% with branch coverage. No need to accomplish 100%.

## Commands
- build: `./gradlew build`
- run: `./gradlew bootRun`
- test: `./gradlew test`
    - use `--tests` options to run specific test cases, like `./gradlew test --tests "*OrderServiceTest"`
