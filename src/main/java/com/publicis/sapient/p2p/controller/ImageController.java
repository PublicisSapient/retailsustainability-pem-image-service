package com.publicis.sapient.p2p.controller;

import com.publicis.sapient.p2p.model.ServiceResponseDto;
import com.publicis.sapient.p2p.model.UrlDto;
import com.publicis.sapient.p2p.service.ImageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value="/images")
@Tag(name = "Images", description = "Image Service")
public class ImageController {
    @Autowired
    ImageService imageService;

    private final Logger logger = LoggerFactory.getLogger(ImageController.class);


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServiceResponseDto uploadImage(@RequestPart("files") MultipartFile files) {
        logger.info("Entered in uploadImage method in controller");
        String url = imageService.uploadImage(files);
        return new ServiceResponseDto(HttpStatus.OK.value(),"Image uploaded Successfully.",url);
    }

    @DeleteMapping
    public ServiceResponseDto deleteImage(@RequestBody UrlDto url) {
        logger.info("Entered in deleteImage method in controller");
        String msg = imageService.deleteImage(url.getUrl());
        return new ServiceResponseDto(HttpStatus.OK.value(),msg,null);
    }

    @GetMapping(value="/homebanner")
    public ServiceResponseDto getImage() {
        logger.info("Entered in getImage method in controller");

        return new ServiceResponseDto(HttpStatus.OK.value(),"Image fetched Successfully.",imageService.getImage());
    }

    @DeleteMapping(value="/dumpImage")
    public ServiceResponseDto deleteImageFromDumpImage(@RequestBody UrlDto urls) {
        logger.info("Entering deleteImageFromDumpImage method with endpoint: /images/dumpImage");
        String msg = imageService.deleteImageFromDumpImage(urls.getUrl());
        return new ServiceResponseDto(HttpStatus.OK.value(),msg,null);
    }
}
