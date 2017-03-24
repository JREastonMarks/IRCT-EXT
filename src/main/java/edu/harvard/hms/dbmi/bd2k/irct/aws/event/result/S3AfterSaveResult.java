/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package edu.harvard.hms.dbmi.bd2k.irct.aws.event.result;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

import edu.harvard.hms.dbmi.bd2k.irct.event.action.AfterExecutionPlan;
import edu.harvard.hms.dbmi.bd2k.irct.exception.ResourceInterfaceException;
import edu.harvard.hms.dbmi.bd2k.irct.executable.Executable;
import edu.harvard.hms.dbmi.bd2k.irct.executable.ExecutableStatus;
import edu.harvard.hms.dbmi.bd2k.irct.model.result.Job;
import edu.harvard.hms.dbmi.bd2k.irct.model.security.SecureSession;

/**
 * Copies the results of an execution plan to an AWS S3 bucket. The bucket is
 * name is set by the Bucket Name parameter. The Executable Status must be set
 * to COMPLETED in order for the result to be saved remotely.
 * 
 * @author Jeremy R. Easton-Marks
 *
 */
public class S3AfterSaveResult implements AfterExecutionPlan {
	private AmazonS3 s3client;
	private Log log;
	private String bucketName;
	private String s3Folder;

	@Override
	public void init(Map<String, String> parameters) {
		log = LogFactory.getLog("AWS S3 Monitoring");
		bucketName = parameters.get("Bucket Name");
		s3Folder = parameters.get("s3Folder");

		s3client = new AmazonS3Client(new InstanceProfileCredentialsProvider());
	}

	@Override
	public void fire(SecureSession session, Executable executable) {

		try {
			if (executable.getStatus() != ExecutableStatus.COMPLETED) {
				return;
			}

			Job result = executable.getResults();
			for (File resultFile : result.getData().getFileList()) {
				String keyName = s3Folder + result.getId() + "/"
						+ resultFile.getName();
				// Copy the result into S3 if bucketName is not empty or null
				s3client.putObject(new PutObjectRequest(bucketName, keyName,
						resultFile));
				log.info("Moved " + result.getResultSetLocation() + " to "
						+ bucketName + "/" + keyName);
				// Delete File
				resultFile.delete();
				log.info("Deleted " + resultFile.getName());
			}
			result.setResultSetLocation("S3://" + s3Folder + result.getId());

		} catch (AmazonServiceException ase) {
			log.warn("Caught an AmazonServiceException, which "
					+ "means your request made it "
					+ "to Amazon S3, but was rejected with an error response"
					+ " for some reason.");
			log.warn("Error Message:    " + ase.getMessage());
			log.warn("HTTP Status Code: " + ase.getStatusCode());
			log.warn("AWS Error Code:   " + ase.getErrorCode());
			log.warn("Error Type:       " + ase.getErrorType());
			log.warn("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			log.warn("Caught an AmazonClientException, which "
					+ "means the client encountered "
					+ "an internal error while trying to "
					+ "communicate with S3, "
					+ "such as not being able to access the network.");
			log.warn("Error Message: " + ace.getMessage());
		} catch (ResourceInterfaceException e) {
			log.warn("Error Message: " + e.getMessage());
		}
	}

}
