# Publishing Guide for @aacassandra/capacitor-nfc

This guide outlines the steps to publish the Capacitor NFC plugin to npm.

## Preparation

Before publishing, make sure:

1. All code changes are completed and tested
2. The version in `package.json` is updated appropriately
3. The README.md is up-to-date
4. The build process completes successfully

## Build the Plugin

```bash
npm run build
```

This will:
- Clean the `dist` directory
- Compile TypeScript files
- Bundle the plugin with Rollup
- Create a tarball package

## Publishing to npm

Make sure you're logged in to npm with an account that has publishing rights to the `@aacassandra` organization:

```bash
npm login
```

Then publish the package:

```bash
npm publish --access public
```

If you want to test the package without publishing it globally:

```bash
npm pack
```

This will create a tarball that you can install locally in a test project:

```bash
npm install /path/to/aacassandra-capacitor-nfc-1.1.0.tgz
```

## After Publishing

Once published:

1. Create a Git tag for the release:
   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```

2. Update the GitHub repository with release notes

3. Notify users of the new release and any breaking changes

## Versioning Guidelines

Follow semantic versioning:

- **MAJOR** version for incompatible API changes
- **MINOR** version for new functionality in a backward-compatible manner
- **PATCH** version for backward-compatible bug fixes

## Testing Before Publishing

Test the plugin in both iOS and Android projects before publishing:

1. Create a test Capacitor app
2. Install the local package:
   ```bash
   npm install /path/to/aacassandra-capacitor-nfc-1.1.0.tgz
   ```
3. Test all functionality:
   - NFC reading
   - NFC writing
   - Error handling
   - Platform-specific behaviors

## Troubleshooting Common Issues

### iOS Build Issues

- Ensure iOS target is set to iOS 13.0+
- Verify CoreNFC framework is linked
- Check podspec file for any issues

### Android Build Issues

- Verify Android Gradle plugin compatibility
- Check for any dependency conflicts
- Ensure permissions are correctly specified in the manifest
