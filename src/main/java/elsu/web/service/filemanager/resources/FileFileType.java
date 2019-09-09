package elsu.web.service.filemanager.resources;

import java.sql.Timestamp;
import java.util.*;

public class FileFileType {
 
	private long fileId;
	private String fileName;
	private String isPublic;
	private String mimeType;
	private java.util.Date dateUpdated;
		
	public long getFileId() {
		return this.fileId;
	}
	public void setFileId(long id) {
		this.fileId = id;
	}
	
	public String getFileName() {
		return this.fileName;
	}
	public void setFileName(String name) {
		this.fileName = name;
	}
	
	public String getIsPublic() {
		return this.isPublic;
	}
	public void setIsPublic(String isPublic) {
		this.isPublic = isPublic;
	}
	
	public String getMimeType() {
		return this.mimeType;
	}
	public void setMimeType(String type) {
		this.mimeType = type;
	}
	
	public java.util.Date getDateUpdated() {
		return this.dateUpdated;
	}
	public java.sql.Timestamp getSQLDateUpdated() {
		return new Timestamp(this.dateUpdated.getTime());
	}
	public void setDateUpdated(java.util.Date date) {
		this.dateUpdated = date;
	}
	public void setDateUpdated(java.sql.Timestamp date) {
		this.dateUpdated = date;
	}
}
