package com.elanco.countrypopulation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Country implements Serializable {
    private String name;
    private String capital;
    private String flag;
    private Long population;
    private String region;
    private String subregion;
}