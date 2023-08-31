package com.publicis.sapient.p2p.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import com.google.cloud.vision.v1.*;
import com.publicis.sapient.p2p.exception.BusinessException;
import com.publicis.sapient.p2p.exception.util.ErrorCode;
import com.publicis.sapient.p2p.repository.ImageDumpRepository;
import com.publicis.sapient.p2p.validator.ImageValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {ImageServiceImpl.class})
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"spring.cloud.bucket-name=p2p-images"})
@TestPropertySource(properties = {"spring.cloud.folder-name-product=product/"})
@TestPropertySource(properties = {"spring.cloud.image-url=https://storage.googleapis.com/p2p-images/"})
class ImageServiceImplTest {

    @MockBean
    Storage storage;

    @Autowired
    ImageServiceImpl imageService;

    @MockBean
    ImageValidator imageValidator;

    @MockBean
    ImageAnnotatorClient visionClient;

    @MockBean
    private ImageDumpRepository imageDumpRepository;


    String BUCKET_NAME = "p2p-images";

    String PRODUCT_FOLDER_NAME = "product/";

    String imageUrl = "https://storage.googleapis.com/p2p-images/";
    String URL_1 = "https://storage.googleapis.com/test-bucket/product/image1.png";
    String URL_2 = "https://storage.googleapis.com/test-bucket/product/image2.png";

    @Test
    void testDeleteImage() {
        List<String> urls = new ArrayList<>();
        urls.add(URL_1);
        urls.add(URL_2);

        imageService.deleteImage(urls);
        verify(storage).delete(BlobId.of(BUCKET_NAME, PRODUCT_FOLDER_NAME + "image1.png"));
        verify(storage).delete(BlobId.of(BUCKET_NAME, PRODUCT_FOLDER_NAME + "image2.png"));
    }

