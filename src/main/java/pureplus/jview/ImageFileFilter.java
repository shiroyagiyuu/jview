import java.io.*;

public class ImageFileFilter implements FilenameFilter
{
	String[]  exts = {".png",".jpg",".jpeg",".jfif"};
	@Override
	public boolean accept(File dir, String name) {
		for (int i=0; i<exts.length; i++) {
			if (name.endsWith(exts[i])) return true;
		}
		return false;
	}
}
