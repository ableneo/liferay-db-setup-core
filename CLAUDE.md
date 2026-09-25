# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`liferay-db-setup-core` is a Liferay 7.4 OSGi module that creates Liferay portal data (roles, sites, pages, users, custom fields, permissions, service access policies, etc.) from a declarative XML file - "data as code". Distributed via Maven Central as `com.ableneo.liferay:com.ableneo.liferay.db.setup.core`.

- Liferay version: 7.4.3.125-ga125 (see compatibility matrix in README.adoc)
- Java 21 (with `--enable-preview`), Maven 3.8.1+, BND for OSGi manifest
- `nix-shell` (or direnv) provides a ready build environment - see CONTRIBUTING.adoc

## Build & Test Commands

```bash
mvn compile              # compile only
mvn test                 # run all tests
mvn test -Dtest=MarshallUtilTest                      # single test class
mvn test -Dtest=MarshallUtilTest#unmarshallValid  # single test method
mvn test -P coverage     # tests with JaCoCo coverage
mvn package              # full build, produces OSGi bundle jar
mvn prettier:write       # format code (required before commit)
mvn prettier:check       # check formatting
mvn generate-sources     # regenerate JAXB domain classes from XSD
```

## Architecture

### Execution flow

Entry point is `LiferaySetup.setup(...)` (overloads for `File`, `InputStream`, unmarshalled `Setup`, optionally with a caller `Bundle` for classpath resource resolution). The flow:

1. `MarshallUtil.unmarshall(...)` parses the XML into JAXB domain objects, validating against `src/main/resources/setup_definition.xsd` (validation can be disabled via `MarshallUtil.skipValidate`).
2. For each `<company>` in `<configuration>` (by companyid or companywebid), `setupCompany(...)`:
   - configures `SetupConfigurationThreadLocal` (companyId, runAsUserId resolved from `run-as-user-email` or a random admin, caller bundle),
   - runs company-scoped setup in fixed order: delete objects, custom fields, organizations, resource permissions, sites, then company settings (service access policies),
   - resolves the group (named group or GUEST default), sets `runInGroupId`, then runs group-scoped setup: page templates, roles, users, user groups.
3. Original thread-local state (principal, permission checker, site default locale) is restored in a `finally` block.

Order matters: `executeSetupConfiguration` runs before group-scoped setup; new setup features must be wired into `LiferaySetup` in the correct phase (company-scoped vs group-scoped).

### Key packages

- `com.ableneo.liferay.portal.setup` - entry point (`LiferaySetup`), XML (un)marshalling (`MarshallUtil`), thread-local context (`SetupConfigurationThreadLocal`)
- `com.ableneo.liferay.portal.setup.core` - one `Setup*` utility class per feature area (SetupSites, SetupPages, SetupArticles, SetupRoles, ...)
- `com.ableneo.liferay.portal.setup.core.util` - resolvers and helpers (`ResolverUtil` for XML placeholder resolution, `ResourcesUtil` for caller-bundle resources, `ServiceTrackerBuilder`, ...)
- `com.ableneo.liferay.portal.setup.upgrade` - `BasicSetupUpgradeProcess` / `SetupUpgradeProcess` extend Liferay `UpgradeProcess` so consumer bundles can run setup XML files as DB upgrade steps
- `com.ableneo.liferay.portal.setup.domain` - JAXB-generated, never edit by hand (see below)
- `com.ableneo.liferay.site.example/` - separate example Maven project used primarily for integration testing

### Domain model (JAXB-generated)

- Domain classes are generated from `src/main/resources/setup_definition.xsd` by `jaxb2-maven-plugin` into `target/generated-sources/sources/`.
- To change the XML schema: edit the XSD (it also carries the user-facing inline documentation), then `mvn generate-sources`. Never edit generated classes.

### Core Setup pattern

Each `Setup*` class is a utility class (private constructor). Public entry methods accept domain model lists, log the item count, and delegate to private single-item methods:

