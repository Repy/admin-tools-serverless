package info.repy.tools.controller.config;

import lombok.Data;

import java.util.List;

@Data
public class Root {
    private List<DbBatchConfig> dbBatch;
    private List<DbCsvConfig> dbCsv;
    private S3UploadConfig s3Upload;
    private List<DynamoUploadConfig> dynamoUpload;
}