package com.publicis.sapient.p2p.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public interface ImageService {

    String uploadImage(MultipartFile files);

    String deleteImage(List<String> url);

    String getImage();

    String deleteImageFromDumpImage(List<String> urls);
}