    @Test
    void testDeleteImageWithException() {
        List<String> urls = new ArrayList<>();
        urls.add(URL_1);

        Mockito.when(storage.delete(any(BlobId.class))).thenThrow(new StorageException(new IOException()));

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> imageService.deleteImage(urls));
        String expectedErrorMessage =  "No image in folder";
        assertEquals(expectedErrorMessage, ex.getMessage());
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void testDeleteImageWithUrlException() {
        List<String> urls = new ArrayList<>();
        urls.add("img");

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> imageService.deleteImage(urls));
        String expectedErrorMessage =  "Invalid URL";
        assertEquals(expectedErrorMessage, ex.getMessage());
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void uploadImageWithException() throws Exception{
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        Mockito.when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
        Mockito.when(mockFile.getBytes()).thenThrow(new IOException());

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> imageService.uploadImage(mockFile));
        String expectedErrorMessage = "Error in uploading the image file.";
        assertEquals(expectedErrorMessage, ex.getMessage());
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());

    }


    @Test
    void testGetImage() {
        Blob blob1 = mock(Blob.class);
        when(blob1.getName()).thenReturn("home-banner/image1.jpg");
        Page<Blob> blobs = Mockito.mock(Page.class);
        when(storage.list(any(String.class), any(Storage.BlobListOption.class))).thenReturn(blobs);
        when(blobs.iterateAll()).thenReturn(List.of(blob1));
        String expectedUrl = imageUrl + "home-banner/image1.jpg";
        String actualUrl = imageService.getImage();
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void testGetImageWithException() {
        Blob blob1 = mock(Blob.class);
        when(blob1.getName()).thenReturn("home-banner/");
        Page<Blob> blobs = Mockito.mock(Page.class);
        when(storage.list(any(String.class), any(Storage.BlobListOption.class))).thenReturn(blobs);
        when(blobs.iterateAll()).thenReturn(List.of(blob1));

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> imageService.getImage());
        String expectedErrorMessage =  "No image in folder";
        assertEquals(expectedErrorMessage, ex.getMessage());
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }


    @Test
    void testDetectExplicitContent(){

        SafeSearchAnnotation safeSearchAnnotation = Mockito.mock(SafeSearchAnnotation.class);
        AnnotateImageResponse annotateImageResponse = Mockito.mock(AnnotateImageResponse.class);
        BatchAnnotateImagesResponse batchAnnotateImagesResponse = Mockito.mock(BatchAnnotateImagesResponse.class);
        byte[] imageBytes = "HI".getBytes();

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchAnnotateImagesResponse);

        Mockito.when(batchAnnotateImagesResponse.getResponses(0)).thenReturn(annotateImageResponse);

        Mockito.when(annotateImageResponse.getSafeSearchAnnotation()).thenReturn(safeSearchAnnotation);
        Mockito.when(safeSearchAnnotation.getAdult()).thenReturn(Likelihood.LIKELY);
        Mockito.when(safeSearchAnnotation.getMedical()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getSpoof()).thenReturn(Likelihood.VERY_UNLIKELY);
        Mockito.when(safeSearchAnnotation.getViolence()).thenReturn(Likelihood.POSSIBLE);
        Mockito.when(safeSearchAnnotation.getRacy()).thenReturn(Likelihood.VERY_LIKELY);


        List<String> result = imageService.detectExplicitContent(imageBytes);

        List<String> expected = new ArrayList<>();
        expected.add(Likelihood.LIKELY.toString());
        expected.add(Likelihood.UNLIKELY.toString());
        expected.add(Likelihood.VERY_UNLIKELY.toString());
        expected.add(Likelihood.POSSIBLE.toString());
        expected.add(Likelihood.VERY_LIKELY.toString());

        assertEquals(expected, result);

        Mockito.verify(visionClient).batchAnnotateImages(Mockito.anyList());
    }

    @Test
    void uploadImage(){

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());

        SafeSearchAnnotation safeSearchAnnotation = Mockito.mock(SafeSearchAnnotation.class);
        AnnotateImageResponse annotateImageResponse = Mockito.mock(AnnotateImageResponse.class);
        BatchAnnotateImagesResponse batchAnnotateImagesResponse = Mockito.mock(BatchAnnotateImagesResponse.class);

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchAnnotateImagesResponse);

        Mockito.when(batchAnnotateImagesResponse.getResponses(0)).thenReturn(annotateImageResponse);

        Mockito.when(annotateImageResponse.getSafeSearchAnnotation()).thenReturn(safeSearchAnnotation);
        Mockito.when(safeSearchAnnotation.getAdult()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getMedical()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getSpoof()).thenReturn(Likelihood.VERY_UNLIKELY);
        Mockito.when(safeSearchAnnotation.getViolence()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getRacy()).thenReturn(Likelihood.VERY_UNLIKELY);

        String url = imageService.uploadImage(file);

        verify(storage).create(any(BlobInfo.class), any(byte[].class));
        assertNotNull(url);
        assertTrue(url.startsWith(imageUrl+PRODUCT_FOLDER_NAME));
        assertTrue(url.endsWith("test.jpg"));
        Mockito.verify(visionClient).batchAnnotateImages(Mockito.anyList());
        Assertions.assertDoesNotThrow(() -> imageValidator.validate(file));
    }


    @Test
    void uploadImageWithExplicitContent(){

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());

        SafeSearchAnnotation safeSearchAnnotation = Mockito.mock(SafeSearchAnnotation.class);
        AnnotateImageResponse annotateImageResponse = Mockito.mock(AnnotateImageResponse.class);
        BatchAnnotateImagesResponse batchAnnotateImagesResponse = Mockito.mock(BatchAnnotateImagesResponse.class);

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchAnnotateImagesResponse);

        Mockito.when(batchAnnotateImagesResponse.getResponses(0)).thenReturn(annotateImageResponse);

        Mockito.when(annotateImageResponse.getSafeSearchAnnotation()).thenReturn(safeSearchAnnotation);
        Mockito.when(safeSearchAnnotation.getAdult()).thenReturn(Likelihood.LIKELY);
        Mockito.when(safeSearchAnnotation.getMedical()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getSpoof()).thenReturn(Likelihood.VERY_LIKELY);
        Mockito.when(safeSearchAnnotation.getViolence()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getRacy()).thenReturn(Likelihood.VERY_LIKELY);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> imageService.uploadImage(file));
        String expectedErrorMessage =  "Image contains Sensitive Content";
        assertEquals(expectedErrorMessage, ex.getMessage());
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());

    }

    @Test
    void uploadImageWithExplicitContentImage(){

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());

        SafeSearchAnnotation safeSearchAnnotation = Mockito.mock(SafeSearchAnnotation.class);
        AnnotateImageResponse annotateImageResponse = Mockito.mock(AnnotateImageResponse.class);
        BatchAnnotateImagesResponse batchAnnotateImagesResponse = Mockito.mock(BatchAnnotateImagesResponse.class);

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchAnnotateImagesResponse);

        Mockito.when(batchAnnotateImagesResponse.getResponses(0)).thenReturn(annotateImageResponse);

        Mockito.when(annotateImageResponse.getSafeSearchAnnotation()).thenReturn(safeSearchAnnotation);
        Mockito.when(safeSearchAnnotation.getAdult()).thenReturn(Likelihood.VERY_LIKELY);
        Mockito.when(safeSearchAnnotation.getMedical()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getSpoof()).thenReturn(Likelihood.VERY_LIKELY);
        Mockito.when(safeSearchAnnotation.getViolence()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getRacy()).thenReturn(Likelihood.VERY_LIKELY);

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> imageService.uploadImage(file));
        String expectedErrorMessage =  "Image contains Sensitive Content";
        assertEquals(expectedErrorMessage, ex.getMessage());
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());

    }

    @Test
    void uploadImageWithExceptionUpload(){

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());

        SafeSearchAnnotation safeSearchAnnotation = Mockito.mock(SafeSearchAnnotation.class);
        AnnotateImageResponse annotateImageResponse = Mockito.mock(AnnotateImageResponse.class);
        BatchAnnotateImagesResponse batchAnnotateImagesResponse = Mockito.mock(BatchAnnotateImagesResponse.class);

        when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchAnnotateImagesResponse);

        Mockito.when(batchAnnotateImagesResponse.getResponses(0)).thenReturn(annotateImageResponse);

        Mockito.when(annotateImageResponse.getSafeSearchAnnotation()).thenReturn(safeSearchAnnotation);
        Mockito.when(safeSearchAnnotation.getAdult()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getMedical()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getSpoof()).thenReturn(Likelihood.VERY_UNLIKELY);
        Mockito.when(safeSearchAnnotation.getViolence()).thenReturn(Likelihood.UNLIKELY);
        Mockito.when(safeSearchAnnotation.getRacy()).thenReturn(Likelihood.VERY_UNLIKELY);

        Mockito.when(storage.create(any(BlobInfo.class), any(byte[].class))).thenThrow(new StorageException(new IOException()));

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> imageService.uploadImage(file));
        String expectedErrorMessage = "Error in uploading the image file.";
        assertEquals(expectedErrorMessage, ex.getMessage());
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());

    }

    @Test
    void testDeleteImageFromDumpImage() {
        List<String> urls = new ArrayList<>();
        urls.add(URL_1);
        urls.add(URL_2);

        String msg = imageService.deleteImageFromDumpImage(urls);
        verify(imageDumpRepository).deleteAllByUrl(urls);
        String expectedMessage = "Removed images from dump repository.";
        assertEquals(expectedMessage, msg);
    }

}
