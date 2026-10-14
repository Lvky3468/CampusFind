package com.example.lostfound.controller;

import com.example.lostfound.model.ItemPost;
import com.example.lostfound.model.User;
import com.example.lostfound.repository.ItemPostRepository;
import com.example.lostfound.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemPostRepository itemRepo;
    private final UserRepository userRepo;

    public ItemController(ItemPostRepository itemRepo, UserRepository userRepo) {
        this.itemRepo = itemRepo;
        this.userRepo = userRepo;
    }

    // Real database-backed summary; never use hardcoded platform metrics.
    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "total", itemRepo.count(),
                "lost", itemRepo.countByLost(true),
                "found", itemRepo.countByLost(false),
                "returned", itemRepo.countByStatusIgnoreCase("RETURNED")
        );
    }

    // 🔍 PUBLIC SEARCH
    @GetMapping
    public List<ItemPost> search(
            @RequestParam(required=false) String q,
            @RequestParam(required=false) String location,
            @RequestParam(required=false) String category,
            @RequestParam(required=false) Boolean isLost,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        return itemRepo.search(blankToNull(q), blankToNull(location), blankToNull(category), isLost,
                PageRequest.of(safePage, safeSize));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // 👁️ PUBLIC VIEW SINGLE ITEM
    @GetMapping("/{id}")
    public ResponseEntity<ItemPost> getItemById(@PathVariable Long id) {
        return itemRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🖼️ PUBLIC PHOTO VIEW
    @GetMapping("/photo/{id}")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        return itemRepo.findById(id)
            .filter(i -> i.getPhoto() != null)
            .map(i -> {
                MediaType type = MediaType.IMAGE_JPEG;
                byte[] bytes = i.getPhoto();
                if (bytes.length > 3 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50) {
                    type = MediaType.IMAGE_PNG;
                }
                return ResponseEntity.ok().contentType(type).body(bytes);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // ➕ POST LOST / FOUND ITEM
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestParam String title,
            @RequestParam(required=false) String description,
            @RequestParam(required=false) String location,
            @RequestParam String dateEvent,
            @RequestParam String category,
            @RequestParam String mobile,
            @RequestParam boolean lost,
            @RequestParam(required=false) MultipartFile photo,
            Authentication auth) throws Exception {

        if (title == null || title.isBlank() || title.trim().length() > 120
                || category == null || category.isBlank()
                || location == null || location.isBlank()
                || mobile == null || mobile.isBlank()) {
            return ResponseEntity.badRequest().body("Title, category, location, and contact number are required.");
        }
        if (photo != null && !photo.isEmpty()) {
            if (photo.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("Photo must be 5 MB or smaller.");
            }
            String contentType = photo.getContentType();
            if (contentType == null || !List.of("image/jpeg", "image/png").contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body("Photo must be a JPEG or PNG image.");
            }
        }

        // 🔐 LOST ITEM → LOGIN REQUIRED
        if (lost && auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Login required to post lost item");
        }

        User reporter = null;
        if (auth != null) {
            reporter = userRepo.findByUsername(auth.getName()).orElseThrow();
        }

        ItemPost item = new ItemPost();
        item.setTitle(title);
        item.setDescription(description);
        item.setLocation(location);
        item.setMobile(mobile);
        item.setCategory(category);
        item.setLost(lost);
        try {
            item.setDateEvent(LocalDate.parse(dateEvent));
        } catch (RuntimeException invalidDate) {
            return ResponseEntity.badRequest().body("dateEvent must be a valid date in YYYY-MM-DD format.");
        }
        item.setReportedBy(reporter);

        if (photo != null && !photo.isEmpty())
            item.setPhoto(photo.getBytes());

        item.setStatus(lost ? "UNVERIFIED" : "VERIFIED");

        return ResponseEntity.ok(itemRepo.save(item));
    }
}
