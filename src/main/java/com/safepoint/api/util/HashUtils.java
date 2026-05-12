package com.safepoint.api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtils {

  private HashUtils() {}

  /**
   * SHA-256 hash of username:pin used as an anonymous identifier.
   * The raw values are never stored — only this hash.
   */
  public static String computeHash(String username, String pin) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String input = username.trim().toLowerCase() + ":" + (pin != null ? pin.trim() : "");
      byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }
}
