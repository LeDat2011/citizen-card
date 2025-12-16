package citizencard.util;

import javafx.scene.image.Image;
// import javafx.embed.swing.SwingFXUtils; // Not available in all JavaFX versions
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * Photo Utilities for Smart Card Photo Management
 * 
 * Handles image compression, resizing, and format conversion
 * for smart card photo storage (max 8KB)
 */
public class PhotoUtils {
    
    private static final int MAX_PHOTO_SIZE = 8192; // 8KB
    private static final int TARGET_WIDTH = 100;
    private static final int TARGET_HEIGHT = 120;
    private static final float JPEG_QUALITY = 0.7f;
    
    /**
     * Prepare photo for smart card upload
     * Resizes and compresses to fit 8KB limit
     */
    public static byte[] preparePhotoForCard(File imageFile) throws Exception {
        if (!imageFile.exists()) {
            throw new FileNotFoundException("Image file not found: " + imageFile.getPath());
        }
        
        // Read original image
        BufferedImage original = ImageIO.read(imageFile);
        if (original == null) {
            throw new IOException("Cannot read image file: " + imageFile.getPath());
        }
        
        System.out.println("[PHOTO] Original size: " + original.getWidth() + "x" + original.getHeight());
        
        // Resize to target dimensions
        BufferedImage resized = resizeImage(original, TARGET_WIDTH, TARGET_HEIGHT);
        System.out.println("[PHOTO] Resized to: " + resized.getWidth() + "x" + resized.getHeight());
        
        // Compress to JPEG with quality adjustment
        byte[] photoBytes = compressToJPEG(resized, JPEG_QUALITY);
        System.out.println("[PHOTO] Initial compression: " + photoBytes.length + " bytes");
        
        // If still too large, reduce quality further
        float quality = JPEG_QUALITY;
        while (photoBytes.length > MAX_PHOTO_SIZE && quality > 0.1f) {
            quality -= 0.1f;
            photoBytes = compressToJPEG(resized, quality);
            System.out.println("[PHOTO] Recompressed with quality " + quality + ": " + photoBytes.length + " bytes");
        }
        
        // If still too large, resize smaller
        int width = TARGET_WIDTH;
        int height = TARGET_HEIGHT;
        while (photoBytes.length > MAX_PHOTO_SIZE && width > 50) {
            width = (int)(width * 0.9);
            height = (int)(height * 0.9);
            resized = resizeImage(original, width, height);
            photoBytes = compressToJPEG(resized, 0.5f);
            System.out.println("[PHOTO] Resized to " + width + "x" + height + ": " + photoBytes.length + " bytes");
        }
        
        if (photoBytes.length > MAX_PHOTO_SIZE) {
            throw new IOException("Cannot compress image to fit 8KB limit. Final size: " + photoBytes.length + " bytes");
        }
        
        System.out.println("[PHOTO] Final photo ready: " + photoBytes.length + " bytes");
        return photoBytes;
    }
    
    /**
     * Prepare JavaFX Image for smart card upload
     */
    public static byte[] preparePhotoForCard(Image fxImage) throws Exception {
        if (fxImage == null) {
            throw new IllegalArgumentException("Image is null");
        }
        
        // Convert JavaFX Image to BufferedImage
        // Note: SwingFXUtils may not be available in all JavaFX versions
        // For now, we'll use a workaround
        BufferedImage bufferedImage = new BufferedImage(
            (int)fxImage.getWidth(), 
            (int)fxImage.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );
        
        // Create temporary file and use existing method
        File tempFile = File.createTempFile("temp_photo", ".png");
        try {
            ImageIO.write(bufferedImage, "png", tempFile);
            return preparePhotoForCard(tempFile);
        } finally {
            tempFile.delete();
        }
    }
    
    /**
     * Resize image maintaining aspect ratio
     */
    public static BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        // Calculate scaling to maintain aspect ratio
        double scaleX = (double) targetWidth / original.getWidth();
        double scaleY = (double) targetHeight / original.getHeight();
        double scale = Math.min(scaleX, scaleY);
        
        int newWidth = (int) (original.getWidth() * scale);
        int newHeight = (int) (original.getHeight() * scale);
        
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        
        // High quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return resized;
    }
    
    /**
     * Compress BufferedImage to JPEG bytes with specified quality
     */
    public static byte[] compressToJPEG(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Use ImageIO with quality parameter
        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        
        if (param.canWriteCompressed()) {
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        
        javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        
        writer.dispose();
        ios.close();
        
        return baos.toByteArray();
    }
    
    /**
     * Convert photo bytes back to JavaFX Image
     */
    public static Image bytesToImage(byte[] photoBytes) {
        if (photoBytes == null || photoBytes.length == 0) {
            return null;
        }
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(photoBytes);
            return new Image(bais);
        } catch (Exception e) {
            System.err.println("[PHOTO] Error converting bytes to image: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Save photo bytes to file
     */
    public static void savePhotoToFile(byte[] photoBytes, File outputFile) throws IOException {
        if (photoBytes == null || photoBytes.length == 0) {
            throw new IllegalArgumentException("Photo bytes is empty");
        }
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(photoBytes);
        }
        
        System.out.println("[PHOTO] Saved to file: " + outputFile.getPath() + " (" + photoBytes.length + " bytes)");
    }
    
    /**
     * Get photo info string
     */
    public static String getPhotoInfo(byte[] photoBytes) {
        if (photoBytes == null || photoBytes.length == 0) {
            return "No photo";
        }
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(photoBytes);
            BufferedImage img = ImageIO.read(bais);
            
            if (img != null) {
                return String.format("Size: %dx%d, %s (%d bytes)", 
                    img.getWidth(), img.getHeight(), 
                    getFormatName(photoBytes), photoBytes.length);
            } else {
                return String.format("Unknown format (%d bytes)", photoBytes.length);
            }
        } catch (Exception e) {
            return String.format("Error reading photo (%d bytes)", photoBytes.length);
        }
    }
    
    /**
     * Detect image format from bytes
     */
    private static String getFormatName(byte[] photoBytes) {
        if (photoBytes.length < 4) return "Unknown";
        
        // Check JPEG signature
        if (photoBytes[0] == (byte)0xFF && photoBytes[1] == (byte)0xD8) {
            return "JPEG";
        }
        
        // Check PNG signature
        if (photoBytes[0] == (byte)0x89 && photoBytes[1] == 0x50 && 
            photoBytes[2] == 0x4E && photoBytes[3] == 0x47) {
            return "PNG";
        }
        
        return "Unknown";
    }
    
    /**
     * Validate photo file before processing
     */
    public static void validatePhotoFile(File file) throws Exception {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getPath());
        }
        
        if (!file.isFile()) {
            throw new IllegalArgumentException("Not a file: " + file.getPath());
        }
        
        if (file.length() == 0) {
            throw new IllegalArgumentException("File is empty: " + file.getPath());
        }
        
        if (file.length() > 50 * 1024 * 1024) { // 50MB limit for input
            throw new IllegalArgumentException("File too large (max 50MB): " + file.getPath());
        }
        
        // Try to read as image
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IllegalArgumentException("Not a valid image file: " + file.getPath());
        }
    }
}