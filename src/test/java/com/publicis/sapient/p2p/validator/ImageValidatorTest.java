package com.publicis.sapient.p2p.validator;

import com.publicis.sapient.p2p.exception.BusinessException;
import com.publicis.sapient.p2p.exception.util.ErrorCode;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {ImageValidator.class})
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"spring.cloud.image.minSize=5"})
@TestPropertySource(properties = {"spring.cloud.image.maxSize=50"})
class ImageValidatorTest {


    @Autowired
    ImageValidator imageValidator;


    @Test
    void testValidateWithInvalidFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "image.pdf", "application/pdf", "test data".getBytes());

        String expectedErrorMsg = "Please select an image file. (.jpg, .jpeg, .png)";


        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> imageValidator.validate(file));

        Assertions.assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        Assertions.assertEquals(expectedErrorMsg, exception.getMessage());
    }

    @Test
    void testValidateWithInvalidFileSize() {

        MultipartFile mockFile = Mockito.mock(MultipartFile.class);

        String expectedErrorMsg = "File size is too large or small.";

        when(mockFile.getSize()).thenReturn(20000L);
        when(mockFile.getOriginalFilename()).thenReturn("image.jpg");

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> imageValidator.validate(mockFile));

        Assertions.assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        Assertions.assertEquals(expectedErrorMsg, exception.getMessage());
    }


    @Test
    void testValidateWithInvalidSmallFileSize() {

        MultipartFile mockFile = Mockito.mock(MultipartFile.class);

        String expectedErrorMsg = "File size is too large or small.";

        when(mockFile.getSize()).thenReturn(2L);
        when(mockFile.getOriginalFilename()).thenReturn("image.jpg");

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> imageValidator.validate(mockFile));

        Assertions.assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        Assertions.assertEquals(expectedErrorMsg, exception.getMessage());
    }

    @Test
    void testValidateWithContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "application/pdf", "test data".getBytes());

        String expectedErrorMsg = "Error in reading contents of file.";


        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> imageValidator.validate(file));

        Assertions.assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        Assertions.assertEquals(expectedErrorMsg, exception.getMessage());
    }

    @Test
    void testValidateWithContentTypeIOException() throws IOException {

        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        String expectedErrorMsg = "Error in reading contents of file.";
        when(mockFile.getSize()).thenReturn(15L);
        when(mockFile.getOriginalFilename()).thenReturn("image.jpg");
        when(mockFile.getBytes()).thenThrow(new IOException());

        BusinessException exception = Assertions.assertThrows(BusinessException.class, () -> imageValidator.validate(mockFile));

        Assertions.assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        Assertions.assertEquals(expectedErrorMsg, exception.getMessage());
    }

    @Test
    void testValidate() throws IOException {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getSize()).thenReturn(15L);
        when(mockFile.getOriginalFilename()).thenReturn("image.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        String url = "https://source.unsplash.com/user/c_v_r/500x500";
        byte[] fileContent = IOUtils.toByteArray(new URL(url));
        when(mockFile.getBytes()).thenReturn(fileContent);
        when(mockFile.getInputStream()).thenReturn(new URL(url).openStream());

        Assertions.assertDoesNotThrow(() -> imageValidator.validate(mockFile));
    }

    @Test
    void testValidateContent() throws IOException {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getSize()).thenReturn(15L);
        when(mockFile.getOriginalFilename()).thenReturn("image.jpg");
        when(mockFile.getContentType()).thenReturn("image/pdf");
        String url = "https://source.unsplash.com/user/c_v_r/500x500";
        byte[] fileContent = IOUtils.toByteArray(new URL(url));
        when(mockFile.getBytes()).thenReturn(fileContent);
        when(mockFile.getInputStream()).thenReturn(new URL(url).openStream());

        Assertions.assertThrows(BusinessException.class, () -> imageValidator.validate(mockFile));
    }
}
