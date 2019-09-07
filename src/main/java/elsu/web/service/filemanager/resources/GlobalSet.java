package elsu.web.service.filemanager.resources;

import java.io.*;
import java.util.*;
//import java.util.concurrent.locks.*;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GlobalSet extends KeyPairType implements Serializable {

	private static final long serialVersionUID = -6923670726367104454L;
	//private final Lock lock = new ReentrantLock();

	public void lock() {
		//lock.lock();
	}

	public void tryLock() {
		//lock.tryLock();
	}

	public void unlock() {
		//lock.unlock();
	}
}
