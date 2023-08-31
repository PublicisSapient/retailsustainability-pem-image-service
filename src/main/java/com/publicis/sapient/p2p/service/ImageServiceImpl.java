package com.publicis.sapient.p2p.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.publicis.sapient.p2p.exception.BusinessException;
import com.publicis.sapient.p2p.exception.util.ErrorCode;
import com.publicis.sapient.p2p.model.ImageDump;
import com.publicis.sapient.p2p.repository.ImageDumpRepository;
import com.publicis.sapient.p2p.validator.ImageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class ImageServiceImpl implements ImageService {

    private final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);

    @Autowired
    private Storage storage;

    @Autowired
    private ImageAnnotatorClient visionClient;

    @Autowired
    public ImageValidator imageValidator;

    @Autowired
    private ImageDumpRepository imageDumpRepository;

    @Value("${spring.cloud.bucket-name}")
    private String bucketName;

    @Value("${spring.cloud.folder-name-product}")
    private String productFolderName;

    @Value("${spring.cloud.folder-name-homeBanner}")
    private String homeBannerFolderName;
    @Value("${spring.cloud.image-url}")
    private String imageUrl;

    private static final String ERR_IN_UPLOAD = "Error in uploading the image file.";

    private static final String IMG_NOT_FOUND = "No image in folder";

    private static final String URL_NOT_FOUND = "Invalid URL";


    public String uploadImage(MultipartFile file) {
        logger.info("Entered in uploadImage method in ServiceImpl");
        imageValidator.validate(file);
        String url;
        String fileName = UUID.randomUUID() + "-" + Objects.requireNonNull(file.getOriginalFilename()).replaceAll("[/\\\\:*?\"<>|\\s]", "");
        BlobId blobId = BlobId.of(bucketName,productFolderName+ fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).setAcl(Collections.singletonList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))).build();
        byte[] fileBytes;
        List<String> result;

        logger.info("Checking Safe Search Detection");
        try {
            result = detectExplicitContent(file.getBytes());
        }catch (Exception ex) {
            logger.error(ERR_IN_UPLOAD);
            throw new BusinessException(ErrorCode.BAD_REQUEST, ERR_IN_UPLOAD);
        }
        if(result.contains("LIKELY") || result.contains("VERY_LIKELY")){
            logger.info("Sensitive Contents Detected");
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Image contains Sensitive Content");
        }
        else {
            try {

                logger.info("Uploading image to cloud.");
                fileBytes = file.getBytes();
                storage.create(blobInfo, fileBytes);
                logger.info("Uploaded images to cloud.");
                url = imageUrl + productFolderName + fileName;
            } catch (Exception ex) {
                logger.error(ERR_IN_UPLOAD);
                throw new BusinessException(ErrorCode.BAD_REQUEST, ERR_IN_UPLOAD);
            }
        }
        ImageDump imageDump = new ImageDump();
        imageDump.setUrl(url);
        imageDump.setCreatedTime(Timestamp.from(Instant.now()));
        imageDumpRepository.save(imageDump);
        return url;
    }


    public String deleteImage(List<String> urls) {
        logger.info("Entered in deleteImage method in ServiceImpl");
        for(String url : urls)
        {
            String name;
            try {
                name = new File(new URL(url).getPath()).getName();
            } catch (MalformedURLException e) {
                logger.error(URL_NOT_FOUND);
                throw new BusinessException(ErrorCode.BAD_REQUEST, URL_NOT_FOUND);
            }
            BlobId blobId = BlobId.of(bucketName, productFolderName + name);
            try {
                logger.info("Deleting images from cloud.");
                storage.delete(blobId);
            }
            catch (StorageException e) {
                logger.error(IMG_NOT_FOUND);
                throw new BusinessException(ErrorCode.NOT_FOUND, IMG_NOT_FOUND);
            }
        }
        logger.info("Deleted images from cloud.");
        return "Deleted images from cloud.";
    }

    public String getImage() {
        logger.info("Entered in getImage method in ServiceImpl");
        List<String> name = new ArrayList<>();
        Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(homeBannerFolderName));
        for (Blob blob : blobs.iterateAll()) {
            String imageName = blob.getName();
            if (imageName.matches("(?i).+\\.(jpg|jpeg|png)")) {
                name.add(imageUrl+imageName);
            }
        }
        if(name.isEmpty()){
            logger.error(IMG_NOT_FOUND);
            throw new BusinessException(ErrorCode.NOT_FOUND, IMG_NOT_FOUND);
        }
        else {
            logger.info("Image url of homebanner fetched");
            return name.get(0);
        }

    }

    @Override
    public String deleteImageFromDumpImage(List<String> urls) {
        logger.info("Entering deleteImageFromDumpImage method inside ImageServiceImpl");
        imageDumpRepository.deleteAllByUrl(urls);
        return "Removed images from dump repository.";
    }


    public List<String> detectExplicitContent(byte[] imageBytes){
        logger.info("Entered in detectExplicitContent method in ServiceImpl");
        ByteString imageByteString = ByteString.copyFrom(imageBytes);
        Image image = Image.newBuilder().setContent(imageByteString).build();
        Feature feature = Feature.newBuilder().setType(Feature.Type.SAFE_SEARCH_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(image)
                .build();
        logger.info("Checking Image with Vision API.");
        BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(List.of(request));
        logger.info("Results Fetched from Vision API.");
        AnnotateImageResponse annotateResponse = response.getResponses(0);
        List<String> result = new ArrayList<>();
        result.add(annotateResponse.getSafeSearchAnnotation().getAdult().toString());
        result.add(annotateResponse.getSafeSearchAnnotation().getMedical().toString());
        result.add(annotateResponse.getSafeSearchAnnotation().getSpoof().toString());
        result.add(annotateResponse.getSafeSearchAnnotation().getViolence().toString());
        result.add(annotateResponse.getSafeSearchAnnotation().getRacy().toString());
        return result;
    }

}
