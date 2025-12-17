package citizencard.util;

import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;
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

    private static final int MAX_PHOTO_SIZE = 15360; // 15KB - matched with Applet limit
    private static final float JPEG_QUALITY = 0.9f; // Increased quality

    /**
     * Prepare photo for smart card upload
     * Resizes and compresses to fit limit, prioritizing original dimensions
     */
    public static byte[] preparePhotoForCard(File imageFile) throws Exception {
        if (!imageFile.exists()) {
            throw new FileNotFoundException("Image file not found: " + imageFile.getPath());
        }

        // Read original image
        // Try ImageIO first (supports JPG, PNG, BMP)
        BufferedImage original = ImageIO.read(imageFile);

        // If ImageIO returns null (e.g. for WebP), try loading as JavaFX Image
        if (original == null) {
            System.out.println("[PHOTO] ImageIO failed to read, trying JavaFX Image (likely WebP)...");
            Image fxImage = new Image(imageFile.toURI().toString());
            if (fxImage.isError()) {
                throw new IOException("Cannot read image file: " + imageFile.getPath());
            }
            // Convert FX Image to BufferedImage
            original = SwingFXUtils.fromFXImage(fxImage, null);
        }

        if (original == null) {
            throw new IOException("Failed to load image: " + imageFile.getPath());
        }

        // Remove alpha channel (transparency) by converting to RGB
        // This is important because JPEG doesn't support transparency
        BufferedImage rgbImage = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = rgbImage.createGraphics();
        g2d.setColor(java.awt.Color.WHITE); // Fill background white
        g2d.fillRect(0, 0, original.getWidth(), original.getHeight());
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        original = rgbImage;

        System.out.println("[PHOTO] Original size: " + original.getWidth() + "x" + original.getHeight());

        // Step 1: Try with original dimensions first (but limit max dimension to avoid
        // ultra-large files)
        // If > 800px, resize to 800px first for sanity
        BufferedImage workingImage = original;
        int maxDim = Math.max(original.getWidth(), original.getHeight());
        if (maxDim > 800) {
            workingImage = resizeImage(original, 800, 800 * original.getHeight() / original.getWidth());
            System.out.println("[PHOTO] Large image detected, pre-resized to: " + workingImage.getWidth() + "x"
                    + workingImage.getHeight());
        }

        // Compress to JPEG with high quality
        byte[] photoBytes = compressToJPEG(workingImage, JPEG_QUALITY);
        System.out.println("[PHOTO] Initial compression: " + photoBytes.length + " bytes");

        // Step 2: If too large, reduce quality (down to 0.5)
        float quality = JPEG_QUALITY;
        while (photoBytes.length > MAX_PHOTO_SIZE && quality > 0.5f) {
            quality -= 0.1f;
            photoBytes = compressToJPEG(workingImage, quality);
            System.out.println("[PHOTO] Reducing quality to " + String.format("%.1f", quality) + ": "
                    + photoBytes.length + " bytes");
        }

        // Step 3: If still too large, resize incrementally (maintain aspect ratio)
        // Reduce scaling factor until fit
        double scale = 0.9;
        while (photoBytes.length > MAX_PHOTO_SIZE && scale > 0.1) {
            int newWidth = (int) (workingImage.getWidth() * scale);
            int newHeight = (int) (workingImage.getHeight() * scale);

            BufferedImage resized = resizeImage(workingImage, newWidth, newHeight);

            // Try with good quality first, then lower if needed for this size
            photoBytes = compressToJPEG(resized, 0.7f);

            if (photoBytes.length > MAX_PHOTO_SIZE) {
                photoBytes = compressToJPEG(resized, 0.5f);
            }

            System.out.println("[PHOTO] Resizing to " + newWidth + "x" + newHeight + " (scale "
                    + String.format("%.1f", scale) + "): " + photoBytes.length + " bytes");
            scale -= 0.1;
        }

        if (photoBytes.length > MAX_PHOTO_SIZE) {
            throw new IOException("Cannot compress image to fit " + MAX_PHOTO_SIZE + " byte limit.");
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

        // Convert JavaFX Image to BufferedImage using SwingFXUtils
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);

        // Convert to RGB (handle transparency)
        BufferedImage rgbImage = new BufferedImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = rgbImage.createGraphics();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
        g2d.drawImage(bufferedImage, 0, 0, null);
        g2d.dispose();

        // Use temporary file to reuse the logic (or refactor to direct byte array - but
        // file is easier for now)
        File tempFile = File.createTempFile("temp_photo", ".png");
        try {
            ImageIO.write(rgbImage, "png", tempFile);
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

        // CRITICAL: Fill white background FIRST to prevent black backgrounds
        // This fixes PNG images with transparency appearing black
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, newWidth, newHeight);

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
            System.err.println("[PHOTO] bytesToImage: null or empty input");
            return null;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(photoBytes);
            Image image = new Image(bais);

            // Debug logging
            if (image.isError()) {
                System.err.println("[PHOTO] bytesToImage: Image error - " + image.getException());
                return null;
            }

            System.out.println("[PHOTO] bytesToImage: Created image " +
                    (int) image.getWidth() + "x" + (int) image.getHeight());

            return image;
        } catch (Exception e) {
            System.err.println("[PHOTO] Error converting bytes to image: " + e.getMessage());
            e.printStackTrace();
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
        if (photoBytes.length < 4)
            return "Unknown";

        // Check JPEG signature
        if (photoBytes[0] == (byte) 0xFF && photoBytes[1] == (byte) 0xD8) {
            return "JPEG";
        }

        // Check PNG signature
        if (photoBytes[0] == (byte) 0x89 && photoBytes[1] == 0x50 &&
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

        // Try to read as image - first with ImageIO (JPG, PNG, BMP, GIF)
        BufferedImage img = ImageIO.read(file);
        
        // If ImageIO fails (e.g. WebP), try JavaFX Image
        if (img == null) {
            try {
                Image fxImage = new Image(file.toURI().toString());
                if (fxImage.isError()) {
                    throw new IllegalArgumentException("Not a valid image file: " + file.getPath());
                }
                // WebP or other format supported by JavaFX - valid!
                System.out.println("[PHOTO] Validated via JavaFX Image (likely WebP): " + file.getName());
            } catch (Exception e) {
                throw new IllegalArgumentException("Not a valid image file: " + file.getPath());
            }
        }
    }
}