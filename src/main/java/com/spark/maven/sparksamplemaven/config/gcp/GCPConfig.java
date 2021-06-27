package com.spark.maven.sparksamplemaven.config.gcp;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.*;
import com.google.cloud.storage.*;
import com.spark.maven.sparksamplemaven.util.FileUtil;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class GCPConfig {
    private FileUtil fileUtil;
    private Environment env;

    public GCPConfig(FileUtil fileUtil,
                     Environment env){
        this.fileUtil = fileUtil;
        this.env = env;
    }

    public synchronized GoogleCredentials googleCredentials(String credentials, String project){
        GoogleCredentials googleCredentials = null;
        try {
            String file_path = fileUtil.setGCPCredentialFile(credentials, project);
            FileInputStream fileInputStream = new FileInputStream(file_path);
            googleCredentials = ServiceAccountCredentials.fromStream(fileInputStream);
        } catch (Exception e){
            e.printStackTrace();
        }
        return googleCredentials;
    }

    public synchronized BigQuery setGoogleBigQuery(GoogleCredentials googleCredentials, String project){
        BigQuery bigquery = null;
        try {
            bigquery = BigQueryOptions.newBuilder().setCredentials(googleCredentials)
                    .setProjectId(project).build().getService();
        } catch (Exception e){
            e.printStackTrace();
        }
        return bigquery;
    }

    public synchronized Storage setGoogleStorage(GoogleCredentials googleCredentials){
        Storage storage = null;
        try {
            storage = StorageOptions.newBuilder().setCredentials(googleCredentials).build().getService();
        } catch (Exception e){
            e.printStackTrace();
        }
        return storage;
    }

    public synchronized void getBigQueryResult(BigQuery bigQuery, String chart_name, String query){
        String dataFormat = "JSON";
        String sourceUri = env.getProperty("gcp.storage.uri") + chart_name + "/" + chart_name + "_*.json";
        try {
            String qry =
                    String.format(
                            "EXPORT DATA OPTIONS(uri='%s', format='%s', overwrite=true) "
                                    + "AS \n %s",
                            sourceUri, dataFormat, query);
            QueryJobConfiguration queryPlan = QueryJobConfiguration.newBuilder(query).setDryRun(true).setUseQueryCache(false).build();
            JobId jobPlanId = JobId.of(UUID.randomUUID().toString());
            Job queryPlanJob = bigQuery.create(JobInfo.newBuilder(queryPlan).setJobId(jobPlanId).build());
            JobStatistics.QueryStatistics statistics = queryPlanJob.getStatistics();

            System.out.println(statistics.getSchema());

            QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(qry)
                    .setJobTimeoutMs(60000L).setUseLegacySql(false).build();
            JobId jobId = JobId.of(UUID.randomUUID().toString());
            Job queryJob = bigQuery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public synchronized void downloadBigQueryResult(Storage storage, String chart_name){
        String bucket = env.getProperty("gcp.bucket.name");
        String temp_path = env.getProperty("file.temps.path") + chart_name + "/";
        String prefix = "temps/"+ chart_name + "/";
        Page<Blob> blobPage = storage.list(bucket,
                Storage.BlobListOption.prefix(prefix),
                Storage.BlobListOption.currentDirectory());


        // check folder
        // delete folder
        // create folder
        fileUtil.createQueryFolder(chart_name);

        for (Blob blob : blobPage.iterateAll()){
            String[] blobName = blob.getName().split("/");
            String file_name = blobName[blobName.length-1];
            System.out.println(temp_path + file_name);
            blob.downloadTo(Paths.get(temp_path + file_name));
        }
    }

    public boolean deleteBigQueryResult(Storage storage, String chart_name){
        String bucket = env.getProperty("gcp.bucket.name");
        String prefix = "temps/"+ chart_name + "/";
        List<StorageBatchResult<Boolean>> results = new ArrayList<>();
        StorageBatch batch = storage.batch();
        try {
            Page<Blob> blobs = storage.list(bucket, Storage.BlobListOption.currentDirectory(),
                    Storage.BlobListOption.prefix(prefix));
            for(Blob blob : blobs.iterateAll()) {
                results.add(batch.delete(blob.getBlobId()));
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            batch.submit();
            return results.stream().allMatch(r -> r != null && r.get());
        }
    }
}
