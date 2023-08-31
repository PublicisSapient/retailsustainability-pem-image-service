package com.publicis.sapient.p2p.service;

import com.publicis.sapient.p2p.model.ImageDump;
import com.publicis.sapient.p2p.repository.ImageDumpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@EnableScheduling
public class Scheduler {

    private final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Autowired
    private ImageDumpRepository imageDumpRepository;

    @Autowired
    private ImageServiceImpl imageService;

    @Scheduled(cron = "0 0 7 * * * ")
    public void removeDumpImages() {
        logger.info("Entering removeDumpImages method inside Scheduler");
        List<ImageDump> imageDumps = imageDumpRepository.findAll();
        List<String> urls = new ArrayList<>();
        for(ImageDump imageDump : imageDumps) {
            if(imageDump.getCreatedTime().before(Timestamp.from(Instant.now().minusSeconds(21600)))) {
                urls.add(imageDump.getUrl());
            }
        }
        if(!urls.isEmpty()) {
            imageService.deleteImage(urls);
            logger.info("removing image from image dump");
            imageDumpRepository.deleteAllByUrl(urls);
        }
    }
}
