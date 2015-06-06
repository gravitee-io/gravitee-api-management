package io.gravitee.common.utils;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public final class ManifestUtils {

  private ManifestUtils() {
  }

  public static String getVersion() {
    Package aPackage = ManifestUtils.class.getPackage();
    return aPackage.getImplementationVersion();
  }
}
