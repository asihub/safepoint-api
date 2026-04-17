package com.safepoint.api.service;

import com.safepoint.api.dto.ProgressBackupDto;
import com.safepoint.api.entity.ProgressBackup;
import com.safepoint.api.repository.ProgressBackupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProgressBackupService {

  private final ProgressBackupRepository repository;

  /**
   * Saves or updates an encrypted progress backup.
   * The server only stores the ciphertext — history is never visible in plain text.
   */
  @Transactional
  public ProgressBackup save(ProgressBackupDto dto) {
    String hash = computeHash(dto.getUserCode(), dto.getPin());

    ProgressBackup backup = repository.findByUserHash(hash)
        .orElse(new ProgressBackup());

    backup.setUserHash(hash);
    backup.setEncryptedData(dto.getEncryptedData());

    ProgressBackup saved = repository.save(backup);
    log.info("Progress backup saved for hash prefix: {}", hash.substring(0, 8));
    return saved;
  }

  /**
   * Retrieves an encrypted progress backup by username and PIN.
   */
  @Transactional(readOnly = true)
  public Optional<ProgressBackup> get(String userCode, String pin) {
    String hash = computeHash(userCode, pin);
    return repository.findByUserHash(hash);
  }

  /**
   * Checks if a backup exists for the given credentials.
   */
  @Transactional(readOnly = true)
  public boolean exists(String userCode, String pin) {
    String hash = computeHash(userCode, pin);
    return repository.existsByUserHash(hash);
  }

  /**
   * Deletes the progress backup for the given credentials.
   */
  @Transactional
  public boolean delete(String userCode, String pin) {
    String hash = computeHash(userCode, pin);
    if (repository.existsByUserHash(hash)) {
      repository.deleteByUserHash(hash);
      log.info("Progress backup deleted for hash prefix: {}", hash.substring(0, 8));
      return true;
    }
    return false;
  }

  private String computeHash(String username, String pin) {
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
