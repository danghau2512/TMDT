package com.example.demo.storage;

import com.example.demo.exception.ShopException;
import com.example.demo.model.StoredImage;
import jakarta.servlet.http.Part;
import javax.imageio.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** File bất biến ngoài WAR, không dùng tên/path do trình duyệt gửi. */
public final class ImageStorage {
    private final Path root;
    public ImageStorage(Path root) { this.root=root.toAbsolutePath().normalize(); }
    public StoredImage save(Part part) {
        if (part.getSize()>5*1024*1024) throw new ShopException(400,"Mỗi ảnh tối đa 5 MB.");
        try (var input=ImageIO.createImageInputStream(part.getInputStream())) {
            var readers=ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ShopException(400,"File không phải ảnh JPEG/PNG hợp lệ.");
            var reader=readers.next();
            try {
                reader.setInput(input);
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("jpeg","jpg","png").contains(format)) throw new ShopException(400,"Chỉ nhận ảnh JPEG hoặc PNG.");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if (width<1 || height<1 || (long)width*height>20_000_000) throw new ShopException(400,"Ảnh tối đa 20 megapixel.");
                var decoded=reader.read(0);
                boolean png="png".equals(format);
                var output=new ByteArrayOutputStream();
                ImageIO.write(decoded,png?"png":"jpeg",output);
                byte[] bytes=output.toByteArray();
                if (bytes.length>5*1024*1024) throw new ShopException(400,"Ảnh sau xử lý vượt quá 5 MB.");
                String key=UUID.randomUUID()+(png?".png":".jpg");
                Files.createDirectories(root);
                Files.write(root.resolve(key),bytes,StandardOpenOption.CREATE_NEW);
                return new StoredImage(key,png?"image/png":"image/jpeg",bytes.length,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),width,height);
            } finally { reader.dispose(); }
        } catch (ShopException exception) { throw exception; }
        catch (IOException exception) { throw new ShopException(400,"Không đọc hoặc lưu được ảnh. Kiểm tra ảnh và thư mục lưu trữ."); }
        catch (RuntimeException exception) { throw new ShopException(400,"Không xử lý được ảnh JPEG/PNG. Hãy chọn ảnh khác."); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 không khả dụng."); }
    }
    public byte[] read(String key) {
        if (!key.matches("[0-9a-f-]{36}\\.(png|jpg)")) throw new ShopException(404,"Không tìm thấy ảnh.");
        try { return Files.readAllBytes(root.resolve(key)); }
        catch (IOException exception) { throw new ShopException(404,"Không tìm thấy file ảnh."); }
    }
    /** Chỉ dọn file vừa tạo bởi request thất bại, không dọn asset đã commit. */
    public void discard(List<StoredImage> images) { for(var image:images) try { Files.deleteIfExists(root.resolve(image.key())); } catch(IOException ignored) { } }
}
