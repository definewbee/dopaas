/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.devops.coss.hdfs;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.IOUtils;

import com.wl4g.devops.coss.AbstractCossEndpoint;
import com.wl4g.devops.coss.exception.ServerCossException;
import com.wl4g.devops.coss.hdfs.config.HdfsCossProperties;
import com.wl4g.devops.coss.model.ACL;
import com.wl4g.devops.coss.model.AccessControlList;
import com.wl4g.devops.coss.model.ObjectAcl;
import com.wl4g.devops.coss.model.ObjectListing;
import com.wl4g.devops.coss.model.ObjectMetadata;
import com.wl4g.devops.coss.model.ObjectSymlink;
import com.wl4g.devops.coss.model.ObjectValue;
import com.wl4g.devops.coss.model.Owner;
import com.wl4g.devops.coss.model.PutObjectResult;
import com.wl4g.devops.coss.model.bucket.Bucket;
import com.wl4g.devops.coss.model.bucket.BucketList;
import com.wl4g.devops.coss.model.bucket.BucketMetadata;
import static com.wl4g.devops.coss.utils.PosixFileSystemUtils.*;
import static com.wl4g.devops.tool.common.io.FileSizeUtils.*;

public class HdfsCossEndpoint extends AbstractCossEndpoint<HdfsCossProperties> {

	/**
	 * {@link FileSystem}
	 */
	protected FileSystem hdfs;

	public HdfsCossEndpoint(HdfsCossProperties config) {
		super(config);
		checkAndInitializingHdfsFileSystem();
	}

	@Override
	public CossProvider kind() {
		return CossProvider.Hdfs;
	}

	@Override
	public boolean preHandle(Object[] args) {
		checkAndInitializingHdfsFileSystem();
		return true;
	}

	@Override
	public Bucket createBucket(String bucketName) {
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName);
			hdfs.mkdirs(path, DEFAULT_BUCKET_PERMISSION);
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
		Bucket bucket = new Bucket(bucketName);
		bucket.setCreationDate(new Date());
		bucket.setOwner(getCurrentOwner());
		return bucket;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public BucketList listBuckets(String prefix, String marker, Integer maxKeys) {
		BucketList<Bucket> bucketList = new BucketList<>();
		try {
			FileStatus[] fileStats = hdfs.listStatus(config.getBucketRootPath());
			if (!isNull(fileStats)) {
				for (FileStatus fileStat : fileStats) {
					if (fileStat.isDirectory()) {
						Bucket bucket = new Bucket(fileStat.getPath().getName());
						// TODO CreationDate?
						bucket.setCreationDate(null);
						bucket.setOwner(new Owner(fileStat.getOwner(), fileStat.getOwner()));
						bucketList.getBucketList().add(bucket);
					}
				}
			}
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
		return bucketList;
	}

	@Override
	public void deleteBucket(String bucketName) {
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName);
			// TODO logisic delete?
			hdfs.delete(path, true);
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
	}

