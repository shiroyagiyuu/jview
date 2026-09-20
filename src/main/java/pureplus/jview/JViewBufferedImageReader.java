package pureplus.jview;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

public class JViewBufferedImageReader implements JViewImageReader<BufferedImage>
{
	public BufferedImage loadImage(File file) throws IOException {
		return ImageIO.read(file);
	}
}
