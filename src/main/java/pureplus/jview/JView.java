package pureplus.jview;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.imageio.*;

class JViewView extends JComponent
{
	Image   image;

	public void paint(Graphics g) {
		Graphics2D  g2d = (Graphics2D)g;

		//g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		Rectangle  bounds = g2d.getClipBounds();

		g2d.setColor(Color.lightGray);
		g2d.fillRect(0, 0, bounds.width, bounds.height);

		if (image!=null) {

			int  img_w = image.getWidth(this);
			int  img_h = image.getHeight(this);

			if (img_w != 0 && img_h != 0) {
				double sc_w = (double)bounds.width / img_w;
				double sc_h = (double)bounds.height / img_h;

				double scale = (sc_w < sc_h)?sc_w:sc_h;

				int   draw_w = (int)(img_w * scale);
				int   draw_h = (int)(img_h * scale);
				int   draw_x = (bounds.width - draw_w)/2;
				int   draw_y = (bounds.height - draw_h)/2;
	
				g2d.drawImage(image, draw_x, draw_y, draw_w, draw_h, this);
			}
		}
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void setImage(Image img) {
		this.image = img;
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

	public JView() {
		frame = new JFrame("JView");
		cont = new JViewView();
		
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent ev) {
				ldr.shutdown();
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

		ldr = null;
	}

	public void setPath(String path) {
		if (ldr != null) {
			ldr = null;
		}

		ldr = new JViewLoader(path);
		ldr.addJViewLoadListener(img -> {
			cont.setImage(img);
			cont.repaint();
		});
		ldr.start();
	}

	public void setVisible(boolean v) {
		frame.setVisible(v);
	}

	public boolean isVisible() {
		return frame.isVisible();
	}

	public static void main(String[] args) {
		JView jview = new JView();
		if (args.length>0) {
			jview.setPath(args[0]);
		}
		jview.setVisible(true);
	}
}
