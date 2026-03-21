package com.jou.networkrepair.module.file.controller;

import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.exception.BusinessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    @PostMapping("/upload")
    public ApiResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new BusinessException("文件不能为空");
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        File folder = new File("uploads/" + dateDir);
        if (!folder.exists() && !folder.mkdirs()) throw new BusinessException("创建上传目录失败");
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        File target = new File(folder, filename);
        file.transferTo(target);
        String url = "/uploads/" + dateDir + "/" + filename;
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("name", originalName);
        return ApiResult.success("上传成功", data);
    }
}
