package com.publicis.sapient.p2p.service;

import com.publicis.sapient.p2p.model.ImageDump;
import com.publicis.sapient.p2p.repository.ImageDumpRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {Scheduler.class})
@ExtendWith(SpringExtension.class)
class SchedulerTest {

    @MockBean
    ImageDumpRepository imageDumpRepository;

    @MockBean
    ImageServiceImpl imageServiceImpl;

    @Autowired
    Scheduler scheduler;

    @Test
    void testRemoveDumpImages() {
        List<ImageDump> imageDumps = new ArrayList<>();
        ImageDump img1 = new ImageDump( "1",Timestamp.from(Instant.now()),"url1");
        ImageDump img2 = new ImageDump("2", Timestamp.from(Instant.now().minusSeconds(31600)),"url2");
        imageDumps.add(img1);
        imageDumps.add(img2);

        when(imageDumpRepository.findAll()).thenReturn(imageDumps);
        scheduler.removeDumpImages();

        verify(imageServiceImpl).deleteImage(List.of("url2"));
        verify(imageDumpRepository).deleteAllByUrl(List.of("url2"));
    }

    @Test
    void testRemoveDumpImagesWithNoImage() {
        List<ImageDump> imageDumps = new ArrayList<>();
        when(imageDumpRepository.findAll()).thenReturn(imageDumps);
        scheduler.removeDumpImages();
        verify(imageDumpRepository).findAll();

    }


}
