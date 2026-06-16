package com.app.backend.controller;

import com.app.backend.dto.PresignRequest;
import com.app.backend.dto.PresignResponse;
import com.app.backend.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/presign")
    public List<PresignResponse> presign(@RequestBody @Valid PresignRequest request) {
        return uploadService.presign(request.filenames());
    }
}
