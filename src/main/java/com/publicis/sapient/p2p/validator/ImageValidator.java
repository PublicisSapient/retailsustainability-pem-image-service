package com.publicis.sapient.p2p.validator;

import com.publicis.sapient.p2p.exception.BusinessException;
import com.publicis.sapient.p2p.exception.util.ErrorCode;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;


@Component
public class ImageValidator {

    private final Logger logger = LoggerFactory.getLogger(ImageValidator.class);


    @Value("${spring.cloud.image.minSize}")
    private Long minSize;

    @Value("${spring.cloud.image.maxSize}")
    private Long maxSize;


    public void validate(MultipartFile file) {

        logger.info("Entered in validate method in ImageValidator");
        List<String> content = new ArrayList<>();
        content.add("image/jpeg");
        content.add("image/png");

        if (!Objects.requireNonNull(file.getOriginalFilename()).matches("(?i)[^.]+\\.(jpg|jpeg|png)")) {
            logger.error("Please select an image file. (.jpg, .jpeg, .png)");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Please select an image file. (.jpg, .jpeg, .png)");
        }
        if (file.getSize() < minSize || file.getSize() > maxSize) {
            logger.error("File size is too large or small.");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File size is too large or small.");
        }

        String contentType = "";
        try {
            Tika tika = new Tika();
            contentType = tika.detect(file.getBytes());
            InputStream imageInputStream = file.getInputStream();
            BufferedImage image = ImageIO.read(imageInputStream);
            if(image==null)
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Error");
        } catch (Exception e) {
            logger.error("Error in reading contents of file.");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Error in reading contents of file.");
        }

        if (!content.contains(contentType) || !content.contains(Objects.requireNonNull(file.getContentType()))) {
            logger.error("Invalid File. Please upload an image file.");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid File. Please upload an image file.");
        }
    }
}
