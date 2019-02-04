package rocks.inspectit.oce.bootstrap.version;

public class VersionProvider {

    /**
     * Extract the version number of the agent from the packagename generated by the gradle build script.
     *
     * @return the version name
     */
    public static String getVersion() {
        String versionStr = VersionProvider.class.getName();
        versionStr = versionStr.substring("rocks.inspectit.oce.bootstrap.version".length());
        versionStr = versionStr.substring(0, versionStr.indexOf('.'));
        return versionStr.replace('_', '.');
    }
}
