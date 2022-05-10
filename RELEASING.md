# Release Process

1. Add release entry to [changelog](./CHANGELOG.md)
1. Update version in each pom.xml:
 - [parent](./pom.xml)
 - [library](./libhoney/pom.xml)
 - [example](./examples/pom.xml)
1. Open a PR with the above, and merge that into main
1. Create new tag on merged commit with the new version (e.g. `v1.3.2`)
1. Push the tag upstream (this will kick off the release pipeline in CI)
1. Copy change log entry for newest version into draft GitHub release created as part of CI publish steps