```java
public class SetupFoo {
    private static final Logger LOG = LoggerFactory.getLogger(SetupFoo.class);

    private SetupFoo() {}

    public static void setupFoos(List<Foo> fooList) {
        long companyId = SetupConfigurationThreadLocal.getRunInCompanyId();
        long groupId = SetupConfigurationThreadLocal.getRunInGroupId();
        LOG.info("Setting up {} foos", fooList.size());
        for (Foo foo : fooList) {
            try {
                setupFoo(foo, companyId, groupId);
            } catch (PortalException e) {
                LOG.error("Failed to setup foo: {}", foo.getName(), e);
            }
        }
    }
}
```

- Get `companyId`/`groupId` from `SetupConfigurationThreadLocal` - never from XML domain objects unless the domain explicitly encodes them.
- Setup operations are idempotent: check for existing data (by name/uuid/key) and update rather than fail on re-run.

## Code Style

- Formatter: `prettier-maven-plugin` (prettier-java 2.7.1), 120-char lines, 4-space indent. Match existing style.
- Logging: always SLF4J (`LoggerFactory.getLogger`), never `com.liferay.portal.kernel.log.LogFactoryUtil` - replace it when touching a class that still uses it. Parameterized messages only, exception as last argument.

## Liferay API Conventions

- Use `*LocalServiceUtil` static utilities for service calls (this module uses no DS injection). Never `*ServiceUtil` (remote) unless remote permission checks are required.
- Catch specific exceptions: `PortalException | SystemException` - never `Exception`/`Throwable`. Log with context, or propagate with `throws PortalException`. If wrapping unchecked, use `IllegalStateException` with a message.
- Thread locals: use `com.liferay.petra.lang.CentralizedThreadLocal` for new fields (OSGi classloader isolation), never `java.lang.ThreadLocal`. Restore modified thread-local state in `finally`.
- OSGi services at runtime: `new ServiceTrackerBuilder<>(MyService.class).build()`, always `close()` the tracker (in `finally`) to avoid leaks.
- Use `Validator.isBlank()`/`isNotNull()` from `com.liferay.portal.kernel.util.Validator` and `LocaleUtil` for locale handling.
- Never call `*ServiceUtil` from static initializers or constructors (portal may not be initialized). Never store `ServiceContext` in a static field.

## OSGi & Dependencies

- `bnd.bnd`: all imports `resolution:=optional`, exports only `com.ableneo.*`, `-noee: true`. Keep it that way.
- Liferay/OSGi APIs available in the runtime container are `provided` scope - never `compile`. This includes `release.portal.api`, dom4j, and SLF4J.
- Versions come from `release.portal.bom` / `release.portal.bom.third.party` BOMs - do not hardcode versions for Liferay artifacts.
- Dependencies that must be bundled at runtime go into the maven-shade-plugin config.

## Testing

- JUnit 5 + Mockito 4.x with `mockito-inline` for static mocking of `*LocalServiceUtil` classes:
  ```java
  try (MockedStatic<GroupLocalServiceUtil> groupMock = Mockito.mockStatic(GroupLocalServiceUtil.class)) {
      groupMock.when(() -> GroupLocalServiceUtil.getGroup(anyLong(), anyString())).thenReturn(mockGroup);
  }
  ```
- Extend/reuse `ValidSetupTestMocks` (src/test) for common Liferay mock setup.
- Naming: `<ClassUnderTest>Test.java`, methods `should<Action>When<Condition>()`.

## Versioning & Release

- Version format: `<liferay-major>.<liferay-minor>.<liferay-patch-ga>.<module-revision>[-SNAPSHOT]`, e.g. `7.4.3125.2-SNAPSHOT` targets Liferay 7.4 GA125.
- Releases use `maven-release-plugin` (see `maven-central-*.sh` scripts) - do not manually edit the version in `pom.xml`.
- Commit messages reference GitHub issues (`fix broken feature #1`); update the README.adoc changelog for user-facing changes.
