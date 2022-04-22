package com.forum.community.util;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component
public class S3Util {

    private static final Logger logger = LoggerFactory.getLogger(S3Util.class);

    @Autowired
    private AmazonS3 amazonS3Client;

    public String uploadFile(String fileName, MultipartFile file, String bucketName){
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            amazonS3Client.putObject(bucketName,fileName,file.getInputStream(),metadata);
            return "File uploaded";
        } catch (IOException ioe) {
            logger.error("IOException: " + ioe.getMessage());
        } catch (AmazonServiceException serviceException) {
            logger.info("AmazonServiceException: "+ serviceException.getMessage());
            throw serviceException;
        } catch (AmazonClientException clientException) {
            logger.info("AmazonClientException Message: " + clientException.getMessage());
            throw clientException;
        }
        return "File not uploaded: " + fileName;
    }

    public String uploadFile(String fileName, File file, String bucketName){
        try {
            amazonS3Client.putObject(bucketName,fileName,file);
            return "File uploaded";
        }  catch (AmazonServiceException serviceException) {
            logger.info("AmazonServiceException: "+ serviceException.getMessage());
            throw serviceException;
        } catch (AmazonClientException clientException) {
            logger.info("AmazonClientException Message: " + clientException.getMessage());
            throw clientException;
        }

    }
}