package ru.sbt.edu_power.external_services.nexus;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AssetModel {
    private String downloadUrl;
    private String path;
    private String id;
    private String repository;
    private String format;
    private Date blobCreated;
    private String contentType;
    private Date lastDownloaded;
    private Date lastModified;
}
