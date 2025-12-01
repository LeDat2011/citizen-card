package com.citizencard.backend.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;

public class EncryptionService {
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int RSA_KEY_SIZE = 2048;
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int PBKDF2_KEY_LENGTH = 256;
    
    private KeyPair rsaKeyPair;
    
    public EncryptionService() {
        try {
            generateRSAKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption service", e);
        }
    }
    
    private void generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(RSA_KEY_SIZE);
        this.rsaKeyPair = keyGen.generateKeyPair();
    }
    
    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
    }
    
    public String getPrivateKeyBase64() {
        return Base64.getEncoder().encodeToString(rsaKeyPair.getPrivate().getEncoded());
    }
    
    public PublicKey getPublicKey() {
        return rsaKeyPair.getPublic();
    }
    
    public PrivateKey getPrivateKey() {
        return rsaKeyPair.getPrivate();
    }
    
    public PublicKey loadPublicKeyFromBase64(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
    
    public PrivateKey loadPrivateKeyFromBase64(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
    
    public SecretKey deriveAESKeyFromPIN(String pin) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), "CITIZEN_CARD_SALT".getBytes(), 
                                         PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    public String encryptWithAES(String plaintext, String pin) throws Exception {
        SecretKey key = deriveAESKeyFromPIN(pin);
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
        
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);
        
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }
    
    public String decryptWithAES(String encryptedData, String pin) throws Exception {
        SecretKey key = deriveAESKeyFromPIN(pin);
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] ciphertext = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertext);
        
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
        
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, "UTF-8");
    }
    
    public String encryptWithRSA(String plaintext, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    public String decryptWithRSA(String encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] encrypted = Base64.getDecoder().decode(encryptedData);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, "UTF-8");
    }
    
    public byte[] encryptBytesWithAES(byte[] plaintext, String pin) throws Exception {
        SecretKey key = deriveAESKeyFromPIN(pin);
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext);
        
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);
        
        return byteBuffer.array();
    }
    
    public byte[] decryptBytesWithAES(byte[] encryptedData, String pin) throws Exception {
        SecretKey key = deriveAESKeyFromPIN(pin);
        
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] ciphertext = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertext);
        
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
        
        return cipher.doFinal(ciphertext);
    }
}

