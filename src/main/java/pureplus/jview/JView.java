package pureplus.jview;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import javax.swing.*;

class JViewView extends JComponent
{
	BufferedImage   image;
	BufferedImage   cachedImage;
	Rectangle       cachedBounds;

	/**
     * BOXフィルタで画像を縮小
     * @param src 元画像
     * @param newWidth 縮小後の幅
     * @param newHeight 縮小後の高さ
     * @return 縮小後の画像
     */
    public static BufferedImage resizeBoxFilter(BufferedImage src, int newWidth, int newHeight) {
        if (src == null || newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("Invalid image or dimensions.");
        }

        BufferedImage dst = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        double xRatio = (double) src.getWidth() / newWidth;
        double yRatio = (double) src.getHeight() / newHeight;

        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {

                // 元画像での対応領域
                int xStart = (int) Math.floor(x * xRatio);
                int yStart = (int) Math.floor(y * yRatio);
                int xEnd = (int) Math.min(Math.ceil((x + 1) * xRatio), src.getWidth());
                int yEnd = (int) Math.min(Math.ceil((y + 1) * yRatio), src.getHeight());

                long sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                int count = 0;

                for (int yy = yStart; yy < yEnd; yy++) {
                    for (int xx = xStart; xx < xEnd; xx++) {
                        int rgb = src.getRGB(xx, yy);
                        int a = (rgb >> 24) & 0xFF;
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        sumA += a;
                        sumR += r;
                        sumG += g;
                        sumB += b;
                        count++;
                    }
                }

                // 平均色を計算
                int avgA = (int) (sumA / count);
                int avgR = (int) (sumR / count);
                int avgG = (int) (sumG / count);
                int avgB = (int) (sumB / count);

                int avgRGB = (avgA << 24) | (avgR << 16) | (avgG << 8) | avgB;
                dst.setRGB(x, y, avgRGB);
            }
        }

        return dst;
	}

	private BufferedImage createScaledImage(BufferedImage img, int dstw, int dsth) {
		BufferedImage resized;
		int  img_w = img.getWidth();
		int  img_h = img.getHeight();

		double sc_w = (double)dstw / img_w;
		double sc_h = (double)dsth / img_h;

		double scale = (sc_w < sc_h)?sc_w:sc_h;

		int   draw_w = (int)(img_w * scale);
		int   draw_h = (int)(img_h * scale);

		if (scale < 1.0) {
			resized = resizeBoxFilter(img, draw_w, draw_h);
		} else {
			resized = new BufferedImage(draw_w, draw_h, BufferedImage.TYPE_INT_RGB);
			Graphics2D  g2d = resized.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2d.drawImage(img, 0, 0, draw_w, draw_h, this);
			g2d.dispose();
		}
		return resized;
	}

	public void paint(Graphics g) {		
		Rectangle  bounds = this.getBounds();

		g.setColor(Color.lightGray);
		g.fillRect(0, 0, bounds.width, bounds.height);

		if (image!=null) {
			if (cachedImage != null && cachedBounds != null) {
				if (bounds.width != cachedBounds.width || bounds.height != cachedBounds.height) {
					cachedImage = createScaledImage(image, bounds.width, bounds.height);
					cachedBounds = bounds;
				}
			} else {
				cachedImage = createScaledImage(image, bounds.width, bounds.height);
				cachedBounds = bounds;
			}

			int  draw_x = (bounds.width - cachedImage.getWidth()) / 2;
			int  draw_y = (bounds.height - cachedImage.getHeight()) / 2;

			g.drawImage(cachedImage, draw_x, draw_y, this);
		}
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void setImage(BufferedImage img) {
		this.image = img;
		this.cachedImage = null;
		this.cachedBounds = null;
	}

	public Dimension getPrefferedSize() {
		return new Dimension(1200,768);
	}
}

public class JView
{
	JFrame       frame;
	JViewView    cont;
	JViewLoader  ldr;

	JFrame        imgListFrame;
	JList<String> imgList;

	public JView() {
		frame = new JFrame("JView");
		cont = new JViewView();
		
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent ev) {
				if (imgListFrame!=null) {
					imgListFrame.dispose();
				}
				if (ldr!=null) {
					ldr.shutdown();
				}
				//System.exit(0);
			}
		});
		frame.setContentPane(cont);

		cont.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent ev) {
				if (ldr!=null) { 
					if (ev.isPopupTrigger()) {
						ldr.previous();
						//System.out.println("previous");
					} else {
						ldr.next();
					}
				}
			}
		});

		cont.setFocusable(true);
		cont.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent ev) {
				//System.out.println("KeyEvent code="+ev.getKeyCode());
				if (ldr!=null) {
					switch(ev.getKeyCode()) {
					case KeyEvent.VK_DOWN:
					case KeyEvent.VK_RIGHT:
					case KeyEvent.VK_SPACE:
						ldr.next();
						break;
					case KeyEvent.VK_UP:
					case KeyEvent.VK_LEFT:
						ldr.previous();
					}
				}
			}
		});

		frame.setSize(1200,1024);

		/* SetMenu */
		JMenuBar    menuBar = new JMenuBar();
		JMenu       menu = new JMenu("JView");
		JMenuItem   openList = new JMenuItem("Open Image List");
		openList.addActionListener(ev -> {
			openImageList();
		});
		menu.add(openList);
		menuBar.add(menu);
		frame.setJMenuBar(menuBar);

		ldr = null;
	}

	public void setPath(String path) {
		if (ldr != null) {
			ldr = null;
		}

		ldr = new JViewLoader(path);
		ldr.addJViewLoadListener((img, name) -> {
			cont.setImage(img);
			cont.repaint();
			frame.setTitle(name);
			if (imgList!=null) {
				imgList.setSelectedValue(name, true);
			}
		});
		ldr.start();
	}

	public void setVisible(boolean v) {
		frame.setVisible(v);
	}

	public boolean isVisible() {
		return frame.isVisible();
	}

	public void openImageList() {
		if (imgListFrame==null) {
			imgListFrame = new JFrame("Image List");
			imgListFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			imgListFrame.setSize(200,400);
			imgList = new JList<String>(ldr.getListModel());
			imgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			imgList.addListSelectionListener(ev -> {
				if (!ev.getValueIsAdjusting()) {
					ldr.setIndex(imgList.getSelectedIndex());
					ldr.updateCurrent();
				}
			});
			imgListFrame.add(new JScrollPane(imgList));
		}
		imgListFrame.setVisible(true);
	}

	public static void main(String[] args) {
		JView jview = new JView();
		if (args.length>0) {
			jview.setPath(args[0]);
		}
		jview.setVisible(true);
	}
}