	@Override
	public BucketMetadata getBucketMetadata(String bucketName) {
		BucketMetadata metadata = new BucketMetadata(bucketName);
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName);
			FileStatus fileStat = hdfs.getFileStatus(path);
			if (!isNull(fileStat)) {
				// TODO default is private hdfs server
				metadata.setBucketRegion("private-hdfs");
				metadata.getAttributes().put("blockSize", getHumanReadable(fileStat.getBlockSize()));
				metadata.getAttributes().put("owner", fileStat.getOwner());
				metadata.getAttributes().put("group", fileStat.getGroup());
				metadata.getAttributes().put("len", getHumanReadable(fileStat.getLen()));
			}
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
		return metadata;
	}

	@Override
	public AccessControlList getBucketAcl(String bucketName) {
		AccessControlList acl = new AccessControlList();
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName);
			FileStatus fileStat = hdfs.getFileStatus(path);
			if (!isNull(fileStat)) {
				acl.setOwner(new Owner(fileStat.getOwner(), fileStat.getOwner()));
				acl.setAcl(toAcl(fileStat.getPermission()));
			}
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
		return acl;
	}

	@Override
	public void setBucketAcl(String bucketName, ACL acl) {
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName);
			hdfs.setPermission(path, toFsPermission(acl));
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ObjectListing listObjects(String bucketName) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ObjectListing listObjects(String bucketName, String prefix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectValue getObject(String bucketName, String key) {
		ObjectValue value = new ObjectValue(key, bucketName);
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName + "/" + key);
			FileStatus fileStat = hdfs.getFileStatus(path);
			if (!isNull(fileStat)) {
				value.getMetadata().setAtime(fileStat.getAccessTime());
				value.getMetadata().setMtime(fileStat.getModificationTime());
				value.getMetadata().setContentLength(fileStat.getLen());
				value.getMetadata().getUserMetadata().put("blockSize", getHumanReadable(fileStat.getBlockSize()));
				value.getMetadata().getUserMetadata().put("owner", fileStat.getOwner());
				value.getMetadata().getUserMetadata().put("group", fileStat.getGroup());
				// Checksum
				FileChecksum checkSum = hdfs.getFileChecksum(path);
				if (!isNull(checkSum)) {
					value.getMetadata()
							.setEtag(checkSum.getChecksumOpt().getBytesPerChecksum() + "@" + checkSum.getAlgorithmName());
				}
				value.getMetadata().setVersionId(null); // TODO
			}
			FSDataInputStream input = hdfs.open(path);
			value.setObjectContent(input);
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
		return value;
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata) {
		PutObjectResult result = new PutObjectResult();
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName + "/" + key);
			// TODO Existed check?
			FSDataOutputStream output = hdfs.create(path, true);
			IOUtils.copyBytes(input, output, DEFAULT_WRITE_BUFFER, true);
			// Sets permission
			if (!isNull(metadata.getAcl())) {
				FsPermission fp = toFsPermission(metadata.getAcl());
				hdfs.setPermission(path, fp);
			}
			// Checksum
			FileChecksum checkSum = hdfs.getFileChecksum(path);
			result.setETag(checkSum.getChecksumOpt().getBytesPerChecksum() + "@" + checkSum.getAlgorithmName());
			result.setVersionId(null); // TODO
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
		return result;
	}

	@Override
	public void deleteObject(String bucketName, String key) {
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName + "/" + key);
			// TODO logisic delete and multiple version?
			hdfs.delete(path, true);
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
	}

	@Override
	public ObjectAcl getObjectAcl(String bucketName, String key) {
		ObjectAcl acl = new ObjectAcl();
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName + "/" + key);
			FileStatus fileStat = hdfs.getFileStatus(path);
			if (!isNull(fileStat)) {
				acl.setOwner(new Owner(fileStat.getOwner(), fileStat.getOwner()));
				acl.setAcl(toAcl(fileStat.getPermission()));
			}
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
		return acl;
	}

	@Override
	public void setObjectAcl(String bucketName, String key, ACL acl) {
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName + "/" + key);
			hdfs.setPermission(path, toFsPermission(acl));
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
	}

	@Override
	public boolean doesObjectExist(String bucketName, String key) {
		try {
			Path path = new Path(config.getBucketRootPath(), bucketName + "/" + key);
			return !hdfs.exists(path);
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
	}

	@Override
	public void createSymlink(String bucketName, String symlink, String target) {
		try {
			Path targetPath = new Path(config.getBucketRootPath(), bucketName + "/" + target);
			Path symlinkPath = new Path(config.getBucketRootPath(), bucketName + "/" + symlink);
			hdfs.createSymlink(targetPath, symlinkPath, true);
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
	}

	@Override
	public ObjectSymlink getSymlink(String bucketName, String symlink) {
		ObjectSymlink objSymlink = new ObjectSymlink();
		try {
			Path symlinkPath = new Path(config.getBucketRootPath(), bucketName + "/" + symlink);
			FileStatus fileStat = hdfs.getFileLinkStatus(symlinkPath);
			objSymlink.setSymlink(symlink);
			objSymlink.setTarget(fileStat.getSymlink().getName());

			objSymlink.getMetadata().setAtime(fileStat.getAccessTime());
			objSymlink.getMetadata().setMtime(fileStat.getModificationTime());
			objSymlink.getMetadata().setContentLength(fileStat.getLen());
			objSymlink.getMetadata().getUserMetadata().put("blockSize", getHumanReadable(fileStat.getBlockSize()));
			objSymlink.getMetadata().getUserMetadata().put("owner", fileStat.getOwner());
			objSymlink.getMetadata().getUserMetadata().put("group", fileStat.getGroup());
			// Checksum
			FileChecksum checkSum = hdfs.getFileChecksum(symlinkPath);
			if (!isNull(checkSum)) {
				objSymlink.getMetadata()
						.setEtag(checkSum.getChecksumOpt().getBytesPerChecksum() + "@" + checkSum.getAlgorithmName());
			}
			objSymlink.getMetadata().setVersionId(null); // TODO
		} catch (IOException e) {
			throw new ServerCossException(e);
		}
		return objSymlink;
	}

	/**
	 * Check hdfs {@link FileSystem} or re-initializing.
	 */
	protected void checkAndInitializingHdfsFileSystem() {
		// Check current FileSystem status
		try {
			hdfs.exists(new Path(config.getBucketRootHdfsUri()));
		} catch (IOException e1) {
			try {
				FileSystem.closeAll();
			} catch (IOException e) {
				log.error("Failed to close hdfs FileSystem", e);
			}
		}
		// Re-initializing FileSystem
		hdfs = getHdfsFileSystem(config);
	}

	/**
	 * Gets creation hdfs {@link FileSystem}
	 * 
	 * @param config
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected FileSystem getHdfsFileSystem(HdfsCossProperties config) {
		log.info("Creation hdfs filesystem for user '{}' with root URI: '{}'", config.getUser(), config.getBucketRootHdfsUri());
		try {
			return FileSystem.get(config.getBucketRootHdfsUri(), new Configuration(), config.getUser());
		} catch (IOException | InterruptedException e) {
			throw new ServerCossException(e);
		}
	}

	/**
	 * {@link FsPermission} to {@link Acl}
	 * 
	 * @param fp
	 * @return
	 */
	final public static ACL toAcl(FsPermission fp) {
		return toPosixAcl(fp.toShort());
	}

	/**
	 * {@link Acl} to {@link FsPermission}
	 * 
	 * @param acl
	 * @return
	 */
	final public static FsPermission toFsPermission(ACL acl) {
		int posixPermission = toPosixPermission(acl);
		for (FsPermission fp : ACL_PERMISSSIONS) {
			if (posixPermission == fp.toShort())
				return fp;
		}
		throw new IllegalStateException(format("Unkown acl: %s", acl));
	}

	/**
	 * {@link ACL} <=> POSIX permission.
	 */
	final public static FsPermission[] ACL_PERMISSSIONS = { new FsPermission(valueOf(ACL_PRIVATE_POSIX)),
			new FsPermission(valueOf(ACL_READ_POSIX)), new FsPermission(valueOf(ACL_READ_WRITE_POSIX)) };

	/**
	 * Bucket default permission.(755) {@link FsPermission}
	 */
	final public static FsPermission DEFAULT_BUCKET_PERMISSION = ACL_PERMISSSIONS[1];

}
