package com.publicis.sapient.p2p.controller;

import com.publicis.sapient.p2p.model.ServiceResponseDto;
import com.publicis.sapient.p2p.model.UrlDto;
import com.publicis.sapient.p2p.service.ImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {ImageController.class})
@ExtendWith(SpringExtension.class)
class ImageControllerTest {

    @Autowired
    ImageController imageController;

    @MockBean
    ImageService imageService;


    @Test
    void testUploadFiles() {
        MultipartFile mockFile = mock(MultipartFile.class);

        String expectedFileName = UUID.randomUUID() + "-test.jpg";
        String expectedUrl = "https://storage.googleapis.com/p2p-images/product/" + expectedFileName;
        when(imageService.uploadImage(any(MultipartFile.class))).thenReturn(expectedUrl);

        ServiceResponseDto response = imageController.uploadImage(mockFile);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals(expectedUrl, response.getData());
    }


    @Test
    void testDeleteFiles() {
        List<String> url = new ArrayList<>();
        url.add("https://storage.googleapis.com/p2p-images/product/test.jpg");
        UrlDto urls = new UrlDto(url);

        String expectedMsg = "Deleted images from cloud.";
        when(imageService.deleteImage(anyList())).thenReturn(expectedMsg);

        ServiceResponseDto response = imageController.deleteImage(urls);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals(expectedMsg, response.getMessage());
    }

    @Test
    void testGetFile() {
        String url="https://storage.googleapis.com/p2p-images/product/test.jpg";

        when(imageService.getImage()).thenReturn(url);

        ServiceResponseDto response = imageController.getImage();

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals(url, response.getData());
    }

    @Test
    void testDeleteImageFromDumpImage() {
        List<String> url = new ArrayList<>();
        url.add("https://storage.googleapis.com/p2p-images/product/test.jpg");
        UrlDto urls = new UrlDto(url);

        String expectedMsg = "Removed images from dump repository.";
        when(imageService.deleteImageFromDumpImage(anyList())).thenReturn(expectedMsg);

        ServiceResponseDto response = imageController.deleteImageFromDumpImage(urls);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals(expectedMsg, response.getMessage());
    }
}
