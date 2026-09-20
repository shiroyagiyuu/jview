package pureplus.jview;

import java.io.File;
import java.io.IOException;

public interface JViewImageReader<I>
{
	public abstract I loadImage(File file) throws IOException;
}
