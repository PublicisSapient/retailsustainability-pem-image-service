package com.publicis.sapient.p2p.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServiceResponseDto {
    private int statusCode;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;
}