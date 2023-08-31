package com.publicis.sapient.p2p.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ImageDump", description = "Images to be Deleted")
public class ImageDump {
    @Id
    @Schema(type = "String", description = "Auto-generated ID")
    private String id;

    @Schema(type = "Timestamp", description = "Timestamp when the image is uploaded")
    private Date createdTime;

    @Schema(type = "String", description = "Image url")
    private String url;
}
