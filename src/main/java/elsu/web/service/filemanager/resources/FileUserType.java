package elsu.web.service.filemanager.resources;

import java.sql.*;
import java.util.*;

public class FileUserType {
 
	private long userId;
	private String userName;
	private ArrayList<FileFileType> files = new ArrayList<>();
	
	public long getUserId() {
		return this.userId;
	}
	public void setUserId(long id) {
		this.userId = id;
	}
	
	public String getUserName() {
		return this.userName;
	}
	public void setUserName(String name) {
		this.userName = name;
	}
	
	public ArrayList<FileFileType> getFileList() {
		return this.files;
	}
	public void setFileList(ArrayList<FileFileType> files) {
		this.files = files;
	}
	public void appendFileList(ArrayList<FileFileType> files) {
		boolean found = false;
		
		for(FileFileType file : files) {
			found = false;
			
			for(FileFileType cFile : this.files) {
				if (cFile.getFileName().equals(file.getFileName())) {
					found = true;
				}
			}
			
			if (!found) {
				this.files.add(file);
			}
		}
	}
	public void clearFileList() {
		this.files.clear();
	}
}
