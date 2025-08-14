package pureplus.jview;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.lang.ref.SoftReference;

class ImageList
{
	File							file;
	SoftReference<BufferedImage>	imgref;

	ImageList(File file) {
		this.file = file;
	}

	File getFile() {
		return file;
	}

	BufferedImage getImage() throws IOException {
		BufferedImage  resimg = null;

		if (imgref != null) {
			resimg = imgref.get();
		}

		if (resimg == null) {
			resimg = ImageIO.read(file);
			imgref = new SoftReference<BufferedImage>(resimg);
			System.out.println("loaded "+file.getName());
		}

		return resimg;	
	}
}

public class JViewLoader extends Thread
{
	int          index,load_idx;
	File         	arg_path;
	List<ImageList>	img_list;

	ArrayList<JViewLoadEventListener>  jdisplistener;

	private Object  syncmon = new Object();
	private boolean running;

	public JViewLoader(String path) {
		this.index = 0;
		this.arg_path = new File(path);
		this.img_list = null;

		jdisplistener = new ArrayList<JViewLoadEventListener>();
	}

	public void setIndex(int idx) {
		this.index = idx;
	}

	int nextIndex(int idx) {
			if (idx < img_list.size()-1) {
				return idx+1;
			} else {
				return 0;
			}
	}

	int prevIndex(int idx) {
			if (idx == 0) {
				return img_list.size()-1;
			} else {
				return idx-1;
			}
	}
	
	public void next() {
		if (img_list!=null) {
			this.index = nextIndex(this.index);
			//System.out.println("call next " + this.index + "/" + img_list.size());
			updateCurrent();
		}
	}

	public void previous() {
		if (img_list!=null) {
			this.index = prevIndex(this.index);
			//System.out.println("call prev " + this.index + "/" + img_list.size());
			updateCurrent();
		}
	}

	void updateCurrent() {
		synchronized(syncmon) {
			this.load_idx = this.index;
			syncmon.notify();
		}
	}

	/**
	 * load current image and display
	 * and load next / previous image if not loaded
	 */
	void loadCurrent() {
		if (img_list.size()>0) {
			loadImage(load_idx);
		
			if (img_list.size()>1) {
				loadImage(nextIndex(load_idx));
				loadImage(prevIndex(load_idx));
			}
		}
	}

	public void loadImage(int idx) {
		ImageList	ref = img_list.get(idx);
		try {
			Image		img = ref.getImage();

			if (idx==this.index) {
				//System.out.println("fire"+index+"/"+img_list.size());
				fireJViewLoadEvent(img);
			}
		} catch(IOException ex) {
			System.out.println("load error:" + ref.getFile().getPath());
			System.out.println(ex);
		}
	}

	private List<File> getAllImageFile(File dir) {
		File[]  full_filelist;
		ArrayList<File>   img_filelist = new ArrayList<File>();

		full_filelist = dir.listFiles();
		Arrays.sort(full_filelist);

		for (int i=0; i<full_filelist.length; i++) {
			File  file;
			file = full_filelist[i];
			if (file.isDirectory()) {
				List<File>  ch_list = getAllImageFile(file);
				img_filelist.addAll(ch_list);
			} else {
				String  fname = full_filelist[i].getName();

				if (fname.endsWith(".jpg") ||
					fname.endsWith(".png")) {
					img_filelist.add(full_filelist[i]);
				}
			}
		}
		return  img_filelist;
	}

	@Override
	public void run() {
		System.out.println("run with path="+arg_path);
		if (arg_path.isDirectory()) {
			List<File>  file_list = getAllImageFile(arg_path);

			img_list = new ArrayList<ImageList>(file_list.size());
			for (int i=0; i<file_list.size(); i++) {
				img_list.add(new ImageList(file_list.get(i)));
			}
		} else if (arg_path.isFile()) {
			img_list = new ArrayList<ImageList>(1);
			img_list.add(new ImageList(arg_path));
		}
		updateCurrent();

		try {
			this.running = true;
			while(this.running) {
				loadCurrent();
				synchronized(syncmon) {
					syncmon.wait();
				}
			}
		} catch(InterruptedException ex) {
			System.out.println(ex);
		}
	}

	void shutdown() {
		synchronized(syncmon) {
			this.running = false;
			syncmon.notify();
		}
	}


	public void addJViewLoadListener(JViewLoadEventListener l) {
		jdisplistener.add(l);
	}

	public void removeJViewLoadListener(JViewLoadEventListener l) {
		jdisplistener.remove(l);
	}

	public void fireJViewLoadEvent(Image img) {
		for (int i=0; i<jdisplistener.size(); i++) {
			jdisplistener.get(i).imageLoaded(img);
		}
	}
}
