package com.skytree.epubtest;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.util.Log;

import com.skytree.epub.ContentListener;
import com.skytree.epubtest.IOUtils;

class EpubHandler implements ContentListener {
	ZipFile zipFile = null;
	
	public boolean isCustomFont(String contentPath) {
		if (contentPath.startsWith("/fonts")) {
			return true;
		}
		return false;
	}
	public long getFileLength(String baseDirectory,String contentPath) {
		String path = baseDirectory + "/" + contentPath;
		File file = new File(path);
		if (file.exists()) {
			return file.length(); 
		}
		else {
			return 0;
		}
	}
	
	public boolean isFileExists(String baseDirectory,String contentPath) {		
		String path = baseDirectory +"/"+ contentPath;
		File file = new File(path);
		boolean res = false;
		if (file.exists()) {
			res =  true;
		}
		else {
			res =  false;
		}
		return res;		
	}
	
	public long getLastModifiedForFile(String baseDirectory,String contentPath) {
		String path = baseDirectory + "/" + contentPath;
		File file = new File(path);
		if (file.exists()) {
			return file.lastModified();
		}
		else {
			return 0;		
		}
	}
	
	public InputStream getInputStreamForFile(String baseDirectory,String contentPath) {
		String path = baseDirectory + "/" + contentPath;
		File file = new File(path);
		try {
			FileInputStream fis = new FileInputStream(file);
			return fis;
		}catch(Exception e) {
			return null;
		}		
	}
	
	public void setupZipFile(String baseDirectory,String contentPath) {
		if (zipFile!=null) return;
		String[] subDirs = contentPath.split(Pattern.quote(File.separator));
		String fileName = subDirs[1]+".epub";
		String filePath = baseDirectory+"/"+subDirs[1]+"/"+fileName;		
		try {
			File file = new File(filePath);
			zipFile = new ZipFile(file);			
		}catch(Exception e) {
			zipFile=null;
		}		
	}
	
	// Entry name should start without / like META-INF/container.xml 
	public ZipEntry getZipEntry(String contentPath) {
		if (zipFile==null) return null;
		String[] subDirs = contentPath.split(Pattern.quote(File.separator));
		String corePath = contentPath.replace(subDirs[1], "");
		corePath=corePath.replace("//", "");
		ZipEntry entry = zipFile.getEntry(corePath.replace(File.separatorChar, '/'));
		return entry;
	}

	public long getLength(String baseDirectory,String contentPath) {
		if (this.isCustomFont(contentPath)) {
			return this.getFileLength(baseDirectory, contentPath);
		}
		ZipEntry entry = this.getZipEntry(contentPath);
		if (entry==null) return 0;
		long length = entry.getSize();
		return length;
	}
	
	public boolean isExists(String baseDirectory,String contentPath) {	
		setupZipFile(baseDirectory,contentPath);
		if (this.isCustomFont(contentPath)) {
			return this.isFileExists(baseDirectory, contentPath);
		}
		
		ZipEntry entry = this.getZipEntry(contentPath);
		if (entry==null) {
//			Log.w("EPub",contentPath+" not exist");
		}
		if (contentPath.contains("mp4")) {
//			Log.w("EPub",contentPath);
		}
		if (entry==null) return false;
		else return true;		
	}
	
	public long getLastModified(String baseDirectory,String contentPath) {
		if (this.isCustomFont(contentPath)) {
			return this.getLastModifiedForFile(baseDirectory, contentPath);
		}

		ZipEntry entry = this.getZipEntry(contentPath);		
		if (entry==null) return 0;
		return entry.getTime();
	}
	
	public InputStream getInputStream(String baseDirectory,String contentPath) {
		if (this.isCustomFont(contentPath)) {
			return this.getInputStreamForFile(baseDirectory, contentPath);
		}

		InputStream is = null;
		try {
			ZipEntry entry = this.getZipEntry(contentPath);
			if (entry==null) return null;
			is = zipFile.getInputStream(entry);
			// in some zip format, zipEntry can't generates proper inputStream, 
			// to fix this, byteArrayInputStream is used instead of zipEntry.getInputStream. 
			if (is.available()==1) {
				BufferedInputStream bis = new BufferedInputStream(is);  
				int file_size  = (int) entry.getCompressedSize();  
				byte[] blob = new byte[(int) entry.getCompressedSize()];  
				int bytes_read = 0;  
				int offset = 0;  
				while((bytes_read = bis.read(blob, 0, file_size)) != -1) {  
					offset += bytes_read;  
				} 
				bis.close();
				ByteArrayInputStream bas = new ByteArrayInputStream(blob);
				return bas;
			}
			return is;			
		}catch(Exception e) {
		}
		return is;
	}
}